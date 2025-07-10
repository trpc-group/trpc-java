/*
 * Tencent is pleased to support the open source community by making tRPC available.
 *
 * Copyright (C) 2023 Tencent.
 * All rights reserved.
 *
 * If you have downloaded a copy of the tRPC source code from Tencent,
 * please note that tRPC source code is licensed under the Apache 2.0 License,
 * A copy of the Apache 2.0 License can be found in the LICENSE file.
 */

package com.tencent.trpc.transport.netty;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.CompositeByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.DecoderException;
import io.netty.util.internal.RecyclableArrayList;
import io.netty.util.internal.StringUtil;
import java.util.ArrayList;
import java.util.List;

/**
 * This class mainly modifies {@link io.netty.handler.codec.ByteToMessageDecoder}
 * to provide batch processing submission functionality.
 */
public abstract class AbstractBatchDecoder extends ChannelInboundHandlerAdapter {

    /**
     * Accumulates {@link ByteBuf}s by merging them into a single {@link ByteBuf} using memory copies.
     */
    public static final Cumulator MERGE_CUMULATOR = (alloc, cumulation, in) -> {
        ByteBuf buffer;
        if (cumulation.writerIndex() > cumulation
                .maxCapacity() - in.readableBytes()
                || cumulation.refCnt() > 1) {
            // Expand cumulation (by replace it) when either there is not more room in the buffer
            // or if the refCnt is greater then 1 which may happen when the user use slice().retain() or
            // duplicate().retain().
            //
            // See:
            // - https://github.com/netty/netty/issues/2327
            // - https://github.com/netty/netty/issues/1764
            buffer = expandCumulation(alloc, cumulation, in.readableBytes());
        } else {
            buffer = cumulation;
        }
        buffer.writeBytes(in);
        in.release();
        return buffer;
    };

    /**
     * Accumulates {@link ByteBuf}s by adding them to a {@link CompositeByteBuf},
     * so that memory copies are avoided as much as possible.
     * {@link CompositeByteBuf} uses a more complex indexing implementation,
     * so it depends on the specific scenario.
     * The implementation of the decoder may be slightly slower than using {@link #MERGE_CUMULATOR}.
     */
    public static final Cumulator COMPOSITE_CUMULATOR = (alloc, cumulation, in) -> {
        ByteBuf buffer;
        if (cumulation.refCnt() > 1) {
            // Expand cumulation (by replace it) when the refCnt is greater then 1 which may happen when the user
            // use slice().retain() or duplicate().retain().
            //
            // See:
            // - https://github.com/netty/netty/issues/2327
            // - https://github.com/netty/netty/issues/1764
            buffer = expandCumulation(alloc, cumulation, in.readableBytes());
            buffer.writeBytes(in);
            in.release();
        } else {
            CompositeByteBuf composite;
            if (cumulation instanceof CompositeByteBuf) {
                composite = (CompositeByteBuf) cumulation;
            } else {
                int readable = cumulation
                        .readableBytes();
                composite = alloc
                        .compositeBuffer();
                composite.addComponent(
                        cumulation).writerIndex(
                        readable);
            }
            composite
                    .addComponent(in)
                    .writerIndex(
                            composite.writerIndex()
                                    + in.readableBytes());
            buffer = composite;
        }
        return buffer;
    };

    ByteBuf cumulation;
    /**
     * Cumulator: accumulation is done using memory copies by default.
     */
    private Cumulator cumulator = MERGE_CUMULATOR;
    private boolean singleDecode;
    private boolean decodeWasNull;
    private boolean first;
    private int discardAfterReads = 16;
    private int numReads;

    /**
     * If singleDecode is set, only one message is decoded per {@link #channelRead(ChannelHandlerContext, Object)} call.
     * If some protocol upgrade is needed and it is important to ensure that there is no confusion, it can be used.
     * The default value is {@code false}, as this can impact performance.
     *
     * @param singleDecode whether this decoder decode one message each time
     */
    public void setSingleDecode(boolean singleDecode) {
        this.singleDecode = singleDecode;
    }

    /**
     * If {@code true}, only one message is decoded per {@link #channelRead(ChannelHandlerContext, Object)} call.
     * The default value is {@code false}, as this can impact performance.
     *
     * @return whether this decoder decode one message each time
     */
    public boolean isSingleDecode() {
        return singleDecode;
    }

    /**
     * Set the {@link Cumulator} to accumulate the received {@link ByteBuf}s.
     *
     * @param cumulator {@link ByteBuf}s cumulator
     */
    public void setCumulator(Cumulator cumulator) {
        if (cumulator == null) {
            throw new NullPointerException("cumulator");
        }
        this.cumulator = cumulator;
    }

    /**
     * Set the number of reads before calling {@link ByteBuf#discardSomeReadBytes()} to release memory.
     *
     * @param discardAfterReads The number of reads before discard. Default value is {@code 16}.
     */
    public void setDiscardAfterReads(int discardAfterReads) {
        if (discardAfterReads <= 0) {
            throw new IllegalArgumentException("discardAfterReads must be > 0");
        }
        this.discardAfterReads = discardAfterReads;
    }

    /**
     * Returns the actual number of readable bytes in the internal accumulated buffer of this decoder.
     * This is usually not needed to write a decoder.
     *
     * @return the actual number of readable bytes
     */
    protected int actualReadableBytes() {
        return internalBuffer().readableBytes();
    }

    /**
     * Returns the internal accumulated buffer of this decoder.
     *
     * @return the internal accumulated buffer
     */
    protected ByteBuf internalBuffer() {
        if (cumulation != null) {
            return cumulation;
        } else {
            return Unpooled.EMPTY_BUFFER;
        }
    }

    @Override
    public final void handlerRemoved(ChannelHandlerContext ctx) {
        ByteBuf buf = internalBuffer();
        int readable = buf.readableBytes();
        if (readable > 0) {
            ByteBuf bytes = buf.readBytes(readable);
            buf.release();
            ctx.fireChannelRead(bytes);
        } else {
            buf.release();
        }
        cumulation = null;
        numReads = 0;
        ctx.fireChannelReadComplete();
        handlerRemoved0(ctx);
    }

    /**
     * Called when the {@link io.netty.handler.codec.ByteToMessageDecoder} is removed from the actual context
     * and will no longer handle events.
     *
     * @param ctx the Netty's {@link ChannelHandlerContext}
     */
    protected void handlerRemoved0(ChannelHandlerContext ctx) {
    }

    /**
     * Modified this method to check the size of the decoded msg.
     * The size is recorded by the local variable {@code RecyclableArrayList out}.
     * If more than one message is decoded, then an array list is constructed to
     * submit all decoded messages to the pipeline.
     *
     * @param ctx the Netty's {@link ChannelHandlerContext}
     * @param msg data
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (msg instanceof ByteBuf) {
            RecyclableArrayList out = RecyclableArrayList.newInstance();
            try {
                ByteBuf data = (ByteBuf) msg;
                first = cumulation == null;
                if (first) {
                    cumulation = data;
                } else {
                    cumulation = cumulator.cumulate(ctx.alloc(), cumulation, data);
                }
                callDecode(ctx, cumulation, out);
            } catch (DecoderException e) {
                throw e;
            } catch (Throwable t) {
                throw new DecoderException(t);
            } finally {
                fireChannelProcess(ctx, out);
            }
        } else {
            ctx.fireChannelRead(msg);
        }
    }

    /**
     * Channel read post process
     *
     * @param ctx the Netty's {@link ChannelHandlerContext}
     * @param out decoded output msg list
     */
    private void fireChannelProcess(ChannelHandlerContext ctx, RecyclableArrayList out) {
        if (cumulation != null && !cumulation.isReadable()) {
            numReads = 0;
            cumulation.release();
            cumulation = null;
        } else if (++numReads >= discardAfterReads) {
            // We did enough reads already try to discard some bytes so we not risk to see a OOME.
            // See https://github.com/netty/netty/issues/4275
            numReads = 0;
            discardSomeReadBytes();
        }

        int size = out.size();
        if (size == 0) {
            decodeWasNull = true;
        } else if (size == 1) {
            ctx.fireChannelRead(out.get(0));
        } else {
            ArrayList<Object> ret = new ArrayList<Object>(size);
            for (int i = 0; i < size; i++) {
                ret.add(out.get(i));
            }
            ctx.fireChannelRead(ret);
        }

        out.recycle();
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        numReads = 0;
        discardSomeReadBytes();
        if (decodeWasNull) {
            decodeWasNull = false;
            if (!ctx.channel().config().isAutoRead()) {
                ctx.read();
            }
        }
        ctx.fireChannelReadComplete();
    }

    protected final void discardSomeReadBytes() {
        if (cumulation != null && !first && cumulation.refCnt() == 1) {
            // discard some bytes if possible to make more room in the
            // buffer but only if the refCnt == 1  as otherwise the user may have
            // used slice().retain() or duplicate().retain().
            //
            // See:
            // - https://github.com/netty/netty/issues/2327
            // - https://github.com/netty/netty/issues/1764
            cumulation.discardSomeReadBytes();
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        RecyclableArrayList out = RecyclableArrayList.newInstance();
        try {
            if (cumulation != null) {
                callDecode(ctx, cumulation, out);
                decodeLast(ctx, cumulation, out);
            } else {
                decodeLast(ctx, Unpooled.EMPTY_BUFFER, out);
            }
        } catch (DecoderException e) {
            throw e;
        } catch (Exception e) {
            throw new DecoderException(e);
        } finally {
            try {
                if (cumulation != null) {
                    cumulation.release();
                    cumulation = null;
                }
                int size = out.size();
                for (int i = 0; i < size; i++) {
                    ctx.fireChannelRead(out.get(i));
                }
                if (size > 0) {
                    // Something was read, call fireChannelReadComplete()
                    ctx.fireChannelReadComplete();
                }
                ctx.fireChannelInactive();
            } finally {
                // recycle in all cases
                out.recycle();
            }
        }
    }

    /**
     * This method is called to decode data from the given {@link ByteBuf}.
     * This method will call the decode method to perform the decoding operation.
     *
     * @param ctx the Netty's {@link ChannelHandlerContext}
     * @param in data to read
     * @param out to collect decoded msg
     */
    protected void callDecode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        try {
            while (in.isReadable()) {
                int outSize = out.size();
                int oldInputLength = in.readableBytes();
                decode(ctx, in, out);

                // Check if this handler was removed before continuing the loop.
                // If it was removed, it is not safe to continue to operate on the buffer.
                //
                // See https://github.com/netty/netty/issues/1664
                if (ctx.isRemoved()) {
                    break;
                }

                if (outSize == out.size()) {
                    if (oldInputLength == in.readableBytes()) {
                        break;
                    } else {
                        continue;
                    }
                }

                if (oldInputLength == in.readableBytes()) {
                    throw new DecoderException(
                            StringUtil.simpleClassName(getClass())
                                    + ".decode() did not read anything but decoded a message.");
                }

                if (isSingleDecode()) {
                    break;
                }
            }
        } catch (DecoderException e) {
            throw e;
        } catch (Throwable cause) {
            throw new DecoderException(cause);
        }
    }

    /**
     * The last time this method is called is when the {@link ChannelHandlerContext} is in an inactive state.
     * This means that {@link #channelInactive(ChannelHandlerContext)} has been triggered.
     * By default, it will only call the decode method, but subclasses may override it to perform
     * some special cleanup operations.
     *
     * @param ctx the Netty's {@link ChannelHandlerContext}
     * @param in data to read
     * @param out to collect decoded msgs
     * @throws Exception if decode msg failed
     */
    protected void decodeLast(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        decode(ctx, in, out);
    }

    static ByteBuf expandCumulation(ByteBufAllocator alloc, ByteBuf cumulation, int readable) {
        ByteBuf oldCumulation = cumulation;
        cumulation = alloc.buffer(oldCumulation.readableBytes() + readable);
        cumulation.writeBytes(oldCumulation);
        oldCumulation.release();
        return cumulation;
    }

    /**
     * Cumulate {@link ByteBuf}s.
     */
    public interface Cumulator {

        /**
         * Accumulates the given {@link ByteBuf} and returns a {@link ByteBuf} that holds the accumulated bytes.
         * This method is responsible for handling the lifecycle of the given {@link ByteBuf} correctly,
         * so if the {@link ByteBuf} is fully consumed, {@link ByteBuf#release()} needs to be called.
         *
         * @param alloc the {@link ByteBufAllocator}
         * @param cumulation a {@link ByteBuf} used to cumulate with
         * @param in a {@link ByteBuf} to be cumulated
         * @return a cumulated {@link ByteBuf}
         */
        ByteBuf cumulate(ByteBufAllocator alloc, ByteBuf cumulation, ByteBuf in);
    }

    /**
     * Decodes from one {@link ByteBuf} to another. It will be called until either of the inputs is exhausted.
     * When returning from this method or until any content is read from the input,
     * the {@link ByteBuf} has no readable content.
     *
     * @param ctx the Netty's {@link ChannelHandlerContext}
     * @param in data to read
     * @param out to collect decoded msgs
     * @throws Exception if decode msg failed
     */
    protected abstract void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out)
            throws Exception;
}
