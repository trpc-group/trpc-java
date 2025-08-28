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

package com.tencent.trpc.proto.support;

import com.tencent.trpc.core.exception.ErrorCode;
import com.tencent.trpc.core.exception.TRpcException;
import com.tencent.trpc.core.logger.Logger;
import com.tencent.trpc.core.logger.LoggerFactory;
import com.tencent.trpc.core.rpc.ConsumerInvoker;
import com.tencent.trpc.core.rpc.Request;
import com.tencent.trpc.core.rpc.Response;
import com.tencent.trpc.core.rpc.RpcClientContext;
import com.tencent.trpc.core.rpc.RpcInvocation;
import com.tencent.trpc.core.rpc.TimeoutManager;
import com.tencent.trpc.core.rpc.def.DefTimeoutManager;
import com.tencent.trpc.core.transport.ClientTransport;
import com.tencent.trpc.core.common.ConfigManager;
import com.tencent.trpc.core.common.ShutdownListener;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Future;

/**
 * Manages {@link DefResponseFuture}s for a tRPC client
 *
 * @see DefResponseFuture
 */
public class DefResponseFutureManager implements ShutdownListener {

    private static final Logger LOG = LoggerFactory.getLogger(DefResponseFutureManager.class);

    /**
     * Watcher for timeouts
     */
    private static TimeoutManager TIMEOUT_MANAGER = new DefTimeoutManager(10);
    /**
     * Store
     */
    private final ConcurrentMap<Long, DefResponseFuture> futureMap = new ConcurrentHashMap<>();

public DefResponseFutureManager() {
        ConfigManager.getInstance().registerShutdownListener(this);
    }

    /**
     * Create a {@link DefResponseFuture} for a tRPC request
     *
     * @param context context of the rpc call
     * @param invoker corresponding invoker
     * @param client underlying {@link ClientTransport}
     * @param request corresponding request
     * @return the newly created {@link DefResponseFuture}
     */
    public DefResponseFuture newFuture(RpcClientContext context, ConsumerInvoker<?> invoker,
            ClientTransport client, Request request) {
        DefResponseFuture future = new DefResponseFuture(context, invoker, client, request);
        watchTimeout(future);
        if (futureMap.putIfAbsent(future.getRequest().getRequestId(), future) != null) {
            throw TRpcException.newFrameException(ErrorCode.TRPC_INVOKE_UNKNOWN_ERR,
                    "requestId[%s],protocol[%s],exists,may request id generator duplicate id",
                    future.getRequest().getRequestId(),
                    invoker.getProtocolConfig().toSimpleString());
        }
        return future;
    }

    /**
     * Removes and force stops all {@link DefResponseFuture}s related to a tRPC client.
     * Should be called when a tRPC client closes.
     *
     * @param client the client
     */
    public void closeClient(ClientTransport client) {
        for (ConcurrentMap.Entry<Long, DefResponseFuture> entry : futureMap.entrySet()) {
            if (client.equals(entry.getValue().getClient())) {
                DefResponseFuture future = remove(entry.getKey());
                if (future != null && !future.isDone()) {
                    completeException(future, TRpcException
                            .newFrameException(ErrorCode.TRPC_INVOKE_UNKNOWN_ERR,
                                    "Client(" + client + ") stop"));
                }
            }
        }
    }

    /**
     * Complete a {@link DefResponseFuture} with a Response
     *
     * @param future the future
     * @param rsp corresponding {@link Response}
     */
    public void complete(DefResponseFuture future, Response rsp) {
        Objects.requireNonNull(rsp, "response");
        complete(future, rsp, null);
    }

    private void complete(DefResponseFuture future, Response rsp, Throwable ex) {
        if (future != null) {
            if (ex == null) {
                future.complete(rsp);
            } else {
                future.completeExceptionally(ex);
            }
            Future<?> timeoutFuture = future.getTimeoutFuture();
            if (timeoutFuture != null && !timeoutFuture.isDone()) {
                timeoutFuture.cancel(true);
                if (LOG.isDebugEnabled()) {
                    LOG.debug(
                            "Request future manager remove request=[{}], due to timeout or "
                                    + "receive response",
                            future.getRequest());
                }
            }
        }
    }

    /**
     * Complete a {@link DefResponseFuture} with a Throwable
     *
     * @param future the future
     * @param ex the Throwable
     */
    public void completeException(DefResponseFuture future, Throwable ex) {
        Objects.requireNonNull(ex, "ex");
        complete(future, null, ex);
    }

    /**
     * Get {@link DefResponseFuture} by requestId
     *
     * @param requestId requestId
     * @return related {@link DefResponseFuture}
     */
    public DefResponseFuture get(Long requestId) {
        return futureMap.get(requestId);
    }

    /**
     * Remove {@link DefResponseFuture} by requestId
     *
     * @param requestId requestId
     * @return the {@link DefResponseFuture} just removed
     */
    public DefResponseFuture remove(Long requestId) {
        return futureMap.remove(requestId);
    }

    public void stop() {
        TIMEOUT_MANAGER.close();
    }

    /**
     * Called when the container is reset.
     */
    public static void reset() {
        TIMEOUT_MANAGER = new DefTimeoutManager(10);
    }

    /**
     * Shutdown listener implementation to handle container shutdown
     */
    @Override
    public void onShutdown() {
        LOG.info("DefResponseFutureManager received shutdown notification");
        stop();
    }

    /**
     * Add a watcher for timeout check of the {@link DefResponseFuture}
     */
    private void watchTimeout(final DefResponseFuture future) {
        Request request = future.getRequest();
        long timeoutMills = future.getTimeout();
        Future<?> watch = TIMEOUT_MANAGER.watch(() -> {
            try {
                DefResponseFuture timeoutFuture = remove(request.getRequestId());
                if (timeoutFuture != null) {
                    RpcInvocation invocation = request.getInvocation();
                    String msg =
                            String.format(
                                    "request timeout > %s ms, {rpcService=%s, rpcMethod=%s, "
                                            + "remoteAddr=%s}",
                                    timeoutMills, invocation.getRpcServiceName(),
                                    invocation.getRpcMethodName(),
                                    request.getMeta().getRemoteAddress());
                    completeException(timeoutFuture,
                            TRpcException
                                    .newFrameException(ErrorCode.TRPC_CLIENT_INVOKE_TIMEOUT_ERR,
                                            msg));
                }
            } catch (Exception e) {
                LOG.error("timeout task watch exception.", e);
            }
        }, timeoutMills);
        future.setTimeoutFuture(watch);
    }
}