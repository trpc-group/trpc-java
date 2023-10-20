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

package com.tencent.trpc.core.selector;

import com.tencent.trpc.core.exception.TRpcException;
import com.tencent.trpc.core.exception.TRpcExtensionException;
import com.tencent.trpc.core.extension.InitializingExtension;
import com.tencent.trpc.core.registry.spi.Registry;
import com.tencent.trpc.core.rpc.Request;
import com.tencent.trpc.core.selector.loadbalance.support.RandomLoadBalance;
import com.tencent.trpc.core.selector.spi.Discovery;
import com.tencent.trpc.core.selector.spi.Selector;
import com.tencent.trpc.core.utils.FutureUtils;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AbstractSelector implements Selector, InitializingExtension
 * */
public abstract class AbstractSelector implements Selector, InitializingExtension {

    /**
     * Default loadBalance is RandomLoadBalance
     * */
    protected RandomLoadBalance loadBalance;

    /**
     * The subscribed services and their service discovery centers
     * all service providers cached in Discovery
     * */
    protected Map<String, Discovery> serviceName2Discovery = new ConcurrentHashMap<>();

    /**
     * RegistryCenter
     * */
    protected Registry registryCenter;

    @Override
    public void init() throws TRpcExtensionException {
        loadBalance = new RandomLoadBalance();
    }

    /**
     * Async random select one service instance
     *
     * @param serviceId the identifier of service
     * @param request the request, must not be null
     */
    @Override
    public CompletionStage<ServiceInstance> asyncSelectOne(ServiceId serviceId, Request request) {
        try {
            Discovery discovery = serviceName2Discovery.computeIfAbsent(serviceId.getServiceName(),
                    n -> subscribe(serviceId));
            CompletionStage<ServiceInstance> future = FutureUtils.newFuture();
            ServiceInstance serviceInstance = loadBalance
                    .select(discovery.list(serviceId), request);
            future.toCompletableFuture().complete(serviceInstance);
            return future;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Async random select all service instance
     *
     * @param serviceId the identifier of service
     * @param request the request, must not be null
     */
    @Override
    public CompletionStage<List<ServiceInstance>> asyncSelectAll(ServiceId serviceId,
            Request request) {
        Discovery discovery = serviceName2Discovery.computeIfAbsent(serviceId.getServiceName(),
                n -> subscribe(serviceId));
        CompletionStage<List<ServiceInstance>> future = FutureUtils.newFuture();
        future.toCompletableFuture().complete(discovery.list(serviceId));
        return future;
    }

    @Override
    public void report(ServiceInstance serviceInstance, int code, long costMs)
            throws TRpcException {
    }

    /**
     * Subscribe service。
     * Due to relying solely on serviceName for service discovery,
     * the protocol, host, and port of RegisterInfo here are currently of no practical use
     *
     * @param serviceId subscribe serviceId
     * @return Service Discovery Bound to subscribe service
     */
    protected abstract Discovery subscribe(ServiceId serviceId);
}
