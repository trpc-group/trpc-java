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

package com.tencent.trpc.proto.standard.stream;

import com.tencent.trpc.core.common.config.ProtocolConfig;
import com.tencent.trpc.core.exception.TRpcException;
import com.tencent.trpc.core.logger.Logger;
import com.tencent.trpc.core.logger.LoggerFactory;
import com.tencent.trpc.core.stream.transport.RpcConnection;
import com.tencent.trpc.proto.standard.common.TRPCProtocol.TrpcRetCode;
import com.tencent.trpc.proto.standard.common.TRPCProtocol.TrpcStreamCloseMeta;
import com.tencent.trpc.proto.standard.common.TRPCProtocol.TrpcStreamCloseType;
import com.tencent.trpc.proto.standard.common.TRPCProtocol.TrpcStreamFeedBackMeta;
import com.tencent.trpc.proto.standard.common.TRpcFrameType;
import com.tencent.trpc.proto.standard.stream.codec.TRpcStreamFrameCodec;
import com.tencent.trpc.proto.standard.stream.codec.TRpcStreamFrameHeaderCodec;
import com.tencent.trpc.proto.standard.stream.config.TRpcStreamConstants;
import io.netty.buffer.ByteBuf;
import io.netty.util.ReferenceCountUtil;
import java.nio.channels.ClosedChannelException;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import reactor.core.publisher.Sinks;
import reactor.core.publisher.Sinks.EmitResult;
import reactor.core.publisher.Sinks.Many;

/**
 * TRPC protocol unified stream control class supported by both client and server. Client and server specific logic
 * is implemented in subclasses.
 */
public abstract class BaseTRpcStreamRequesterResponder {

    private static final Logger logger = LoggerFactory.getLogger(BaseTRpcStreamRequesterResponder.class);

    /**
     * Exception indicating that the connection was actively closed, used for controlling the abnormal end
     * of the stream.
     */
    private static final Exception CLOSED_CHANNEL_EXCEPTION = new ClosedChannelException();
    /**
     * Subscription information for the peer to consume.
     */
    protected final ConcurrentMap<Integer, StreamRemoteSubscriber<?>> subscribers = new ConcurrentHashMap<>();
    /**
     * Receivers for consuming peer data.
     */
    protected final ConcurrentMap<Integer, Sinks.Many<ByteBuf>> receivers = new ConcurrentHashMap<>();
    /**
     * Protocol configuration.
     */
    protected final ProtocolConfig protocolConfig;
    /**
     * Connection information.
     */
    protected final RpcConnection connection;
    /**
     * Stream close exception information. Through concurrency control, ensure that a stream has only
     * one close trigger condition.
     */
    private volatile Throwable terminationError;
    /**
     * The terminationError field atomic updater.
     */
    private static final AtomicReferenceFieldUpdater<BaseTRpcStreamRequesterResponder, Throwable> TERMINATION_ERROR =
            AtomicReferenceFieldUpdater.newUpdater(
                    BaseTRpcStreamRequesterResponder.class, Throwable.class, "terminationError");

    BaseTRpcStreamRequesterResponder(ProtocolConfig protocolConfig, RpcConnection connection) {
        this.protocolConfig = Objects.requireNonNull(protocolConfig, "protocolConfig is null");
        this.connection = Objects.requireNonNull(connection, "connection is null");

        // subscribe to connection close events, no need consume
        connection.onClose().subscribe(null, this::terminate, this::shutdown);

        // subscribe to response events
        connection.receive().subscribe(this::handleFrame, this::terminate);
    }

    /**
     * Parse the frame data and determine the call method.
     *
     * @param frame stream input frame data
     */
    private void handleFrame(ByteBuf frame) {
        int streamId = TRpcStreamFrameHeaderCodec.streamId(frame);
        TRpcFrameType frameType = TRpcStreamFrameHeaderCodec.frameType(frame);
        logger.debug("stream {} got frame: {}", streamId, frameType);

        // extract the data body from the TRPC protocol frame, and add a reference count to prevent it from being
        // released prematurely.
        ByteBuf data = frame.retainedSlice(TRpcStreamFrameHeaderCodec.TRPC_FIX_HEADER_LENGTH,
                frame.readableBytes() - TRpcStreamFrameHeaderCodec.TRPC_FIX_HEADER_LENGTH);
        try {
            // call different processing flows based on frame type
            switch (frameType) {
                case INIT: // stream init frame
                    handleStreamInit(streamId, data);
                    break;
                case DATA: // stream data frame
                    handleStreamData(streamId, data);
                    break;
                case FEEDBACK: // stream flow control frame
                    handleStreamFeedback(streamId, data);
                    break;
                case CLOSE: // stream close frame
                    handleStreamClose(streamId, data);
                    break;
                default:
                    throw TRpcException.newFrameException(TrpcRetCode.TRPC_STREAM_UNKNOWN_ERR_VALUE,
                            "unknown frameType " + frameType);
            }
        } catch (Throwable t) {
            // When an unknown exception occurs while processing the frame, throw it directly and close the
            // connection by the reactor processing flow.
            throw TRpcException.newFrameException(TrpcRetCode.TRPC_INVOKE_UNKNOWN_ERR_VALUE,
                    "handle frame failed", t);
        } finally {
            ReferenceCountUtil.safeRelease(data);
        }
    }

    /**
     * Handle stream init frame.
     *
     * @param streamId stream ID
     * @param data data frame
     */
    protected abstract void handleStreamInit(int streamId, ByteBuf data);

    /**
     * Handle stream data frame.
     *
     * @param streamId stream id
     * @param data data frame
     */
    protected void handleStreamData(int streamId, ByteBuf data) {
        Many<ByteBuf> receiver = receivers.get(streamId);
        if (receiver == null) {
            logger.error("cannot find receiver for stream {}", streamId);
            // if the data frame cannot find the stream, send an exception close frame
            errorResetStream(streamId, TrpcRetCode.TRPC_STREAM_UNKNOWN_ERR_VALUE, "not found stream");
            return;
        }

        // data will be sent to the stream for asynchronous consumption, here need to add a reference count
        EmitResult result = receiver.tryEmitNext(data.retain().touch());
        // when emitting data fails, end the entire stream
        if (result.isFailure()) {
            errorResetStream(streamId, TrpcRetCode.TRPC_STREAM_UNKNOWN_ERR_VALUE, "receive data failed: " + result);
        }
    }

    /**
     * Handle stream flow control frame.
     *
     * @param streamId stream id
     * @param data data frame
     */
    protected void handleStreamFeedback(int streamId, ByteBuf data) {
        StreamRemoteSubscriber<?> subscriber = subscribers.get(streamId);
        if (subscriber == null) {
            logger.warn("feedback msg not found stream {}", streamId);
            return;
        }

        TrpcStreamFeedBackMeta feedBack = TRpcStreamFrameCodec.decodeStreamFeedbackFrame(data);
        logger.info("got feedback msg: {}", feedBack);
        subscriber.incrementWindow(feedBack.getWindowSizeIncrement());
    }

    /**
     * Handle stream close frame.
     *
     * @param streamId stream id
     * @param data data frame
     */
    protected void handleStreamClose(int streamId, ByteBuf data) {
        Many<ByteBuf> receiver = receivers.get(streamId);
        if (receiver == null) {
            logger.error("cannot find receiver for stream {}", streamId);
            return;
        }

        TrpcStreamCloseMeta closeMeta = TRpcStreamFrameCodec.decodeStreamCloseFrame(data);

        EmitResult result = null;
        switch (closeMeta.getCloseType()) {
            case TrpcStreamCloseType.TRPC_STREAM_CLOSE_VALUE:   // normal one-way close stream
                if (closeMeta.getRet() != TrpcRetCode.TRPC_INVOKE_SUCCESS_VALUE) {
                    result = receiver.tryEmitError(TRpcException.newFrameException(closeMeta.getRet(),
                            closeMeta.getMsg().toStringUtf8()));
                } else {
                    result = receiver.tryEmitComplete();
                }
                break;
            case TrpcStreamCloseType.TRPC_STREAM_RESET_VALUE:   // exceptional two-way close stream
                errorResetStream(streamId, closeMeta.getRet(), closeMeta.getMsg().toStringUtf8());
                break;
            default:
                logger.error("got unknown close type: {}", closeMeta);
                break;
        }

        if (result != null && result.isFailure() && result != EmitResult.FAIL_CANCELLED) {
            logger.error("close stream {} failed: {}", streamId, result);
        }
    }

    /**
     * Handle stream init exception.
     *
     * @param streamId stream ID
     * @param ret error code
     * @param msg error message
     */
    protected void errorInitStream(int streamId, int ret, String msg) {
        logger.error("init stream {} failed, ret: {}, msg: {}", streamId, ret, msg);
        connection.send(
                TRpcStreamFrameCodec.encodeStreamInitResponseFrame(this.connection.alloc(),
                        streamId, 0, this.protocolConfig, ret, msg));

        // when the stream initialization fails, both ends of the stream are not subscribed, so here directly
        // remove the caches to prevent leakage.
        subscribers.remove(streamId);
        receivers.remove(streamId);
    }

    /**
     * Error handling logic for streams, closing all streams on both ends.
     *
     * @param streamId stream ID
     * @param ret error code
     * @param msg error message
     */
    protected void errorResetStream(int streamId, int ret, String msg) {
        logger.error("error reset stream {}, ret: {}, msg: {}", streamId, ret, msg);

        // cancel remote consume stream and notify the remote that the stream has been reseted
        Optional.ofNullable(subscribers.remove(streamId)).ifPresent(subscriber -> {
            subscriber.cancel();

            // notify the remote end to close the stream, it's okay to resend the exception close frame
            connection.send(TRpcStreamFrameCodec.encodeStreamResetFrame(this.connection.alloc(), streamId, ret, msg));
        });

        // close local receiver stream
        Optional.ofNullable(receivers.remove(streamId)).ifPresent(receiver -> {
            // send error msg to local receiver stream
            EmitResult result = receiver.tryEmitError(TRpcException.newFrameException(ret, msg));
            if (result.isFailure()) {
                logger.error("reset receiver stream {} failed: {}", streamId, result);
            }
        });
    }

    /**
     * Close the connection and all streams on the connection.
     */
    protected void shutdown() {
        terminate(CLOSED_CHANNEL_EXCEPTION);
    }

    /**
     * Connection closing process, need to close all streams on the connection. When the connection is closed, no new
     * streams will be created on the connection, just close the existing ones.
     *
     * @param t exception information
     */
    protected void terminate(Throwable t) {
        // already terminated
        if (terminationError != null) {
            return;
        }

        // terminated by another step
        if (!TERMINATION_ERROR.compareAndSet(this, null, t)) {
            return;
        }

        // close streams on both ends
        subscribers.values().forEach(StreamRemoteSubscriber::cancel);
        receivers.values().forEach(receiver -> receiver.tryEmitError(t));

        subscribers.clear();
        receivers.clear();
    }

    protected int getWindowSize(int window) {
        return window != 0 && window < TRpcStreamConstants.DEFAULT_STREAM_WINDOW_SIZE
                ? TRpcStreamConstants.DEFAULT_STREAM_WINDOW_SIZE
                : window;
    }

}
