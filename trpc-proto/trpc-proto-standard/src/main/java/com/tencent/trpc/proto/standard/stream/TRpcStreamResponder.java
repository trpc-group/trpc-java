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
import com.tencent.trpc.core.common.config.ProtocolConfig;
import com.tencent.trpc.core.exception.ErrorCode;
import com.tencent.trpc.core.logger.Logger;
import com.tencent.trpc.core.logger.LoggerFactory;
import com.tencent.trpc.core.rpc.ProviderInvoker;
import com.tencent.trpc.core.rpc.RpcContext;
import com.tencent.trpc.core.rpc.RpcServerContext;
import com.tencent.trpc.core.rpc.common.RpcMethodInfo;
import com.tencent.trpc.core.rpc.common.RpcMethodInfoAndInvoker;
import com.tencent.trpc.core.rpc.def.DefMethodInfoRegister;
import com.tencent.trpc.core.stream.transport.RpcConnection;
import com.tencent.trpc.core.worker.spi.WorkerPool;
import com.tencent.trpc.proto.standard.common.TRPCProtocol.TrpcRetCode;
import com.tencent.trpc.proto.standard.common.TRPCProtocol.TrpcStreamInitMeta;
import com.tencent.trpc.proto.standard.common.TRPCProtocol.TrpcStreamInitRequestMeta;
import com.tencent.trpc.proto.standard.stream.codec.TRpcStreamFrameCodec;
import com.tencent.trpc.proto.standard.stream.config.TRpcStreamConstants;
import io.netty.buffer.ByteBuf;
import io.netty.util.ReferenceCountUtil;
import java.util.Objects;
import java.util.concurrent.ConcurrentMap;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

/**
 * TRPC protocol streaming response utility class.
 */
public class TRpcStreamResponder extends BaseTRpcStreamRequesterResponder {

    private static final Logger logger = LoggerFactory.getLogger(TRpcStreamResponder.class);

    /**
     * Cache for RPC method invokers.
     */
    private final ConcurrentMap<String, StreamServiceInvoker> services = Maps.newConcurrentMap();
    /**
     * Reference to the service method registrar, used for looking up RPC methods.
     */
    private final DefMethodInfoRegister methodInfoRegister;

    public TRpcStreamResponder(ProtocolConfig protocolConfig, RpcConnection connection,
            DefMethodInfoRegister methodInfoRegister) {
        super(protocolConfig, connection);
        this.methodInfoRegister = Objects.requireNonNull(methodInfoRegister, "methodInfoRegister is null");
        logger.debug("created new rpcResponder for {}", protocolConfig);
    }

    @Override
    protected void handleStreamInit(int streamId, ByteBuf frame) {
        TrpcStreamInitMeta initMeta = TRpcStreamFrameCodec.decodeStreamInitFrame(frame);
        TrpcStreamInitRequestMeta requestMeta = initMeta.getRequestMeta();
        String func = requestMeta.getFunc().toStringUtf8();
        // route the call method
        RpcMethodInfoAndInvoker methodInfoAndInvoker = methodInfoRegister.route(func);
        if (methodInfoAndInvoker == null) {
            errorInitStream(streamId, TrpcRetCode.TRPC_INVOKE_UNKNOWN_ERR_VALUE,
                    String.format("func %s not exist", func));
            return;
        }
        // get the request invoker
        ProviderInvoker<?> providerInvoker = methodInfoAndInvoker.getInvoker();
        RpcMethodInfo methodInfo = methodInfoAndInvoker.getMethodInfo();
        StreamServiceInvoker streamServiceInvoker = services.computeIfAbsent(func, key ->
                new StreamServiceInvoker(providerInvoker.getImpl(), methodInfo.getMethod()));
        // consume client messages and send to downstream server
        Sinks.Many<ByteBuf> receiver = Sinks.many().unicast().onBackpressureBuffer();
        receivers.put(streamId, receiver);

        // create data frame encoder and decoder
        TRpcStreamFrameCodec frameCodec = TRpcStreamFrameCodec.newDataFrameCodec(protocolConfig, connection.alloc(),
                initMeta.getContentEncoding(), initMeta.getContentType());
        final WorkerPool workerPool = providerInvoker.getConfig().getWorkerPoolObj();
        // server size flow control is firstly decided by the client init meta then by configuration.
        int recvBufSize = initMeta.getInitWindowSize() == 0 ? 0 : getWindowSize(this.protocolConfig.getReceiveBuffer());

        // Call the service. Streaming service calls are asynchronous, need to limit the stream consumption thread in
        // the worker thread pool.
        Scheduler scheduler = Schedulers.fromExecutor(workerPool.toExecutor());
        Flux<?> receiverFlux = receiver.asFlux()
                .publishOn(scheduler)
                .doOnNext(new StreamLocalConsumer(connection, streamId, recvBufSize))
                .map(data -> {
                    try {
                        // decode data
                        return frameCodec.decodeDataFrameData(data, (Class<?>) methodInfo.getActualParamsTypes()[1]);
                    } finally {
                        // release reference after decoding
                        ReferenceCountUtil.safeRelease(data);
                    }
                });

        // use the thread pool to call the corresponding interface to prevent blocking the IO thread
        workerPool.execute(() -> {
            // Currently TRPC streaming protocol does not carry timeout field, timeout control is currently managed
            // by the client side
            RpcContext ctx = new RpcServerContext();
            requestMeta.getTransInfoMap().forEach((key, val) -> ctx.getReqAttachMap().put(key, val.toByteArray()));

            Publisher<?> resp;
            switch (streamServiceInvoker.invokeMode) {
                case CLIENT_STREAM: // client stream
                    resp = streamServiceInvoker.clientStream(ctx, receiverFlux);
                    break;
                case SERVER_STREAM: // server stream
                    resp = receiverFlux.next().flatMapMany(req -> streamServiceInvoker.serverStream(ctx, req));
                    break;
                case DUPLEX_STREAM: // bidirectional stream
                    resp = streamServiceInvoker.duplexStream(ctx, receiverFlux);
                    break;
                default:
                    errorInitStream(streamId, TrpcRetCode.TRPC_INVOKE_UNKNOWN_ERR_VALUE,
                            String.format("stream method not support invoke mode %s", streamServiceInvoker.invokeMode));
                    return;
            }

            // 1.Notify the peer that the stream is initialized successfully.
            connection.send(
                    TRpcStreamFrameCodec.encodeStreamInitResponseFrame(connection.alloc(), streamId, recvBufSize,
                            this.protocolConfig, ErrorCode.TRPC_INVOKE_SUCCESS,
                            TRpcStreamConstants.RPC_DEFAULT_RET_CODE_OK));

            // 2.Subscribe to the output stream and send it to the client side.
            StreamRemoteSubscriber<Object> subscriber = new StreamRemoteSubscriber<>(workerPool,
                    connection, frameCodec, streamId, initMeta.getInitWindowSize());
            subscribers.put(streamId, subscriber);
            Flux.from(resp)
                    .doFinally(s -> subscribers.remove(streamId)) // remove cache if stream finished
                    .subscribe(subscriber);
        });
    }

}
