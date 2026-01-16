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

package com.tencent.trpc.proto.standard.stream.client;

import com.tencent.trpc.core.common.config.BackendConfig;
import com.tencent.trpc.core.common.config.ConsumerConfig;
import com.tencent.trpc.core.common.config.ProtocolConfig;
import com.tencent.trpc.core.exception.ErrorCode;
import com.tencent.trpc.core.exception.TRpcException;
import com.tencent.trpc.core.logger.Logger;
import com.tencent.trpc.core.logger.LoggerFactory;
import com.tencent.trpc.core.rpc.ConsumerInvoker;
import com.tencent.trpc.core.rpc.InvokeMode;
import com.tencent.trpc.core.rpc.Request;
import com.tencent.trpc.core.rpc.Response;
import com.tencent.trpc.core.rpc.RpcContext;
import com.tencent.trpc.core.rpc.RpcContextValueKeys;
import com.tencent.trpc.core.rpc.RpcInvocation;
import com.tencent.trpc.core.rpc.def.DefResponse;
import com.tencent.trpc.core.stream.transport.ClientTransport;
import com.tencent.trpc.core.stream.transport.RpcConnection;
import com.tencent.trpc.core.utils.RpcContextUtils;
import com.tencent.trpc.proto.standard.stream.TRpcStreamRequester;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.SignalType;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * Invoker that supports streaming calls
 *
 * @param <T> service interface type
 */
public class StreamConsumerInvoker<T> implements ConsumerInvoker<T> {

    private static final Logger logger = LoggerFactory.getLogger(StreamConsumerInvoker.class);

    private static final int STREAM_CLOSE_COUNT_BOTH_DIRECTION = 2;

    /**
     * Client interface related configuration
     */
    private final ConsumerConfig<T> consumerConfig;
    /**
     * Client related configuration
     */
    private final BackendConfig backendConfig;
    /**
     * Protocol related configuration
     */
    private final ProtocolConfig protocolConfig;
    /**
     * Client transport
     */
    private final ClientTransport clientTransport;

    public StreamConsumerInvoker(ConsumerConfig<T> consumerConfig, ProtocolConfig protocolConfig,
            ClientTransport clientTransport) {
        this.consumerConfig = Objects.requireNonNull(consumerConfig, "consumerConfig is null");
        this.backendConfig = Objects
                .requireNonNull(consumerConfig.getBackendConfig(), "backendConfig is null");
        this.protocolConfig = Objects.requireNonNull(protocolConfig, "protocolConfig is null");
        this.clientTransport = Objects.requireNonNull(clientTransport, "clientTransport is null");
    }

    @Override
    public ConsumerConfig<T> getConfig() {
        return consumerConfig;
    }

    @Override
    public ProtocolConfig getProtocolConfig() {
        return protocolConfig;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Class<T> getInterface() {
        return (Class<T>) backendConfig.getServiceInterface();
    }

    @Override
    public CompletionStage<Response> invoke(Request request) {
        CompletableFuture<Response> future = new CompletableFuture<>();
        // create a link and perform a remote call
        clientTransport
                .connect()
                .subscribe(conn -> {
                    try {
                        TRpcStreamRequester requester = new TRpcStreamRequester(protocolConfig, conn,
                                consumerConfig.getBackendConfig());

                        // Encapsulate the returned result into DefResponse and integrate with the current
                        // rpc framework.
                        DefResponse rpcResponse = new DefResponse();
                        rpcResponse.setValue(doInvoke(request, requester, conn));
                        future.complete(rpcResponse);
                    } catch (Throwable t) {
                        conn.dispose();
                        future.completeExceptionally(TRpcException.newFrameException(ErrorCode.TRPC_CLIENT_NETWORK_ERR,
                                "do invoke failed", t));
                    }
                }, t -> future.completeExceptionally(
                        TRpcException.newFrameException(ErrorCode.TRPC_CLIENT_CONNECT_ERR,
                                "do connect failed", t)
                ));

        return future;
    }

    /**
     * Perform a remote call
     *
     * @param request request data
     * @param requester streaming requester, encapsulating streaming request logic
     * @return remote call result
     */
    @SuppressWarnings("ReactiveStreamsUnusedPublisher")
    private Object doInvoke(Request request, TRpcStreamRequester requester, RpcConnection connection) {
        // get invoker information
        RpcInvocation invocation = request.getInvocation();
        InvokeMode invokeMode = invocation.getInvokeMode();
        RpcContext ctx = request.getContext();

        // add invocation and callinfo information to context
        RpcContextUtils.putValueMapValue(ctx, RpcContextValueKeys.RPC_CALL_INFO_KEY, request.getMeta().getCallInfo());
        RpcContextUtils.putValueMapValue(ctx, RpcContextValueKeys.RPC_INVOCATION_KEY, invocation);

        // close stream connection when both sides have completed
        AtomicInteger unFinished = new AtomicInteger(STREAM_CLOSE_COUNT_BOTH_DIRECTION);
        Consumer<SignalType> onFinally = s -> {
            if (unFinished.decrementAndGet() == 0) {
                logger.debug("close stream invoker connection: {}", connection);
                connection.dispose();
            }
        };

        Object req = invocation.getFirstArgument();
        // call different streaming methods according to the interface type
        switch (invokeMode) {
            case SERVER_STREAM: {   // server streaming call
                unFinished.decrementAndGet();
                return requester.serverStream(ctx, req).doFinally(onFinally);
            }
            case CLIENT_STREAM: {   // client streaming call
                Publisher<?> reqPub = Flux.from((Publisher<?>) req).doFinally(onFinally);
                return requester.clientStream(ctx, reqPub).doFinally(onFinally);
            }
            case DUPLEX_STREAM: {   // bidirectional streaming call
                Publisher<?> reqPub = Flux.from((Publisher<?>) req).doFinally(onFinally);
                return requester.duplexStream(ctx, (Publisher<?>) reqPub).doFinally(onFinally);
            }
            default:
                throw new UnsupportedOperationException("unknown method type " + invokeMode);
        }
    }

}
