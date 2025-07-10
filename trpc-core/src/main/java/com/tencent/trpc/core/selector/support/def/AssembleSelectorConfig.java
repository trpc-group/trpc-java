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

package com.tencent.trpc.core.selector.support.def;

import com.tencent.trpc.core.common.config.PluginConfig;
import com.tencent.trpc.core.selector.circuitbreaker.support.NoneCircuitBreaker;
import com.tencent.trpc.core.selector.discovery.DiscoveryManager;
import com.tencent.trpc.core.selector.loadbalance.support.RandomLoadBalance;
import com.tencent.trpc.core.selector.router.support.NoneRouter;
import com.tencent.trpc.core.utils.PreconditionUtils;
import com.tencent.trpc.core.worker.WorkerPoolManager;
import java.util.Map;
import java.util.Objects;
import org.apache.commons.collections4.MapUtils;

public class AssembleSelectorConfig {

    public static final String DISCOVERY = "discovery";
    public static final String ROUTER = "router";
    public static final String LOAD_BALANCE = "loadbalance";
    public static final String CIRCUIT_BREAKER = "circuitbreaker";
    public static final String WORK_POOL = "workpool";

    private String name;
    private String discovery;
    private String router;
    private String loadbalance;
    private String circuitBreaker;
    private String workerPool;

    /**
     * Build AssembleSelectorConfig from PluginConfig properties map
     * */
    public static final AssembleSelectorConfig parse(String name, Map<String, Object> map) {
        AssembleSelectorConfig config = new AssembleSelectorConfig();
        config.name = Objects.requireNonNull(name, "name");
        config.discovery = MapUtils.getString(map, DISCOVERY,
                DiscoveryManager.getManager().getDefaultPluginName());
        config.router = MapUtils.getString(map, ROUTER, NoneRouter.NAME);
        config.circuitBreaker = MapUtils.getString(map, CIRCUIT_BREAKER, NoneCircuitBreaker.NAME);
        config.loadbalance = MapUtils.getString(map, LOAD_BALANCE, RandomLoadBalance.NAME);
        config.workerPool =
                MapUtils.getString(map, WORK_POOL, WorkerPoolManager.DEF_NAMING_WORKER_POOL_NAME);
        config.validate();
        return config;
    }

    public static final void validate(PluginConfig selectorConfig) {
        parse(selectorConfig.getName(), selectorConfig.getProperties()).validate();
    }

    public void validate() {
        PreconditionUtils.checkArgument(discovery != null,
                "AssembleSelector[%s], found discovery is null", name);
        PreconditionUtils.checkArgument(router != null,
                "AssembleSelector[%s], found router is null", name);
        PreconditionUtils.checkArgument(loadbalance != null,
                "AssembleSelector[%s],found loadbalance is null", name);
        PreconditionUtils.checkArgument(circuitBreaker != null,
                "AssembleSelector[%s], found circuitBreaker is null", name);
    }

    public String getRouter() {
        return router;
    }

    public void setRouter(String router) {
        this.router = router;
    }

    public String getDiscovery() {
        return discovery;
    }

    public void setDiscovery(String discovery) {
        this.discovery = discovery;
    }

    public String getCircuitBreaker() {
        return circuitBreaker;
    }

    public AssembleSelectorConfig setCircuitBreaker(String circuitBreaker) {
        this.circuitBreaker = circuitBreaker;
        return this;
    }

    public String getLoadbalance() {
        return loadbalance;
    }

    public void setLoadbalance(String loadbalance) {
        this.loadbalance = loadbalance;
    }

    public String getWorkerPool() {
        return workerPool;
    }

    public void setWorkerPool(String workerPool) {
        this.workerPool = workerPool;
    }

    public String getMethodLoadBalance(String rpcMethodName) {
        return loadbalance;
    }
}
