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

import com.tencent.trpc.core.cluster.spi.ClusterInterceptor;
import com.tencent.trpc.core.common.config.BackendConfig;
import com.tencent.trpc.core.common.config.ConsumerConfig;
import com.tencent.trpc.core.rpc.Request;
import com.tencent.trpc.core.rpc.Response;
import com.tencent.trpc.core.utils.CollectionUtils;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

public class ClusterInterceptorInvoker<T> implements ClusterInvoker<T> {

    private ClusterInvoker<T> invoker;

    public ClusterInterceptorInvoker(ClusterInvoker<T> target) {
        this.invoker = buildClusterInterceptorInvokerChain(target, target.getBackendConfig().getInterceptors());
    }

    private ClusterInvoker<T> buildClusterInterceptorInvokerChain(ClusterInvoker<T> target, List<String> interceptors) {
        if (CollectionUtils.isEmpty(interceptors)) {
            return target;
        }
        List<ClusterInterceptor> clusterInterceptors = interceptors.stream().map(ClusterInterceptorManager::get)
                .sorted(Comparator.comparing(ClusterInterceptor::getOrder)).collect(Collectors.toList());
        ClusterInvoker<T> lastClusterInvoker = target;
        for (int i = clusterInterceptors.size() - 1; i >= 0; i--) {
            final ClusterInterceptor interceptor = clusterInterceptors.get(i);
            final ClusterInvoker<T> before = lastClusterInvoker;
            lastClusterInvoker = new ClusterInvoker<T>() {
                @Override
                public ConsumerConfig<T> getConfig() {
                    return target.getConfig();
                }

                @Override
                public BackendConfig getBackendConfig() {
                    return target.getBackendConfig();
                }

                @Override
                public Class<T> getInterface() {
                    return target.getInterface();
                }

                @Override
                public CompletionStage<Response> invoke(Request request) {
                    CompletionStage<Response> intercept = interceptor.intercept(before, request);
                    return Objects.requireNonNull(intercept, "the intercept response can't be null");
                }
            };
        }
        return lastClusterInvoker;
    }

    @Override
    public Class<T> getInterface() {
        return invoker.getInterface();
    }

    @Override
    public CompletionStage<Response> invoke(Request request) {
        return invoker.invoke(request);
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
