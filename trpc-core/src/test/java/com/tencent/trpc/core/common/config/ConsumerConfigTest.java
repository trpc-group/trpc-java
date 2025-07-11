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

package com.tencent.trpc.core.common.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.tencent.trpc.core.common.ConfigManager;
import com.tencent.trpc.core.common.config.BackendConfigTest.RemoteLoggerTest;
import com.tencent.trpc.core.extension.ExtensionLoader;
import com.tencent.trpc.core.filter.spi.Filter;
import com.tencent.trpc.core.rpc.GenericClient;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ConsumerConfigTest {

    @Before
    public void before() {
        ConfigManager.stopTest();
        ConfigManager.startTest();
    }

    @After
    public void after() {
        ConfigManager.stopTest();
    }

    @Test
    public void test() {
        ExtensionLoader
                .registerPlugin(new PluginConfig("attalog", Filter.class, RemoteLoggerTest.class));
        ConsumerConfig<GenericClient> config = new ConsumerConfig<>();
        config.setBackendConfig(new BackendConfig());
        config.getBackendConfig().setNamingUrl("ip://127.0.0.1:12345");
        config.getBackendConfig().setRequestTimeout(10);
        config.setServiceInterface(GenericClient.class);
        config.setMock(Boolean.TRUE);
        config.setMockClass("aaa");
        assertEquals("ip://127.0.0.1:12345", config.getBackendConfig().getNamingUrl());
        assertEquals(10, config.getBackendConfig().getRequestTimeout());
        assertTrue(config.isGeneric());
        assertNull(config.getLocalAddress());
        assertTrue(config.toString().contains("ConsumerConfig [serviceInterface="));
        assertEquals("aaa", config.getMockClass());
        assertTrue(config.getMock());
        ConsumerConfig<GenericClient> clone = config.clone();
        assertNotNull(clone);
        assertEquals("ip://127.0.0.1:12345", clone.getBackendConfig().getNamingUrl());
        assertEquals(10, clone.getBackendConfig().getRequestTimeout());
        assertTrue(clone.isGeneric());
        assertNull(clone.getLocalAddress());
        assertTrue(clone.toString().contains("ConsumerConfig [serviceInterface="));
        assertEquals("aaa", clone.getMockClass());
        assertTrue(clone.getMock());
        assertNotNull(config.getProxy());
        assertNotNull(config.getProxyWithSourceSet("a"));
        assertNotNull(config.getProxyWithDestinationSet("b"));
    }
}