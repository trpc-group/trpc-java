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

package com.tencent.trpc.core.cluster;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.tencent.trpc.core.common.LifecycleBase;
import com.tencent.trpc.core.common.config.BackendConfig;
import com.tencent.trpc.core.common.config.ConsumerConfig;
import com.tencent.trpc.core.exception.TRpcException;
import com.tencent.trpc.core.extension.ExtensionLoader;
import com.tencent.trpc.core.logger.Logger;
import com.tencent.trpc.core.logger.LoggerFactory;
import com.tencent.trpc.core.proxy.spi.ProxyFactory;
import java.util.Objects;
import java.util.concurrent.ConcurrentMap;

@SuppressWarnings("unchecked")
public abstract class AbstractRpcClusterClient extends LifecycleBase implements RpcClusterClient {

    private static final Logger logger = LoggerFactory.getLogger(AbstractRpcClusterClient.class);

    protected BackendConfig backendConfig;

    protected ProxyFactory proxyFactory;

    private final ConcurrentMap<Class, ProxyWrapper> clazzToProxyWrapper = Maps.newConcurrentMap();

    public AbstractRpcClusterClient(BackendConfig backendConfig) {
        this.backendConfig = backendConfig;
    }

    @Override
    public <T> T getProxy(ConsumerConfig<T> config) {
        Preconditions.checkArgument(config.getBackendConfig() == backendConfig,
                "ConsumerConfig's backend should = RpcClusterClient's backend");
        Class<T> serviceInterface = config.getServiceInterface();
        return (T) clazzToProxyWrapper.computeIfAbsent(serviceInterface, clazz -> newProxyWrapper(config, clazz))
                .getProxy();
    }

    protected abstract <T> ProxyWrapper<T> newProxyWrapper(ConsumerConfig<T> config, Class<T> clazz);

    @Override
    public void initInternal() {
        Objects.requireNonNull(backendConfig, "backendConfig is null");
        proxyFactory = ExtensionLoader.getExtensionLoader(ProxyFactory.class)
                .getExtension(backendConfig.getProxyType());
    }

    @Override
    public void stopInternal() {
        try {
            clazzToProxyWrapper.clear();
        } catch (Exception ex) {
            logger.error("Stop cluster[" + backendConfig.getName() + "] fail", ex);
        }
    }

    @Override
    public BackendConfig getConfig() {
        return backendConfig;
    }

    @Override
    public void open() throws TRpcException {
        super.start();
    }

    @Override
    public boolean isAvailable() {
        return super.isStarted();
    }

    @Override
    public boolean isClosed() {
        return super.isFailed() || super.isStopping() || super.isStopped();
    }

    @Override
    public void close() {
        super.stop();
    }

    /**
     * ClusterInvoker and ProxyFactory wrapper class
     *
     * @param <T>
     */
    public static class ProxyWrapper<T> {

        private final ClusterInvoker<T> invoker;

        private final T proxy;

        public ProxyWrapper(ClusterInvoker<T> invoker, T proxy) {
            super();
            this.invoker = invoker;
            this.proxy = proxy;
        }

        public ClusterInvoker<T> getInvoker() {
            return invoker;
        }

        public T getProxy() {
            return proxy;
        }
    }

}
