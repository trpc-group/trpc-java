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

package com.tencent.trpc.selector.nacos;

import com.tencent.trpc.core.exception.TRpcException;
import com.tencent.trpc.core.exception.TRpcExtensionException;
import com.tencent.trpc.core.extension.DisposableExtension;
import com.tencent.trpc.core.extension.Extension;
import com.tencent.trpc.core.extension.ExtensionLoader;
import com.tencent.trpc.core.extension.InitializingExtension;
import com.tencent.trpc.core.logger.Logger;
import com.tencent.trpc.core.logger.LoggerFactory;
import com.tencent.trpc.core.registry.spi.Registry;
import com.tencent.trpc.core.rpc.Request;
import com.tencent.trpc.core.selector.ServiceId;
import com.tencent.trpc.core.selector.ServiceInstance;
import com.tencent.trpc.core.selector.loadbalance.support.RandomLoadBalance;
import com.tencent.trpc.core.selector.spi.Discovery;
import com.tencent.trpc.core.selector.spi.Selector;
import com.tencent.trpc.registry.center.RegistryCenter;
import com.tencent.trpc.registry.discovery.RegistryDiscovery;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service discovery of Nacos.
 * Support load balancing
 */
@Extension(NacosSelector.NAME)
public class NacosSelector implements Selector, InitializingExtension, DisposableExtension {

    private static final Logger logger = LoggerFactory.getLogger(NacosSelector.class);

    /**
     * Plugin name constant
     */
    public static final String NAME = "nacos";

    /**
     * Random load balancer
     */
    private RandomLoadBalance loadBalance;

    /**
     * All service providers have been cached in Discovery
     * for the subscribed services and their service discovery centers
     */
    private final Map<String, Discovery> serviceNameToDiscovery = new ConcurrentHashMap<>();

    /**
     * Registry center
     */
    private RegistryCenter registryCenter;

    @Override
    public void init() throws TRpcExtensionException {
        registryCenter = (RegistryCenter) ExtensionLoader.getExtensionLoader(Registry.class)
                .getExtension(NAME);
        loadBalance = new RandomLoadBalance();
    }


    /**
     * Async random select one service instance
     * */
    @Override
    public CompletionStage<ServiceInstance> asyncSelectOne(ServiceId serviceId, Request request) {
        try {
            Discovery discovery = serviceNameToDiscovery.computeIfAbsent(serviceId.getServiceName(),
                    n -> subscribe(serviceId));
            return CompletableFuture.supplyAsync(() -> loadBalance
                    .select(discovery.list(serviceId), request));
        } catch (Exception ex) {
            logger.error("[NacosSelector] asyncSelectOne error", ex);
            throw new RuntimeException(ex);
        }
    }

    /**
     * Subscribe to a service. As service discovery rely only on serviceName
     * the protocol, host, and port of RegisterInfo are not currently used
     * @param serviceId   Service to be subscribed
     * @return Discovery   Service discovery Discovery bound to the subscribed service
     */
    private Discovery subscribe(ServiceId serviceId) {
        return new RegistryDiscovery(serviceId, registryCenter);
    }

    /**
     * Async select all service instance from subscribe service
     * */
    @Override
    public CompletionStage<List<ServiceInstance>> asyncSelectAll(ServiceId serviceId, Request request) {
        Discovery discovery = serviceNameToDiscovery.computeIfAbsent(serviceId.getServiceName(),
                n -> subscribe(serviceId));
        return CompletableFuture.supplyAsync(() -> discovery.list(serviceId));
    }

    @Override
    public void report(ServiceInstance serviceInstance, int code, long costMs) throws TRpcException {

    }

    @Override
    public void destroy() throws TRpcExtensionException {
        serviceNameToDiscovery.clear();
    }

    public void setRegistryCenter(RegistryCenter registryCenter) {
        this.registryCenter = registryCenter;
    }

    public void setLoadBalance(RandomLoadBalance loadBalance) {
        this.loadBalance = loadBalance;
    }
}
