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

package com.tencent.trpc.selector.open.polaris.common;

import com.google.common.base.Preconditions;
import com.tencent.trpc.core.common.config.PluginConfig;
import com.tencent.trpc.core.selector.loadbalance.support.RandomLoadBalance;
import com.tencent.trpc.core.worker.WorkerPoolManager;
import com.tencent.trpc.core.worker.spi.WorkerPool;
import org.apache.commons.collections4.MapUtils;
import java.util.HashMap;
import java.util.Map;

/**
 * Polaris selector Config
 */
public class PolarisSelectorConfig {

    public static final String LOAD_BALANCE = "load_balance";

    public static final String CIRCUIT_BREAKER = "circuit_breaker";

    public static final String WORKER_POOL = "worker_pool";

    private String loadbalance;

    private Map<String, Object> circuitBreakerExtMap = new HashMap<>();

    private WorkerPool workerPool;

    private Map<String, Object> extMap = new HashMap<>();

    public static void validate(PluginConfig selectorConfig) {
        Preconditions.checkNotNull(selectorConfig, "selectConfig can not be null");
    }

    /**
     * To parse the configuration of the polaris plugin, be sure to call validate before calling it
     *
     * @param selectorConfig polaris plugin trpc framework configuration
     * @return polaris config
     */
    @SuppressWarnings("unchecked")
    public static PolarisSelectorConfig parse(PluginConfig selectorConfig) {
        validate(selectorConfig);
        Map<String, Object> extMap = selectorConfig.getProperties();
        PolarisSelectorConfig extConfig = new PolarisSelectorConfig();
        extConfig.extMap.putAll(extMap);
        String loadbalance = MapUtils.getString(extMap, LOAD_BALANCE, RandomLoadBalance.NAME);
        extConfig.setLoadbalance(loadbalance);
        Map cbExtMap = MapUtils.getMap(extMap, CIRCUIT_BREAKER, new HashMap<>());
        extConfig.setCircuitBreakerExtMap((Map<String, Object>) cbExtMap);
        String workPoolId =
                MapUtils.getString(extMap, WORKER_POOL,
                        WorkerPoolManager.DEF_NAMING_WORKER_POOL_NAME);
        WorkerPool workerPool = WorkerPoolManager.get(workPoolId);
        extConfig.setWorkerPool(workerPool);
        return extConfig;
    }

    public void stop() {
    }

    public WorkerPool getWorkerPool() {
        return workerPool;
    }

    public void setWorkerPool(WorkerPool workerPool) {
        this.workerPool = workerPool;
    }

    public String getLoadbalance() {
        return loadbalance;
    }

    public void setLoadbalance(String loadbalance) {
        this.loadbalance = loadbalance;
    }

    public String getMethodLoadBalance(String rpcMethodName) {
        return this.loadbalance;
    }

    public Map<String, Object> getCircuitBreakerExtMap() {
        return circuitBreakerExtMap;
    }

    public PolarisSelectorConfig setCircuitBreakerExtMap(Map<String, Object> circuitBreakerExtMap) {
        this.circuitBreakerExtMap = circuitBreakerExtMap;
        return this;
    }

    public Map<String, Object> getExtMap() {
        return extMap;
    }
}
