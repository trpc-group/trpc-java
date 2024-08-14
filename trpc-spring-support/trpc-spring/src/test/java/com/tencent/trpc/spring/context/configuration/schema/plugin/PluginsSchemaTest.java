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

import java.util.HashMap;
import java.util.Map;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class PluginsSchemaTest {

    private PluginsSchema pluginsSchema;

    @Before
    public void setUp() {
        pluginsSchema = new PluginsSchema();
    }

    @Test
    public void testSetConfig() {
        Map<String, Map<String, Object>> configMap = new HashMap<>();
        configMap.put("k1", new HashMap<>());
        pluginsSchema.setConfig(configMap);
        Map<String, Map<String, Object>> config = pluginsSchema.getConfig();
        Assert.assertEquals(configMap.size(), config.size());
    }

    @Test
    public void testSetTracing() {
        Map<String, Map<String, Object>> traceMap = new HashMap<>();
        traceMap.put("trace1", new HashMap<>());
        pluginsSchema.setTracing(traceMap);
        Map<String, Map<String, Object>> config = pluginsSchema.getTracing();
        Assert.assertEquals(traceMap.size(), config.size());
    }

    @Test
    public void testSetTelemetry() {
        Map<String, Map<String, Object>> telemetryMap = new HashMap<>();
        telemetryMap.put("t1", new HashMap<>());
        pluginsSchema.setTelemetry(telemetryMap);
        Map<String, Map<String, Object>> config = pluginsSchema.getTelemetry();
        Assert.assertEquals(telemetryMap.size(), config.size());
    }

    @Test
    public void testSetSelector() {
        Map<String, Map<String, Object>> selectMap = new HashMap<>();
        selectMap.put("selectMapKey", new HashMap<>());
        pluginsSchema.setSelector(selectMap);
        Map<String, Map<String, Object>> config = pluginsSchema.getSelector();
        Assert.assertEquals(selectMap.size(), config.size());
    }

    @Test
    public void testSetDiscovery() {
        Map<String, Map<String, Object>> discoveryMap = new HashMap<>();
        discoveryMap.put("discoveryKey", new HashMap<>());
        pluginsSchema.setDiscovery(discoveryMap);
        Map<String, Map<String, Object>> config = pluginsSchema.getDiscovery();
        Assert.assertEquals(discoveryMap.size(), config.size());
    }

    @Test
    public void testSetLoadbalance() {
        Map<String, Map<String, Object>> loadMap = new HashMap<>();
        loadMap.put("loadKey", new HashMap<>());
        pluginsSchema.setLoadbalance(loadMap);
        Map<String, Map<String, Object>> config = pluginsSchema.getLoadbalance();
        Assert.assertEquals(loadMap.size(), config.size());
    }

    @Test
    public void testSetCircuitbreaker() {
        Map<String, Map<String, Object>> circuitbreakerMap = new HashMap<>();
        circuitbreakerMap.put("circuitbreakerKey", new HashMap<>());
        pluginsSchema.setCircuitbreaker(circuitbreakerMap);
        Map<String, Map<String, Object>> config = pluginsSchema.getCircuitbreaker();
        Assert.assertEquals(circuitbreakerMap.size(), config.size());
    }

    @Test
    public void testSetRouter() {
        Map<String, Map<String, Object>> routerMap = new HashMap<>();
        routerMap.put("routerKey", new HashMap<>());
        pluginsSchema.setRouter(routerMap);
        Map<String, Map<String, Object>> config = pluginsSchema.getRouter();
        Assert.assertEquals(routerMap.size(), config.size());
    }

    @Test
    public void testSetRegistry() {
        Map<String, Map<String, Object>> registryMap = new HashMap<>();
        registryMap.put("registryKey", new HashMap<>());
        pluginsSchema.setRegistry(registryMap);
        Map<String, Map<String, Object>> config = pluginsSchema.getRegistry();
        Assert.assertEquals(registryMap.size(), config.size());
    }

    @Test
    public void testSetRemoteLog() {
        Map<String, Map<String, Object>> remoteLogMap = new HashMap<>();
        remoteLogMap.put("remoteLogKey", new HashMap<>());
        pluginsSchema.setRemoteLog(remoteLogMap);
        Map<String, Map<String, Object>> config = pluginsSchema.getRemoteLog();
        Assert.assertEquals(remoteLogMap.size(), config.size());
    }

    @Test
    public void testSetMetrics() {
        Map<String, Map<String, Object>> metricsMap = new HashMap<>();
        metricsMap.put("metricsKey", new HashMap<>());
        pluginsSchema.setMetrics(metricsMap);
        Map<String, Map<String, Object>> config = pluginsSchema.getMetrics();
        Assert.assertEquals(metricsMap.size(), config.size());
    }

    @Test
    public void testSetLimiter() {
        Map<String, Map<String, Object>> limitMap = new HashMap<>();
        limitMap.put("limitKey", new HashMap<>());
        pluginsSchema.setLimiter(limitMap);
        Map<String, Map<String, Object>> config = pluginsSchema.getLimiter();
        Assert.assertEquals(limitMap.size(), config.size());
    }

    @Test
    public void testSetFilter() {
        Map<String, Map<String, Object>> filterMap = new HashMap<>();
        filterMap.put("filterKey", new HashMap<>());
        pluginsSchema.setFilter(filterMap);
        Map<String, Map<String, Object>> config = pluginsSchema.getFilter();
        Assert.assertEquals(filterMap.size(), config.size());
    }

    @Test
    public void testToString() {
        Map<String, Map<String, Object>> filterMap = new HashMap<>();
        filterMap.put("filterKey", new HashMap<>());
        pluginsSchema.setFilter(filterMap);
        String schemaStr = pluginsSchema.toString();
        Assert.assertNotNull(schemaStr);
        Assert.assertTrue(schemaStr.contains("filter={filterKey={}}"));
    }
}
