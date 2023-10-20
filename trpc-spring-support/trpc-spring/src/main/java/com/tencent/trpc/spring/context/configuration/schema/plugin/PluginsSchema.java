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

package com.tencent.trpc.spring.context.configuration.schema.plugin;

import com.google.common.collect.Maps;
import java.util.Map;

/**
 * Configuration for plugins
 *
 * @see com.tencent.trpc.core.common.config.PluginConfig
 */
public class PluginsSchema {

    /**
     * WorkerPool plugin configs
     */
    private Map<String, Map<String, Object>> workerPool = Maps.newHashMap();

    /**
     * Config center plugin configs
     */
    private Map<String, Map<String, Object>> config = Maps.newHashMap();

    /**
     * Filter plugin configs
     */
    private Map<String, Map<String, Object>> filter = Maps.newHashMap();

    /**
     * Tracing plugin configs
     */
    private Map<String, Map<String, Object>> tracing = Maps.newHashMap();

    /**
     * Telemetry plugin configs
     */
    private Map<String, Map<String, Object>> telemetry = Maps.newHashMap();

    /**
     * Selector plugin configs
     */
    private Map<String, Map<String, Object>> selector = Maps.newHashMap();

    /**
     * Discovery plugin configs
     */
    private Map<String, Map<String, Object>> discovery = Maps.newHashMap();

    /**
     * Load balance plugin configs
     */
    private Map<String, Map<String, Object>> loadbalance = Maps.newHashMap();

    /**
     * Circuit breaker plugin configs
     */
    private Map<String, Map<String, Object>> circuitbreaker = Maps.newHashMap();

    /**
     * Router plugin configs
     */
    private Map<String, Map<String, Object>> router = Maps.newHashMap();

    /**
     * Registry plugin configs
     */
    private Map<String, Map<String, Object>> registry = Maps.newHashMap();

    /**
     * Remote log plugin configs
     */
    private Map<String, Map<String, Object>> remoteLog = Maps.newHashMap();

    /**
     * Metrics plugin configs
     */
    private Map<String, Map<String, Object>> metrics = Maps.newHashMap();

    /**
     * Limiter plugin configs
     */
    private Map<String, Map<String, Object>> limiter = Maps.newHashMap();

    public Map<String, Map<String, Object>> getWorkerPool() {
        return workerPool;
    }

    public void setWorkerPool(Map<String, Map<String, Object>> workerPool) {
        this.workerPool = workerPool;
    }

    public Map<String, Map<String, Object>> getConfig() {
        return config;
    }

    public void setConfig(Map<String, Map<String, Object>> config) {
        this.config = config;
    }

    public Map<String, Map<String, Object>> getFilter() {
        return filter;
    }

    public void setFilter(Map<String, Map<String, Object>> filter) {
        this.filter = filter;
    }

    public Map<String, Map<String, Object>> getTracing() {
        return tracing;
    }

    public void setTracing(Map<String, Map<String, Object>> tracing) {
        this.tracing = tracing;
    }

    public Map<String, Map<String, Object>> getTelemetry() {
        return telemetry;
    }

    public void setTelemetry(Map<String, Map<String, Object>> telemetry) {
        this.telemetry = telemetry;
    }

    public Map<String, Map<String, Object>> getSelector() {
        return selector;
    }

    public void setSelector(Map<String, Map<String, Object>> selector) {
        this.selector = selector;
    }

    public Map<String, Map<String, Object>> getDiscovery() {
        return discovery;
    }

    public void setDiscovery(Map<String, Map<String, Object>> discovery) {
        this.discovery = discovery;
    }

    public Map<String, Map<String, Object>> getLoadbalance() {
        return loadbalance;
    }

    public void setLoadbalance(Map<String, Map<String, Object>> loadbalance) {
        this.loadbalance = loadbalance;
    }

    public Map<String, Map<String, Object>> getCircuitbreaker() {
        return circuitbreaker;
    }

    public void setCircuitbreaker(Map<String, Map<String, Object>> circuitbreaker) {
        this.circuitbreaker = circuitbreaker;
    }

    public Map<String, Map<String, Object>> getRouter() {
        return router;
    }

    public void setRouter(Map<String, Map<String, Object>> router) {
        this.router = router;
    }

    public Map<String, Map<String, Object>> getRegistry() {
        return registry;
    }

    public void setRegistry(Map<String, Map<String, Object>> registry) {
        this.registry = registry;
    }

    public Map<String, Map<String, Object>> getRemoteLog() {
        return remoteLog;
    }

    public void setRemoteLog(Map<String, Map<String, Object>> remoteLog) {
        this.remoteLog = remoteLog;
    }

    public Map<String, Map<String, Object>> getMetrics() {
        return metrics;
    }

    public void setMetrics(Map<String, Map<String, Object>> metrics) {
        this.metrics = metrics;
    }

    public Map<String, Map<String, Object>> getLimiter() {
        return limiter;
    }

    public void setLimiter(Map<String, Map<String, Object>> limiter) {
        this.limiter = limiter;
    }

    @Override
    public String toString() {
        return "PluginsSchema{" +
                "workerPool=" + workerPool +
                ", config=" + config +
                ", filter=" + filter +
                ", tracing=" + tracing +
                ", telemetry=" + telemetry +
                ", selector=" + selector +
                ", discovery=" + discovery +
                ", loadbalance=" + loadbalance +
                ", circuitbreaker=" + circuitbreaker +
                ", router=" + router +
                ", registry=" + registry +
                ", remoteLog=" + remoteLog +
                ", metrics=" + metrics +
                ", limiter=" + limiter +
                '}';
    }
}
