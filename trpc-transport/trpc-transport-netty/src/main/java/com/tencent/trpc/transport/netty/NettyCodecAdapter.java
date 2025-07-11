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

import com.google.common.base.Preconditions;
import com.tencent.trpc.core.common.config.ProtocolConfig;
import com.tencent.trpc.core.exception.TransportException;
import com.tencent.trpc.core.rpc.Request;
import com.tencent.trpc.core.rpc.RequestMeta;
import com.tencent.trpc.core.rpc.Response;
import com.tencent.trpc.core.transport.codec.ChannelBuffer;
import com.tencent.trpc.core.transport.codec.Codec;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.DatagramPacket;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.MessageToByteEncoder;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.handler.codec.MessageToMessageEncoder;
import java.net.InetSocketAddress;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Codec adapter both for tcp & udp
 */
public class NettyCodecAdapter {

    private static final Logger LOGGER = LoggerFactory.getLogger(NettyCodecAdapter.class);
    private final ChannelHandler encoder;
    private final ChannelHandler decoder;
    private final Codec codec;
    private final ProtocolConfig config;

    private NettyCodecAdapter(Codec codec, ProtocolConfig config, boolean isTcp) {
        this.codec = codec;
        this.config = config;
        this.encoder = (isTcp ? new TcpEncoder0() : new UdpEncoder0());
        this.decoder = (isTcp ? (config.getBatchDecoder() ? new TcpDecoder0() : new TcpDecoder1()) : new UdpDecoder0());
    }

    public static NettyCodecAdapter createTcpCodecAdapter(Codec codec, ProtocolConfig config) {
        return new NettyCodecAdapter(codec, config, true);
    }

    public static NettyCodecAdapter createUdpCodecAdapter(Codec codec, ProtocolConfig config) {
        return new NettyCodecAdapter(codec, config, false);
    }

    public ChannelHandler getEncoder() {
        return encoder;
    }

    public ChannelHandler getDecoder() {
        return decoder;
    }

    private class TcpEncoder0 extends MessageToByteEncoder<Object> {

        @Override
        protected void encode(ChannelHandlerContext ctx, Object msg, ByteBuf out) throws Exception {
            ChannelBuffer buffer = new NettyChannelBuffer(out);
            Channel ch = ctx.channel();
            NettyChannel channel = NettyChannelManager.getOrAddChannel(ch, config);
            try {
                codec.encode(channel, buffer, msg);
            } finally {
                NettyChannelManager.removeChannelIfDisconnected(ctx.channel());
            }
        }
    }

    private class UdpEncoder0 extends MessageToMessageEncoder<Object> {

        @Override
        protected void encode(ChannelHandlerContext ctx, Object msg, List<Object> out)
                throws Exception {
            NettyChannel channel = NettyChannelManager.getOrAddChannel(ctx.channel(), config);
            try {
                ByteBuf ioBuffer = null;
                boolean exception = true;
                InetSocketAddress addr = null;
                try {
                    if (msg instanceof Response) {
                        addr = ((Response) msg).getRequest().getMeta().getRemoteAddress();
                    } else if (msg instanceof Request) {
                        addr = ((Request) msg).getMeta().getRemoteAddress();
                    } else {
                        throw new TransportException("unsupport " + msg.getClass());
                    }
                    Preconditions
                            .checkArgument(addr != null, "udp address could not be null, msg:%s",
                                    msg);
                    ioBuffer = ctx.alloc().directBuffer();
                    codec.encode(channel, new NettyChannelBuffer(ioBuffer), msg);
                    exception = false;
                } finally {
                    if (exception && ioBuffer != null) {
                        ioBuffer.release();
                    }
                }
                DatagramPacket pkg = new DatagramPacket(ioBuffer, addr);
                out.add(pkg);
            } finally {
                NettyChannelManager.removeChannelIfDisconnected(ctx.channel());
            }
        }
    }

    private class TcpDecoder0 extends AbstractBatchDecoder {

        @Override
        protected void decode(ChannelHandlerContext ctx, ByteBuf input, List<Object> out)
                throws Exception {
            NettyCodecAdapter.this.decode(ctx, input, out);
        }
    }

    private class TcpDecoder1 extends ByteToMessageDecoder {

        @Override
        protected void decode(ChannelHandlerContext ctx, ByteBuf input, List<Object> out)
                throws Exception {
            NettyCodecAdapter.this.decode(ctx, input, out);
        }
    }

    /**
     * Call the underlying codec to decode msg
     *
     * @param ctx channel handler context
     * @param input input data
     * @param out to collect decoded msgs
     */
    private void decode(ChannelHandlerContext ctx, ByteBuf input, List<Object> out) {
        ChannelBuffer message = new NettyChannelBuffer(input);
        NettyChannel channel = NettyChannelManager.getOrAddChannel(ctx.channel(), config);
        try {
            do {
                int readIndex = message.readerIndex();
                Object msg = codec.decode(channel, message);
                // reset remoteAddr
                if (msg instanceof Request) {
                    RequestMeta meta = ((Request) msg).getMeta();
                    meta.setRemoteAddress((InetSocketAddress) (ctx.channel().remoteAddress()));
                    meta.setLocalAddress((InetSocketAddress) (ctx.channel().localAddress()));
                }
                if (msg == Codec.DecodeResult.NOT_ENOUGH_DATA) {
                    message.readerIndex(readIndex);
                    break;
                } else {
                    // not sure
                    if (readIndex == message.readerIndex()) {
                        throw TransportException.create("tcp|decode without read data");
                    }
                    if (msg != null) {
                        out.add(msg);
                    }
                }
            } while (message.isReadable());
        } catch (Exception e) {
            message.skipBytes(message.readableBytes());
            throw new TransportException("tcp|decode failure", e);
        } finally {
            NettyChannelManager.removeChannelIfDisconnected(ctx.channel());
        }
    }

    private class UdpDecoder0 extends MessageToMessageDecoder<DatagramPacket> {

        @Override
        protected void decode(ChannelHandlerContext ctx, DatagramPacket dpkg, List<Object> out)
                throws Exception {
            ChannelBuffer message = new NettyChannelBuffer(dpkg.content());
            NettyChannel channel = NettyChannelManager.getOrAddChannel(ctx.channel(), config);
            try {
                InetSocketAddress sender = dpkg.sender();
                try {
                    do {
                        int readIndex = message.readerIndex();
                        Object msg = codec.decode(channel, message);
                        // reset remoteAddr
                        if (msg instanceof Request) {
                            RequestMeta meta = ((Request) msg).getMeta();
                            meta.setRemoteAddress(sender);
                        }
                        if (msg == Codec.DecodeResult.NOT_ENOUGH_DATA) {
                            break;
                        } else {
                            // not sure
                            if (readIndex == message.readerIndex()) {
                                throw TransportException.create("udp|decode without read data");
                            }
                            if (msg != null) {
                                out.add(msg);
                            }
                        }
                    } while (message.isReadable());// dpkg may contains more request
                } catch (Exception e) {
                    LOGGER.error("UdpDecoder0 decode failure:", e);
                }
            } finally {
                NettyChannelManager.removeChannelIfDisconnected(ctx.channel());
            }
        }
    }
}
