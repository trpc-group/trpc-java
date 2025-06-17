/*
 * Tencent is pleased to support the open source community by making tRPC available.
 *
 * Copyright (C) 2023 THL A29 Limited, a Tencent company. 
 * All rights reserved.
 *
 * If you have downloaded a copy of the tRPC source code from Tencent,
 * please note that tRPC source code is licensed under the Apache 2.0 License,
 * A copy of the Apache 2.0 License can be found in the LICENSE file.
 */

package com.tencent.trpc.proto.standard.stream;

import com.tencent.trpc.core.logger.Logger;
import com.tencent.trpc.core.logger.LoggerFactory;
import com.tencent.trpc.core.stream.transport.RpcConnection;
import com.tencent.trpc.core.utils.PreconditionUtils;
import com.tencent.trpc.core.worker.spi.WorkerPool;
import com.tencent.trpc.proto.standard.common.TRPCProtocol;
import com.tencent.trpc.proto.standard.stream.codec.TRpcStreamFrameCodec;
import com.tencent.trpc.proto.standard.stream.codec.TRpcStreamFrameHeaderCodec;
import com.tencent.trpc.proto.standard.stream.config.TRpcStreamConstants;
import io.netty.buffer.ByteBuf;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import jakarta.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import org.reactivestreams.Subscription;
import reactor.core.publisher.BaseSubscriber;

/**
 * Remote consumption control class for local streams, used to subscribe to local streams and send data to the
 * remote end, while supporting flow control.
 *
 * @param <T> specific consumption object type
 */
@ThreadSafe
public class StreamRemoteSubscriber<T> extends BaseSubscriber<T> {

    private static final Logger logger = LoggerFactory.getLogger(StreamRemoteSubscriber.class);

    private final WorkerPool workerPool;
    /**
     * Connection information.
     */
    private final RpcConnection connection;
    /**
     * Data frame encoder and decoder.
     */
    private final TRpcStreamFrameCodec frameCodec;
    /**
     * Stream ID.
     */
    private final int streamId;

    // Initialized window size of flow control. Zero means no flow control.
    private final int initialWindowSize;
    // Left window size.
    private volatile int windowSize;
    @SuppressWarnings("rawtypes")
    private static final AtomicIntegerFieldUpdater<StreamRemoteSubscriber> WINDOW_SIZE =
            AtomicIntegerFieldUpdater.newUpdater(StreamRemoteSubscriber.class, "windowSize");

    private final boolean noFlowControl;

    /**
     * Create a remote flow consumption control class.
     *
     * @param workerPool worker pool used to trigger flow control
     * @param connection stream connection
     * @param frameCodec frame codec
     * @param streamId stream ID
     * @param windowSize initialized window size, zero means no flow control
     */
    public StreamRemoteSubscriber(WorkerPool workerPool, RpcConnection connection, TRpcStreamFrameCodec frameCodec,
            int streamId, int windowSize) {
        this.workerPool = Objects.requireNonNull(workerPool, "workerPool is null");
        this.connection = Objects.requireNonNull(connection, "connection is null");
        this.frameCodec = Objects.requireNonNull(frameCodec, "frameCodec is null");
        PreconditionUtils.checkArgument(windowSize >= 0 && streamId >= 0,
                "create remote flow subscriber failed, windowSize=%d, streamId=%d",
                windowSize, streamId);
        this.streamId = streamId;
        this.initialWindowSize = windowSize;
        this.windowSize = windowSize;
        this.noFlowControl = windowSize == 0;
    }

    @Override
    protected void hookOnSubscribe(@Nonnull Subscription s) {
        if (noFlowControl) {
            s.request(Long.MAX_VALUE);
        } else {
            s.request(1);
        }
    }

    /**
     * Normally consume a piece of data and send it to the remote end.
     * Data is sent in frames.
     *
     * @param value the consumed data
     */
    @Override
    protected void hookOnNext(@Nonnull T value) {
        ByteBuf dataFrame = frameCodec.encodeStreamDataFrame(streamId, value);
        connection.send(dataFrame);

        if (noFlowControl) {
            return;
        }

        // get the real data size, without frame header
        int dataSize = dataFrame.readableBytes() - TRpcStreamFrameHeaderCodec.TRPC_FIX_HEADER_LENGTH;
        consumeWindow(dataSize);
    }

    /**
     * Normal consumption of data in the stream and send the close frame.
     * Normal one-way stream closure, indicating that the data has been sent.
     */
    @Override
    protected void hookOnComplete() {
        connection.send(TRpcStreamFrameCodec.encodeStreamCloseFrame(connection.alloc(), streamId,
                TRPCProtocol.TrpcRetCode.TRPC_INVOKE_SUCCESS_VALUE, TRpcStreamConstants.RPC_DEFAULT_RET_CODE_OK));
    }

    /**
     * Abnormal consumption of data in the stream and send the exception close frame.
     *
     * @param t exception information
     */
    @Override
    protected void hookOnError(Throwable t) {
        connection.send(TRpcStreamFrameCodec.encodeStreamCloseFrame(connection.alloc(), streamId,
                TRPCProtocol.TrpcRetCode.TRPC_STREAM_UNKNOWN_ERR_VALUE, t.toString()));
    }

    /**
     * Increment window size by remote feedback, this function may trigger the recovery of the stream consumption.
     *
     * @param size increment window size
     */
    public void incrementWindow(int size) {
        long window = WINDOW_SIZE.addAndGet(this, size);
        if (window - size <= 0 && window > 0) {
            logger.info("stream {} has recovered {} windowSize, reconsuming", streamId, window);
            workerPool.execute(() -> request(1));
        }
    }

    /**
     * Consume window size, if window is not been used out, this function will trigger another stream consume request.
     *
     * @param size consumed window size
     */
    private void consumeWindow(int size) {
        long window = WINDOW_SIZE.addAndGet(this, -size);
        if (window > 0) {
            request(1);
        } else {
            logger.warn("stream {} has used all {} windowSize, stop consuming", streamId, initialWindowSize);
        }
    }

}
