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

import com.tencent.trpc.core.cluster.AbstractRpcClusterClient;
import com.tencent.trpc.core.cluster.ClusterInterceptorInvoker;
import com.tencent.trpc.core.common.config.BackendConfig;
import com.tencent.trpc.core.common.config.ConsumerConfig;

public class DefRpcClusterClient extends AbstractRpcClusterClient {

    public DefRpcClusterClient(BackendConfig backendConfig) {
        super(backendConfig);
    }

    /**
     * New proxy instance
     *
     * @param config consumer config
     * @param clazz Class
     * @return the instance of serviceInterface {@link ProxyWrapper}
     */
    public <T> ProxyWrapper<T> newProxyWrapper(ConsumerConfig<T> config, Class<T> clazz) {
        DefClusterInvokerMockWrapper<T> mockClusterInvoker = new DefClusterInvokerMockWrapper<>(
                new ClusterInterceptorInvoker<>(new DefClusterInvoker<>(config)));
        T proxy = proxyFactory.getProxy(clazz, new DefClusterInvocationHandler(mockClusterInvoker));
        return new ProxyWrapper<>(mockClusterInvoker, proxy);
    }

}