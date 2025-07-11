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

import com.google.common.collect.Maps;
import com.tencent.trpc.core.common.Constants;
import com.tencent.trpc.core.common.config.BackendConfig;
import com.tencent.trpc.core.common.config.ProtocolConfig;
import com.tencent.trpc.core.exception.TRpcException;
import com.tencent.trpc.core.logger.Logger;
import com.tencent.trpc.core.logger.LoggerFactory;
import com.tencent.trpc.core.rpc.RpcContext;
import com.tencent.trpc.core.rpc.RpcContextValueKeys;
import com.tencent.trpc.core.rpc.RpcInvocation;
import com.tencent.trpc.core.stream.StreamCall;
import com.tencent.trpc.core.stream.transport.RpcConnection;
import com.tencent.trpc.core.utils.RpcContextUtils;
import com.tencent.trpc.core.worker.spi.WorkerPool;
import com.tencent.trpc.proto.standard.common.TRPCProtocol.TrpcRetCode;
import com.tencent.trpc.proto.standard.common.TRPCProtocol.TrpcStreamInitMeta;
import com.tencent.trpc.proto.standard.common.TRPCProtocol.TrpcStreamInitResponseMeta;
import com.tencent.trpc.proto.standard.stream.codec.TRpcStreamFrameCodec;
import com.tencent.trpc.proto.standard.stream.config.TRpcStreamConstants;
import io.netty.buffer.ByteBuf;
import io.netty.util.ReferenceCountUtil;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

/**
 * TRPC protocol streaming request utility class.
 */
public class TRpcStreamRequester extends BaseTRpcStreamRequesterResponder implements StreamCall {

    private static final Logger logger = LoggerFactory.getLogger(TRpcStreamRequester.class);

    /**
     * Stream initializer listener.
     */
    private final ConcurrentMap<Integer, Sinks.One<TrpcStreamInitMeta>> streamSetups = Maps.newConcurrentMap();
    /**
     * Stream ID generator, each connection has its own independent stream ID generation.
     */
    private final AtomicInteger streamIdGenerator = new AtomicInteger(TRpcStreamConstants.MIN_USER_STREAM_ID);

    /**
     * Client-side reference to the cluster-related configuration.
     */
    private final BackendConfig backendConfig;
    /**
     * Reactor execution thread pool.
     */
    private final WorkerPool workerPool;
    private final Scheduler scheduler;

    public TRpcStreamRequester(ProtocolConfig protocolConfig, RpcConnection connection,
            BackendConfig backendConfig) {
        super(protocolConfig, connection);
        this.backendConfig = Objects.requireNonNull(backendConfig, "backendConfig is null");
        this.workerPool = backendConfig.getWorkerPoolObj();
        this.scheduler = Schedulers.fromExecutor(workerPool.toExecutor());
    }

    @Override
    public <ReqT, RspT> Flux<RspT> serverStream(RpcContext ctx, ReqT request) {
        return duplexStream(ctx, Mono.just(request));
    }

    @Override
    public <ReqT, RspT> Mono<RspT> clientStream(RpcContext ctx, Publisher<ReqT> requests) {
        Flux<RspT> rspFlux = duplexStream(ctx, requests);
        return rspFlux.next();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <ReqT, RspT> Flux<RspT> duplexStream(RpcContext ctx, Publisher<ReqT> requests) {
        // the call context must carry the invocation information
        RpcInvocation invocation = RpcContextUtils.getValueMapValue(ctx, RpcContextValueKeys.RPC_INVOCATION_KEY);
        Objects.requireNonNull(invocation, "invocation");
        final int streamId = streamIdGenerator.getAndIncrement();
        // stream receive buffer size
        int recvBufSize = getWindowSize(this.protocolConfig.getReceiveBuffer());

        // stream initializing signal, triggered by the remote init response
        Sinks.One<TrpcStreamInitMeta> setup = Sinks.one();
        streamSetups.put(streamId, setup);

        // create subsequent communication stream
        Sinks.Many<ByteBuf> receiver = Sinks.many().unicast().onBackpressureBuffer();
        receivers.put(streamId, receiver);

        // build the stream call information
        TRpcStreamFrameCodec.RpcCallInfo callInfo = TRpcStreamFrameCodec.buildRpcCallInfo(ctx);
        // send stream initialization information
        connection.send(TRpcStreamFrameCodec.encodeStreamInitRequestFrame(connection.alloc(), streamId, recvBufSize,
                callInfo, this.backendConfig));

        // After the stream initialization packet is successfully received, start the subscription consumption.
        // Otherwise, the initialization exception information will be directly transmitted to the downstream.
        return setup.asMono()
                .timeout(getStreamBuildTimeout(ctx))
                .doOnError(t -> {
                    // When the stream initialization fails, trigger a two-way stream close operation to prevent the
                    // remote end from being unaware.
                    if (t instanceof TRpcException) {
                        TRpcException e = (TRpcException) t;
                        errorResetStream(streamId, e.getCode(), e.getMessage());
                    } else if (t instanceof TimeoutException) {
                        errorResetStream(streamId, TrpcRetCode.TRPC_STREAM_CLIENT_READ_TIMEOUT_ERR_VALUE,
                                "read stream timeout");
                    } else {
                        errorResetStream(streamId, TrpcRetCode.TRPC_STREAM_UNKNOWN_ERR_VALUE, "init stream failed");
                    }
                })
                .doFinally(s -> streamSetups.remove(streamId))
                .flatMapMany(initMeta -> {
                    // create data frame encoder and decoder
                    TRpcStreamFrameCodec frameCodec = TRpcStreamFrameCodec.newDataFrameCodec(protocolConfig,
                            connection.alloc(), initMeta.getContentEncoding(), initMeta.getContentType());

                    StreamRemoteSubscriber<ReqT> subscriber = new StreamRemoteSubscriber<>(this.workerPool,
                            connection, frameCodec, streamId, initMeta.getInitWindowSize());
                    subscribers.put(streamId, subscriber);
                    Flux.from(requests)
                            .subscribeOn(scheduler)
                            .doFinally(s -> subscribers.remove(streamId))
                            .subscribe(subscriber);

                    // adapt receiver stream to the subsequent communication stream
                    return receiver
                            .asFlux()
                            .publishOn(scheduler) // switch subsequent consumption thread
                            .doOnNext(new StreamLocalConsumer(connection, streamId, recvBufSize))
                            .map(data -> {
                                try {
                                    // decode data
                                    return frameCodec.decodeDataFrameData(data,
                                            (Class<RspT>) invocation.getRpcMethodInfo().getActualReturnType());
                                } finally {
                                    // release reference after decoding
                                    ReferenceCountUtil.safeRelease(data);
                                }
                            });
                })
                .doFinally(signal -> receivers.remove(streamId));
    }

    /**
     * Get the stream build timeout. The timeout is only used for stream establishment, and no timeout is specified
     * for subsequent communication.
     *
     * @param ctx context
     * @return timeout duration, default to use the unified tRPC client call timeout
     */
    private Duration getStreamBuildTimeout(RpcContext ctx) {
        // determine if timeout monitoring is required
        long timeout = ctx.toClientContext().getTimeoutMills();
        // if no timeout is configured, use the system default configuration, and the timeout here only controls
        // the establishment of the stream.
        if (timeout <= 0) {
            timeout = Constants.DEFAULT_TIMEOUT;
        }
        return Duration.ofMillis(timeout);
    }

    @Override
    protected void handleStreamInit(int streamId, ByteBuf data) {
        Sinks.One<TrpcStreamInitMeta> setupMonoSink = this.streamSetups.get(streamId);
        if (setupMonoSink == null) {
            errorResetStream(streamId, TrpcRetCode.TRPC_STREAM_UNKNOWN_ERR_VALUE, "not found stream");
            return;
        }
        TrpcStreamInitMeta frameInitMeta = TRpcStreamFrameCodec.decodeStreamInitFrame(data);
        Objects.requireNonNull(frameInitMeta, "the stream init frame is null");
        // determine if the initialization is successful based on the stream's init header
        TrpcStreamInitResponseMeta responseMeta = frameInitMeta.getResponseMeta();
        if (responseMeta.getRet() != TrpcRetCode.TRPC_INVOKE_SUCCESS_VALUE) {
            logger.error("stream {} init failed: {}", streamId, responseMeta);
            setupMonoSink.emitError(TRpcException.newFrameException(responseMeta.getRet(),
                    responseMeta.getErrorMsg().toStringUtf8()), Sinks.EmitFailureHandler.FAIL_FAST);
        } else {
            setupMonoSink.emitValue(frameInitMeta, Sinks.EmitFailureHandler.FAIL_FAST);
        }
    }

}
