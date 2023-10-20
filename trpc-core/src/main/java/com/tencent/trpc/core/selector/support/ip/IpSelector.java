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

package com.tencent.trpc.core.selector.support.ip;

import com.tencent.trpc.core.exception.TRpcExtensionException;
import com.tencent.trpc.core.extension.Extension;
import com.tencent.trpc.core.extension.InitializingExtension;
import com.tencent.trpc.core.rpc.Request;
import com.tencent.trpc.core.selector.ServiceId;
import com.tencent.trpc.core.selector.ServiceInstance;
import com.tencent.trpc.core.selector.discovery.ListDiscovery;
import com.tencent.trpc.core.selector.loadbalance.support.RandomLoadBalance;
import com.tencent.trpc.core.selector.spi.Selector;
import com.tencent.trpc.core.utils.FutureUtils;
import java.util.List;
import java.util.concurrent.CompletionStage;

/**
 * Ip selector
 * */
@Extension(IpSelector.NAME)
public class IpSelector implements Selector, InitializingExtension {

    public static final String NAME = "ip";

    private ListDiscovery discovery;

    private RandomLoadBalance loadBalance;

    public IpSelector() {
    }

    @Override
    public void init() throws TRpcExtensionException {
        discovery = new ListDiscovery();
        loadBalance = new RandomLoadBalance();
        discovery.init();
    }

    @Override
    public void warmup(ServiceId serviceId) {
        selectAll(serviceId);
    }


    /**
     * Async select one instance from loadBalance all
     * */
    @Override
    public CompletionStage<ServiceInstance> asyncSelectOne(ServiceId serviceId, Request request) {
        try {
            CompletionStage<ServiceInstance> future = FutureUtils.newFuture();
            future.toCompletableFuture()
                    .complete(loadBalance.select(selectAll(serviceId), request));
            return future;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Async select all instance from discovery
     * */
    @Override
    public CompletionStage<List<ServiceInstance>> asyncSelectAll(ServiceId serviceId,
            Request request) {
        CompletionStage<List<ServiceInstance>> future = FutureUtils.newFuture();
        future.toCompletableFuture().complete(selectAll(serviceId));
        return future;
    }

    /**
     * Select all instance from discovery
     * */
    public List<ServiceInstance> selectAll(ServiceId serviceId) {
        try {
            return discovery.list(serviceId);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public void report(ServiceInstance serviceInstance, int code, long costMs) {
    }
}
