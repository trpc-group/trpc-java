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

package com.tencent.trpc.proto.support;

import com.tencent.trpc.core.common.config.ConsumerConfig;
import com.tencent.trpc.core.common.config.ProtocolConfig;
import com.tencent.trpc.core.exception.ErrorCode;
import com.tencent.trpc.core.exception.ExceptionHelper;
import com.tencent.trpc.core.exception.TRpcException;
import com.tencent.trpc.core.exception.TransportException;
import com.tencent.trpc.core.logger.Logger;
import com.tencent.trpc.core.logger.LoggerFactory;
import com.tencent.trpc.core.rpc.ConsumerInvoker;
import com.tencent.trpc.core.rpc.Request;
import com.tencent.trpc.core.rpc.Response;
import com.tencent.trpc.core.rpc.RpcClientContext;
import com.tencent.trpc.core.transport.ClientTransport;
import com.tencent.trpc.core.utils.FutureUtils;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.BiConsumer;

/**
 * Default implementation of {@link ConsumerInvoker}
 *
 * @see ConsumerInvoker
 */
public class DefConsumerInvoker<T> implements ConsumerInvoker<T> {

    private static final Logger LOG = LoggerFactory.getLogger(DefConsumerInvoker.class);
    private final ProtocolConfig config;
    private final ClientTransport transport;
    private final DefRpcClient rpcClient;
    private final DefResponseFutureManager futureManager;
    private final ConsumerConfig<T> consumerConfig;

    public DefConsumerInvoker(DefRpcClient client, ConsumerConfig<T> consumerConfig) {
        super();
        this.rpcClient = Objects.requireNonNull(client, "client");
        this.transport = client.getTransport();
        this.config = client.getProtocolConfig();
        this.futureManager = client.getFutureManager();
        this.consumerConfig = Objects.requireNonNull(consumerConfig, "consumerConfig");
    }

    /**
     * {@inheritDoc}
     *
     * @return corresponding service interface class
     */
    @Override
    public Class<T> getInterface() {
        return consumerConfig.getServiceInterface();
    }

    /**
     * Invoke the service method related to the request
     *
     * @param request the request object
     * @return {@link CompletionStage} of the invocation
     */
    @Override
    public CompletionStage<Response> invoke(Request request) {
        RpcClientContext context = (RpcClientContext) (request.getContext());
        BiConsumer<Void, Throwable> callback = (result, t) -> {
            if (t != null) {
                fail(request, t);
            }
        };
        if (context.isOneWay()) {
            try {
                transport.send(request).whenComplete(callback);
                return CompletableFuture.completedFuture(null);
            } catch (Exception ex) {
                callback.accept(null, ex);
                return FutureUtils.failed(transSendError2TRpcException(ex));
            }
        } else {
            try {
                CompletionStage<Response> future =
                        futureManager.newFuture(context, this, transport, request);
                transport.send(request).whenComplete(callback);
                return future;
            } catch (Exception ex) {
                callback.accept(null, ex);
                return FutureUtils.failed(transSendError2TRpcException(ex));
            }
        }
    }

    private void fail(Request r, Throwable t) {
        LOG.error("Client send request error, (request=" + r + ", transport=" + transport + ")", t);
        long requestId = r.getRequestId();
        DefResponseFuture future = futureManager.remove(requestId);
        if (future != null) {
            rpcClient.getFutureManager().completeException(future, transSendError2TRpcException(t));
        }
    }

    private TRpcException transSendError2TRpcException(Throwable ex) {
        Throwable originException = ExceptionHelper.unwrapCompletionException(ex);
        if (originException instanceof TransportException) {
            return TRpcException.newFrameException(ErrorCode.TRPC_CLIENT_NETWORK_ERR,
                    originException.getMessage(), originException);
        } else {
            return TRpcException.newFrameException(ErrorCode.TRPC_INVOKE_UNKNOWN_ERR,
                    originException.getMessage(), originException);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ProtocolConfig getProtocolConfig() {
        return config;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ConsumerConfig<T> getConfig() {
        return consumerConfig;
    }
}
