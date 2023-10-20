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

package com.tencent.trpc.core.selector.support.def;


import static com.tencent.trpc.core.common.Constants.INCLUDE_CIRCUITBREAK;
import static com.tencent.trpc.core.common.Constants.INCLUDE_UNHEALTHY;

import com.tencent.trpc.core.common.Constants;
import com.tencent.trpc.core.common.config.PluginConfig;
import com.tencent.trpc.core.exception.TRpcExtensionException;
import com.tencent.trpc.core.extension.Extension;
import com.tencent.trpc.core.extension.InitializingExtension;
import com.tencent.trpc.core.extension.PluginConfigAware;
import com.tencent.trpc.core.logger.Logger;
import com.tencent.trpc.core.logger.LoggerFactory;
import com.tencent.trpc.core.rpc.Request;
import com.tencent.trpc.core.selector.ServiceId;
import com.tencent.trpc.core.selector.ServiceInstance;
import com.tencent.trpc.core.selector.circuitbreaker.CircuitBreakerManager;
import com.tencent.trpc.core.selector.discovery.DiscoveryManager;
import com.tencent.trpc.core.selector.loadbalance.LoadBalanceManager;
import com.tencent.trpc.core.selector.router.RouterManager;
import com.tencent.trpc.core.selector.spi.CircuitBreaker;
import com.tencent.trpc.core.selector.spi.Discovery;
import com.tencent.trpc.core.selector.spi.LoadBalance;
import com.tencent.trpc.core.selector.spi.Router;
import com.tencent.trpc.core.selector.spi.Selector;
import com.tencent.trpc.core.worker.WorkerPoolManager;
import com.tencent.trpc.core.worker.spi.WorkerPool;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

@Extension(AssembleSelector.NAME)
public class AssembleSelector implements Selector, PluginConfigAware, InitializingExtension {

    public static final String NAME = "assemble";

    private static final Logger LOG = LoggerFactory.getLogger(AssembleSelector.class);

    /**
     * Service discovery
     * */
    private Discovery discovery;

    /**
     * Service router
     * */
    private Router router;

    /**
     * Service loadBalance
     * */
    private LoadBalance loadBalance;

    /**
     * Service circuitBreaker
     * */
    private CircuitBreaker circuitBreaker;

    /**
     * Worker thread pool
     * */
    private WorkerPool workerPool;

    private PluginConfig config;

    public AssembleSelector() {

    }

    @Override
    public void setPluginConfig(PluginConfig pluginConfig) throws TRpcExtensionException {
        this.config = pluginConfig;
    }

    @Override
    public void init() throws TRpcExtensionException {
        Objects.requireNonNull(config, "config");
        AssembleSelectorConfig selectorConfig =
                AssembleSelectorConfig.parse(config.getName(), config.getProperties());
        this.discovery = DiscoveryManager.getManager().get(selectorConfig.getDiscovery());
        this.loadBalance = LoadBalanceManager.getManager().get(selectorConfig.getLoadbalance());
        this.router = RouterManager.getManager().get(selectorConfig.getRouter());
        this.circuitBreaker =
                CircuitBreakerManager.getManager().get(selectorConfig.getCircuitBreaker());
        this.workerPool = WorkerPoolManager.get(selectorConfig.getWorkerPool());
    }

    @Override
    public void warmup(ServiceId serviceId) {
        discovery.asyncList(serviceId, workerPool.toExecutor()).toCompletableFuture().join();
    }

    /**
     * Async select one instance
     * */
    @Override
    public CompletionStage<ServiceInstance> asyncSelectOne(ServiceId serviceId, Request request) {
        // Step1 service discovery,contains unhealthy instance
        serviceId.getParameters().put(INCLUDE_UNHEALTHY, true);
        serviceId.getParameters().put(INCLUDE_CIRCUITBREAK, true);

        //Return contains unhealthy instance list
        CompletionStage<List<ServiceInstance>> serviceListFuture =
                discovery.asyncList(serviceId, workerPool.toExecutor());
        return serviceListFuture.thenApply((serviceList) -> {
            //Put the meta info to valueMap,and by used when service router
            if (serviceId.getParameters().containsKey(Constants.METADATA)) {
                request.getContext().getValueMap()
                        .putIfAbsent(Constants.METADATA, serviceId.getObject(Constants.METADATA, null));
            }
            // Step2 service router
            serviceList = router.route(serviceList, request);
            // step3 service loadBalance
            ServiceInstance select = loadBalance.select(serviceList, request);
            if (select == null) {
                LOG.debug("[assembleSelector] load balance return null, serviceList:{}", serviceList);
                return null;
            }
            // Step4 service circuitBreaker
            if (circuitBreaker.allowRequest(select)) {
                return select;
            }
            // Step5 When the instance obtained from loadBalance is not available,
            // Traverse the entire service instance list to determine the available instance,
            // and then perform loadBalance judgment
            List<ServiceInstance> availableServiceList = serviceList.stream()
                    .filter(si -> circuitBreaker.allowRequest(si)).collect(Collectors.toList());
            return loadBalance.select(availableServiceList, request);
        });
    }

    /**
     * Async select all allowRequest instance
     * */
    @Override
    public CompletionStage<List<ServiceInstance>> asyncSelectAll(ServiceId serviceId,
            Request request) {
        return discovery.asyncList(serviceId, workerPool.toExecutor())
                .thenApply(serviceList -> {
            if (serviceList.isEmpty()) {
                return serviceList;
            }
            return serviceList.stream().filter(si -> circuitBreaker.allowRequest(si))
                    .collect(Collectors.toList());
        });
    }

    @Override
    public void report(ServiceInstance serviceInstance, int code, long costMs) {
        circuitBreaker.report(serviceInstance, code, costMs);
    }
}
