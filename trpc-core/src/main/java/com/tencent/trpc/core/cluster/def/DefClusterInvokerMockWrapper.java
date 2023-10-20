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

package com.tencent.trpc.core.cluster.def;

import com.tencent.trpc.core.cluster.ClusterInvoker;
import com.tencent.trpc.core.common.config.BackendConfig;
import com.tencent.trpc.core.common.config.ConsumerConfig;
import com.tencent.trpc.core.logger.Logger;
import com.tencent.trpc.core.logger.LoggerFactory;
import com.tencent.trpc.core.rpc.InvokeMode;
import com.tencent.trpc.core.rpc.Request;
import com.tencent.trpc.core.rpc.Response;
import com.tencent.trpc.core.rpc.RpcInvocation;
import com.tencent.trpc.core.rpc.common.RpcMethodInfo;
import com.tencent.trpc.core.utils.RpcUtils;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

public class DefClusterInvokerMockWrapper<T> implements ClusterInvoker<T> {

    private static final Logger logger = LoggerFactory.getLogger(DefClusterInvokerMockWrapper.class);

    private final ClusterInvoker<T> invoker;

    public DefClusterInvokerMockWrapper(ClusterInvoker<T> invoker) {
        this.invoker = invoker;
    }

    @Override
    public Class<T> getInterface() {
        return invoker.getInterface();
    }

    @Override
    public CompletionStage<Response> invoke(Request request) {
        ConsumerConfig<T> consumerConfig = invoker.getConfig();
        RpcInvocation invocation = request.getInvocation();
        if (consumerConfig.getMock()) {
            logger.info("mock true: serviceName:{},methodName:{}", invocation.getRpcServiceName(),
                    invocation.getRpcMethodName());
            return doMockInvoke(request);
        }
        return invoker.invoke(request);
    }

    @SuppressWarnings("unchecked")
    private CompletionStage<Response> doMockInvoke(Request request) {
        ConsumerConfig<T> consumerConfig = invoker.getConfig();
        String mockClassName = consumerConfig.getMockClass();
        if (StringUtils.isBlank(mockClassName)) {
            mockClassName = invoker.getInterface().getName() + "Mock";
        }
        CompletableFuture<Response> responseFuture = new CompletableFuture<>();
        try {
            Class<?> mockClass = Class.forName(mockClassName);
            T mockObject = (T) mockClass.newInstance();
            RpcInvocation invocation = request.getInvocation();
            RpcMethodInfo rpcMethodInfo = invocation.getRpcMethodInfo();
            Class<?>[] parameterTypes = rpcMethodInfo.getMethod().getParameterTypes();
            String methodName = rpcMethodInfo.getMethod().getName();
            Object[] params = ArrayUtils.addAll(new Object[]{request.getContext()}, invocation.getArguments());
            Method method = mockObject.getClass().getMethod(methodName, parameterTypes);
            Object result = method.invoke(mockObject, params);
            if (result == null) {
                return null;
            }
            if (InvokeMode.isAsync(invocation.getInvokeMode())) {
                return ((CompletionStage<?>) result).thenApply((obj) -> RpcUtils.newResponse(request, obj, null));
            }
            responseFuture.complete(RpcUtils.newResponse(request, result, null));
        } catch (InvocationTargetException e) {
            responseFuture.completeExceptionally(e.getTargetException());
        } catch (Exception e) {
            responseFuture.completeExceptionally(e);
        }
        return responseFuture;
    }

    @Override
    public ConsumerConfig<T> getConfig() {
        return invoker.getConfig();
    }

    @Override
    public BackendConfig getBackendConfig() {
        return invoker.getBackendConfig();
    }

}
