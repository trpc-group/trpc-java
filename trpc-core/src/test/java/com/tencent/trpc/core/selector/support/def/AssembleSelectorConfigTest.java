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

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.tencent.trpc.core.common.config.PluginConfig;
import com.tencent.trpc.core.extension.MockSelector;
import com.tencent.trpc.core.selector.spi.Selector;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class AssembleSelectorConfigTest {

    private AssembleSelectorConfig assembleSelectorConfig;

    private PluginConfig pluginConfig;

    @BeforeEach
    public void setUp() throws Exception {
        Map<String, Object> config = new HashMap<>();
        config.put("discovery", "discovery");
        config.put("router", "router");
        config.put("loadbalance", "loadBalance");
        config.put("circuitbreaker", "circuitBreaker");
        config.put("workpool", "workerPool");
        pluginConfig = new PluginConfig("a", Selector.class, MockSelector.class, config);
        assembleSelectorConfig = AssembleSelectorConfig.parse("conf", config);
    }

    @Test
    public void testValidate() {
        assembleSelectorConfig.validate();
    }

    @Test
    public void testValidateStatic() {
        AssembleSelectorConfig.validate(pluginConfig);
    }

    @Test
    public void testGetRouter() {
        assertEquals("router", assembleSelectorConfig.getRouter());
    }

    @Test
    public void testSetRouter() {
        assembleSelectorConfig.setRouter("router1");
        assertEquals("router1", assembleSelectorConfig.getRouter());
    }

    @Test
    public void testGetDiscovery() {
        assertEquals("discovery", assembleSelectorConfig.getDiscovery());
    }

    @Test
    public void testSetDiscovery() {
        assembleSelectorConfig.setDiscovery("discovery1");
        assertEquals("discovery1", assembleSelectorConfig.getDiscovery());
    }

    @Test
    public void testGetCircuitBreaker() {
        assertEquals("circuitBreaker", assembleSelectorConfig.getCircuitBreaker());
    }

    @Test
    public void testSetCircuitBreaker() {
        assembleSelectorConfig.setCircuitBreaker("circuitBreaker1");
        assertEquals("circuitBreaker1", assembleSelectorConfig.getCircuitBreaker());
    }

    @Test
    public void testGetLoadbalance() {
        assertEquals("loadBalance", assembleSelectorConfig.getLoadbalance());
    }

    @Test
    public void testSetLoadbalance() {
        assembleSelectorConfig.setLoadbalance("loadBalance1");
        assertEquals("loadBalance1", assembleSelectorConfig.getLoadbalance());
    }

    @Test
    public void testGetWorkerPool() {
        assertEquals("workerPool", assembleSelectorConfig.getWorkerPool());
    }

    @Test
    public void testSetWorkerPool() {
        assembleSelectorConfig.setWorkerPool("workerPool1");
        assertEquals("workerPool1", assembleSelectorConfig.getWorkerPool());
    }

    @Test
    public void testGetMethodLoadBalance() {
        assertEquals("loadBalance", assembleSelectorConfig.getMethodLoadBalance("1"));

    }
}
