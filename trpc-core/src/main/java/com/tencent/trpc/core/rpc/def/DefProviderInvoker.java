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

package com.tencent.trpc.core.rpc.def;

import com.google.common.collect.Maps;
import com.tencent.trpc.core.common.Constants;
import com.tencent.trpc.core.common.config.ProtocolConfig;
import com.tencent.trpc.core.common.config.ProviderConfig;
import com.tencent.trpc.core.exception.ErrorCode;
import com.tencent.trpc.core.exception.TRpcException;
import com.tencent.trpc.core.logger.Logger;
import com.tencent.trpc.core.logger.LoggerFactory;
import com.tencent.trpc.core.rpc.InvokeMode;
import com.tencent.trpc.core.rpc.ProviderInvoker;
import com.tencent.trpc.core.rpc.Request;
import com.tencent.trpc.core.rpc.Response;
import com.tencent.trpc.core.rpc.RpcContext;
import com.tencent.trpc.core.rpc.RpcContextValueKeys;
import com.tencent.trpc.core.rpc.RpcInvocation;
import com.tencent.trpc.core.utils.PreconditionUtils;
import com.tencent.trpc.core.utils.RpcContextUtils;
import com.tencent.trpc.core.utils.RpcUtils;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import org.apache.commons.lang3.ArrayUtils;

/**
 * Default provider implementation with method granularity.
 */
public class DefProviderInvoker<T> implements ProviderInvoker<T> {

    private static final Logger LOG = LoggerFactory.getLogger(DefProviderInvoker.class);
    private ProtocolConfig config;
    private ProviderConfig<T> providerConfig;
    private Map<String, Method> rpcMethodMap = Maps.newHashMap();

    /**
     * Provider constructor.
     *
     * @param config protocol-related configuration
     * @param pConfig configuration related to the service interface list
     */
    public DefProviderInvoker(ProtocolConfig config, ProviderConfig<T> pConfig) {
        this.config = config;
        this.providerConfig = pConfig;
        Class<T> serviceType = pConfig.getServiceInterface();
        Arrays.stream(serviceType.getDeclaredMethods()).forEach(method -> {
            String rpcMethodName = RpcUtils.parseRpcMethodName(method, null);
            if (rpcMethodName == null) {
                LOG.warn("Gen providerInvoker error,  parse interface={}, which has no rpc method name",
                        serviceType.getName());
                return;
            }
            PreconditionUtils.checkArgument(!rpcMethodMap.containsKey(rpcMethodName),
                    "interface=[%s], rpcMethod[%s], duplicate", serviceType.getName(),
                    rpcMethodName);
            rpcMethodMap.put(rpcMethodName, method);
        });
        PreconditionUtils.checkArgument(pConfig.getRef() != null, "providerConfig ref is null");
    }

    /**
     * Get the timeout for the current request.
     *
     * @param request request object
     * @param costTime elapsed time in ms
     * @return timeout in ms
     */
    private LeftTimeout parseTimeout(final Request request, final long costTime) {
        // The timeout set by the caller, minus the network time, queue waiting time, etc. to get the remaining time
        int reqLeftTimeout = request.getMeta().getTimeout() - (int) costTime;
        // The timeout set by the callee
        long methodTimeout = getConfig().getRequestTimeout();
        if (request.getMeta().getTimeout() > 0 && methodTimeout > 0) {
            return new LeftTimeout(
                    Math.min(request.getMeta().getTimeout(), (int) methodTimeout),
                    Math.min(reqLeftTimeout, (int) methodTimeout));
        } else if (request.getMeta().getTimeout() > 0) {
            return new LeftTimeout(request.getMeta().getTimeout(), reqLeftTimeout);
        } else if (methodTimeout > 0) {
            return new LeftTimeout((int) methodTimeout, (int) methodTimeout);
        } else {
            return new LeftTimeout(Constants.DEFAULT_TIMEOUT, Constants.DEFAULT_TIMEOUT);
        }
    }

    @Override
    public CompletionStage<Response> invoke(Request request) {
        RpcContext context = request.getContext();
        RpcInvocation invocation = request.getInvocation();
        Object[] params = ArrayUtils.addAll(new Object[]{context}, invocation.getArguments());
        CompletableFuture<Response> responseFuture = new CompletableFuture<>();
        try {
            Method method = rpcMethodMap.get(invocation.getRpcMethodName());
            // unexpected cases theoretically do not exist
            if (method == null) {
                responseFuture.complete(RpcUtils.newResponse(request, null,
                        TRpcException.newFrameException(ErrorCode.TRPC_INVOKE_UNKNOWN_ERR,
                                "Unknown rpcMethod[" + invocation.getRpcMethodName()
                                        + "]")));
                return responseFuture;
            }
            long costTime = System.currentTimeMillis() - request.getMeta().getCreateTime();
            LeftTimeout leftTimeout = parseTimeout(request, costTime);
            long leftTime = leftTimeout.getLeftTimeout();
            long originTimeout = leftTimeout.getOriginTimeout();
            if (leftTime <= 0) {
                throw TRpcException.newFrameException(ErrorCode.TRPC_SERVER_TIMEOUT_ERR,
                        "cost time = " + costTime + "ms and timeout=" + originTimeout + " ms");
            }
            // put the full link timeout information into the Context
            RpcContextUtils.putValueMapValue(context, RpcContextValueKeys.CTX_LINK_INVOKE_TIMEOUT,
                    LinkInvokeTimeout.builder()
                            .startTime(System.currentTimeMillis())
                            .timeout(originTimeout)
                            .leftTimeout(leftTime)
                            .serviceEnableLinkTimeout(getConfig().getEnableLinkTimeout())
                            .build());

            T serviceImpl = providerConfig.getRef();
            Object result = method.invoke(serviceImpl, params);
            if (InvokeMode.isAsync(invocation.getInvokeMode())) {
                PreconditionUtils.checkArgument(result != null,
                        "Found invoker(rpcServiceName=%s, rpcMethodName=%s) return value is null",
                        invocation.getRpcServiceName(), invocation.getRpcMethodName());
                // 1) asynchronous call on the server side
                return ((CompletionStage<?>) result)
                        .thenApply((obj) -> RpcUtils.newResponse(request, obj, null));
            }
            // 2) support synchronous call on the server side
            responseFuture.complete(RpcUtils.newResponse(request, result, null));
        } catch (Throwable ex) {
            if (ex instanceof InvocationTargetException) {
                providerConfig.getWorkerPoolObj().getUncaughtExceptionHandler()
                        .uncaughtException(Thread.currentThread(), ex.getCause());
                responseFuture.complete(RpcUtils.newResponse(request, null, ex.getCause()));
            } else {
                providerConfig.getWorkerPoolObj().getUncaughtExceptionHandler()
                        .uncaughtException(Thread.currentThread(), ex);
                responseFuture.complete(RpcUtils.newResponse(request, null, ex));
            }
        }
        return responseFuture;
    }

    @Override
    public Class<T> getInterface() {
        return providerConfig.getServiceInterface();
    }

    @Override
    public ProtocolConfig getProtocolConfig() {
        return config;
    }

    @Override
    public T getImpl() {
        return providerConfig.getRef();
    }

    @Override
    public ProviderConfig<T> getConfig() {
        return providerConfig;
    }

}
