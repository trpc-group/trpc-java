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

package com.tencent.trpc.core.cluster;

import com.tencent.trpc.core.common.config.BackendConfig;
import com.tencent.trpc.core.common.config.ConsumerConfig;
import com.tencent.trpc.core.common.config.NamingOptions;
import com.tencent.trpc.core.exception.ErrorCode;
import com.tencent.trpc.core.exception.TRpcException;
import com.tencent.trpc.core.rpc.Invoker;
import com.tencent.trpc.core.rpc.Request;
import com.tencent.trpc.core.rpc.Response;
import com.tencent.trpc.core.selector.SelectorManager;
import com.tencent.trpc.core.selector.ServiceInstance;
import com.tencent.trpc.core.selector.spi.Selector;
import com.tencent.trpc.core.utils.FutureUtils;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

public abstract class AbstractClusterInvoker<T> implements ClusterInvoker<T> {

    protected final ConsumerConfig<T> consumerConfig;

    protected final BackendConfig backendConfig;

    public AbstractClusterInvoker(ConsumerConfig<T> config) {
        this.consumerConfig = Objects.requireNonNull(config);
        this.backendConfig = Objects.requireNonNull(config.getBackendConfig());
    }

    /**
     * Used to find the address of the name service.
     * Interceptors intercept before and after method calls.
     * {@link com.tencent.trpc.core.cluster.spi.ClusterInterceptor#intercept(Invoker, Request)}
     *
     * override {@link com.tencent.trpc.core.rpc.Invoker#invoke(Request)}
     *
     * @param request request object, see
     *         {@link com.tencent.trpc.core.rpc.Invoker#invoke(com.tencent.trpc.core.rpc.Request)}
     * @return CompletionStage of the response
     * @see com.tencent.trpc.core.cluster.spi.ClusterInterceptor#intercept(Invoker, Request)
     */
    @Override
    public CompletionStage<Response> invoke(Request request) {
        NamingOptions namingOptions = backendConfig.getNamingOptions();
        Selector selector = SelectorManager.getManager().get(namingOptions.getSelectorId());
        return Optional.ofNullable(selector).map(s -> {
            Objects.requireNonNull(selector, "selector");
            return doInvoke(request, selector.asyncSelectOne(backendConfig.toNamingServiceId(), request));
        }).orElseGet(() -> FutureUtils.failed(TRpcException.newFrameException(ErrorCode.TRPC_CLIENT_ROUTER_ERR,
                "the selector name:" + namingOptions.getSelectorId() + " not found selector")));
    }

    protected abstract CompletionStage<Response> doInvoke(Request request, CompletionStage<ServiceInstance> instance);

    @Override
    public Class<T> getInterface() {
        return consumerConfig.getServiceInterface();
    }

    @Override
    public ConsumerConfig<T> getConfig() {
        return consumerConfig;
    }

    @Override
    public BackendConfig getBackendConfig() {
        return backendConfig;
    }

}
