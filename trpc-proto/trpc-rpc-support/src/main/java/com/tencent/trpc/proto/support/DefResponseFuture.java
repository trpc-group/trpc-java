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

import com.tencent.trpc.core.common.Constants;
import com.tencent.trpc.core.exception.ErrorCode;
import com.tencent.trpc.core.exception.TRpcException;
import com.tencent.trpc.core.logger.Logger;
import com.tencent.trpc.core.logger.LoggerFactory;
import com.tencent.trpc.core.rpc.RpcContext;
import com.tencent.trpc.core.rpc.ConsumerInvoker;
import com.tencent.trpc.core.rpc.Request;
import com.tencent.trpc.core.rpc.Response;
import com.tencent.trpc.core.rpc.RpcClientContext;
import com.tencent.trpc.core.rpc.common.RpcMethodInfo;
import com.tencent.trpc.core.transport.ClientTransport;
import com.tencent.trpc.core.utils.PreconditionUtils;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * A {@link CompletableFuture} of tRPC server {@link Response}
 *
 * @see DefResponseFutureManager
 * @see Response
 * @see CompletableFuture
 */
public class DefResponseFuture extends CompletableFuture<Response> {

    private static final Logger LOG = LoggerFactory.getLogger(DefResponseFuture.class);
    private Request request;
    private RpcClientContext context;
    private ConsumerInvoker<?> invoker;
    private RpcMethodInfo methodInfo;
    private ClientTransport client;
    private int timeoutMills;
    private Future<?> timeoutFuture;

    public DefResponseFuture(RpcClientContext context, ConsumerInvoker<?> invoker,
            ClientTransport client, Request request) {
        super();
        this.context = Objects.requireNonNull(context, "context");
        this.request = Objects.requireNonNull(request, "request");
        this.invoker = Objects.requireNonNull(invoker, "invoker");
        this.client = Objects.requireNonNull(client, "client");
        this.timeoutMills = this.request.getMeta().getTimeout();
        if (this.request.getMeta().getTimeout() <= 0) {
            this.timeoutMills = Constants.DEFAULT_TIMEOUT;
            if (LOG.isDebugEnabled()) {
                LOG.debug("Add request into timeout queue, request<" + request
                        + "> without timeout options, set timeout to default 1s");
            }
        }
        PreconditionUtils.checkArgument(this.timeoutMills > 0, "timeoutMills[%s] <= 0",
                this.timeoutMills);
    }

    /**
     * {@inheritDoc}
     *
     * @return the {@link Response}
     * @throws CancellationException if this future was cancelled
     * @throws ExecutionException if this future completed exceptionally
     * @throws InterruptedException if the current thread was interrupted
     */
    @Override
    public Response get() throws InterruptedException, ExecutionException {
        try {
            return super.get(timeoutMills, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            throw TRpcException.newFrameException(ErrorCode.TRPC_CLIENT_INVOKE_TIMEOUT_ERR,
                    "timeout>" + timeoutMills, e);
        }
    }

    public Future<?> getTimeoutFuture() {
        return timeoutFuture;
    }

    public void setTimeoutFuture(Future<?> timeoutFuture) {
        this.timeoutFuture = timeoutFuture;
    }

    public long getRequestId() {
        return request.getRequestId();
    }

    public int getTimeout() {
        return this.timeoutMills;
    }

    public Request getRequest() {
        return request;
    }

    public void setRequest(Request request) {
        this.request = request;
    }

    public RpcContext getContext() {
        return context;
    }

    public void setContext(RpcClientContext context) {
        this.context = context;
    }

    public RpcMethodInfo getRpcMethodInfo() {
        return methodInfo;
    }

    public ConsumerInvoker<?> getInvoker() {
        return invoker;
    }

    public void setInvoker(ConsumerInvoker<?> invoker) {
        this.invoker = invoker;
    }

    public ClientTransport getClient() {
        return client;
    }

    public void setClient(ClientTransport client) {
        this.client = client;
    }
}
