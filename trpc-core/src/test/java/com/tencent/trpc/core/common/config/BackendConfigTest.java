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

package com.tencent.trpc.core.common.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.tencent.trpc.core.common.ConfigManager;
import com.tencent.trpc.core.common.Constants;
import com.tencent.trpc.core.extension.ExtensionLoader;
import com.tencent.trpc.core.filter.spi.Filter;
import com.tencent.trpc.core.logger.RemoteLoggerFilter;
import com.tencent.trpc.core.rpc.GenericClient;
import com.tencent.trpc.core.selector.ServiceId;
import com.tencent.trpc.core.worker.support.thread.ThreadWorkerPool;
import java.util.HashMap;
import java.util.Map;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class BackendConfigTest {

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
    public void testEquals() {
        // case 0
        BackendConfig a = new BackendConfig();
        a.setNamingUrl("a://b");
        a.setDefault();
        BackendConfig b = new BackendConfig();
        b.setNamingUrl("a://b");
        b.setDefault();
        assertNotEquals(a, b);
        // case 1
        BackendConfig a1 = new BackendConfig();
        BackendConfig b1 = new BackendConfig();
        assertNotEquals(a1, b1);
        // case 2
        BackendConfig a2 = new BackendConfig();
        a2.setName("");
        BackendConfig b2 = new BackendConfig();
        b2.setName("");
        assertNotEquals(a1, b1);
    }

    @Test
    public void testDefault() {
        BackendConfig config = new BackendConfig();
        config.setNamingUrl("a://b");
        config.setDefault();
        assertEquals(Constants.DEFAULT_PROTOCOL, config.getProtocol());
        assertEquals(Constants.DEFAULT_SERIALIZATION, config.getSerialization());
        assertEquals(Constants.DEFAULT_COMPRESSOR, config.getCompressor());
        assertEquals(true, config.isKeepAlive());
        assertEquals(Constants.DEFAULT_TRANSPORTER, config.getTransporter());
        assertEquals(Constants.DEFAULT_NETWORK_TYPE, config.getNetwork());
        assertEquals(16384, config.getReceiveBuffer());
        assertEquals(16384, config.getSendBuffer());
        assertEquals(180000, config.getIdleTimeout().intValue());
        assertEquals(false, config.isLazyinit());
        assertEquals(2, config.getConnsPerAddr());
        assertEquals(1000, config.getConnTimeout());
        assertEquals(true, config.isIoThreadGroupShare());
        assertEquals(Constants.DEFAULT_IO_THREADS, config.getIoThreads());
        assertEquals(1000, config.getRequestTimeout());
    }

    @Test
    public void testConfig() {
        BackendConfig config = new BackendConfig();
        config.setNamingUrl("a://b");
        config.setTarget("a://b");
        config.setRequestTimeout(10);
        config.setProtocol("trpc");
        config.setSerialization("pb");
        config.setCompressor("gzip");
        config.setKeepAlive(false);
        config.setTransporter("transporter");
        config.setNetwork("network");
        config.setReceiveBuffer(30);
        config.setSendBuffer(40);
        config.setIdleTimeout(60);
        config.setLazyinit(true);
        config.setConnsPerAddr(70);
        config.setConnTimeout(80);
        config.setIoThreadGroupShare(true);
        config.setIoThreads(1000);
        config.setBasePath("/trpc");
        config.setCompressMinBytes(10);
        config.setWorkerPool("thread");
        config.setDefault();
        assertEquals("trpc", config.getProtocol());
        assertEquals("pb", config.getSerialization());
        assertEquals("gzip", config.getCompressor());
        assertEquals(false, config.isKeepAlive());
        assertEquals("transporter", config.getTransporter());
        assertEquals("network", config.getNetwork());
        assertEquals(30, config.getReceiveBuffer());
        assertEquals(40, config.getSendBuffer());
        assertEquals(60, config.getIdleTimeout().intValue());
        assertEquals(true, config.isLazyinit());
        assertEquals(true, config.getLazyinit());
        assertEquals(70, config.getConnsPerAddr());
        assertEquals(80, config.getConnTimeout());
        assertEquals(10, config.getRequestTimeout());
        assertEquals(true, config.isIoThreadGroupShare());
        assertEquals(1000, config.getIoThreads());
        assertEquals("/trpc", config.getBasePath());
        assertEquals(10, config.getCompressMinBytes());
        assertEquals("thread", config.getWorkerPool());
        assertNull(config.getLocalAddress());
        assertNull(config.getWorkerPoolObj());
        assertFalse(config.isStoped());
        ProtocolConfig tcp = config.generateProtocolConfig("127.0.0.1", 8080, "tcp");
        assertEquals("127.0.0.1", tcp.getIp());
        assertEquals(8080, tcp.getPort());
        assertEquals("tcp", tcp.getNetwork());
        ProtocolConfig udp = config.generateProtocolConfig("127.0.0.2", 8081, "udp", new HashMap<>());
        assertEquals("127.0.0.2", udp.getIp());
        assertEquals(8081, udp.getPort());
        assertEquals("udp", udp.getNetwork());
        assertTrue(udp.getExtMap().isEmpty());
    }

    @Test
    public void testSetCallee() {
        ExtensionLoader
                .registerPlugin(new PluginConfig("attalog", Filter.class, RemoteLoggerTest.class));
        BackendConfig config = new BackendConfig();
        config.setName("trpc.calleeapp.calleeserver.calleeservice.calleemethod");
        config.setNamingUrl("ip://127.0.0.1:8888");
        config.setExtMap(ImmutableMap.of("attalog", (Object) "attalog"));
        config.setFilters(Lists.newArrayList("attalog"));
        config.setGroup("group");
        config.setCallee("trpc.app.server.service");
        config.init();
        assertEquals(config.getCalleeApp(), "");
        assertEquals(config.getCalleeServer(), "");
        assertEquals(config.getCalleeService(), "");
        assertEquals("127.0.0.1:8888", config.getCallee());
    }

    @Test
    public void testNameSpace() {
        ExtensionLoader
                .registerPlugin(new PluginConfig("attalog", Filter.class, RemoteLoggerTest.class));
        BackendConfig config = new BackendConfig();
        config.setName("trpc.calleeapp.calleeserver.calleeservice.calleemethod");
        config.setNamingUrl("ip://127.0.0.1:8888");
        config.setExtMap(ImmutableMap.of("attalog", (Object) "attalog"));
        config.setFilters(Lists.newArrayList("attalog"));
        config.setGroup("group");
        config.setCallee("trpc.app.server.service");
        config.setNamespace("abc");
        config.setNamingMap(new HashMap<>());
        config.setNamingOptions(new NamingOptions());
        config.init();
        config.toString();
        assertEquals(0, config.getNamingMap().size());
        assertEquals(config.getCalleeApp(), "");
        assertEquals(config.getCalleeServer(), "");
        assertEquals(config.getCalleeService(), "");
        assertEquals(config.getNamingOptions().getExtMap().get("namespace"), "abc");
    }

    @Test
    public void testIp() {
        ExtensionLoader
                .registerPlugin(new PluginConfig("attalog", Filter.class, RemoteLoggerTest.class));
        ExtensionLoader.registerPlugin(ThreadWorkerPool.newThreadWorkerPoolConfig("thread", 10, Boolean.FALSE));
        BackendConfig config = new BackendConfig();
        config.setNamingUrl("ip://127.0.0.1:8888");
        config.setVersion("v888");
        Map<String, Object> extMap = new HashMap<>();
        extMap.put("attalog", "attalog");
        config.setExtMap(extMap);
        config.setFilters(Lists.newArrayList("attalog"));
        config.setGroup("group");
        config.setMock(false);
        config.setMockClass("mock.interface");
        config.setWorkerPool("thread");
        config.setProxyType("jdk");
        config.setServiceInterface(GenericClient.class);
        config.setRequestTimeout(1234);
        config.init();
        try {
            // check
            assertEquals(config.getNamingUrl(), "ip://127.0.0.1:8888");
            assertEquals(config.getExtMap().get("attalog"), "attalog");
            assertEquals(config.getFilters().get(0), "attalog");
            assertEquals(config.getGroup(), "group");
            assertFalse(config.getMock());
            assertEquals(config.getMockClass(), "mock.interface");
            assertEquals(config.getWorkerPool(), "thread");
            assertEquals(config.getProxyType(), "jdk");
            assertEquals(config.getServiceInterface(), GenericClient.class);
            assertEquals(config.getRequestTimeout(), 1234);
            assertEquals(config.getVersion(), "v888");
            assertEquals("127.0.0.1:8888", config.getCallee());
            assertEquals("", config.getCalleeApp());
            assertEquals("", config.getCalleeServer());
            assertEquals("", config.getCalleeService());
            ServiceId serviceId = config.toNamingServiceId();
            assertEquals(serviceId.getGroup(), "group");
            assertEquals(serviceId.getServiceName(), "127.0.0.1:8888");
            assertEquals(serviceId.getVersion(), "v888");
        } finally {
            config.stop();
        }
    }

    @Test
    public void test() {
        ExtensionLoader
                .registerPlugin(new PluginConfig("attalog", Filter.class, RemoteLoggerTest.class));
        ExtensionLoader.registerPlugin(ThreadWorkerPool.newThreadWorkerPoolConfig("thread", 10,
                10, Boolean.FALSE));
        BackendConfig config = new BackendConfig();
        config.setCallee("trpc.calleeapp.calleeserver.calleeservice.calleemethod");
        config.setNamingUrl("ip://127.0.0.1:8888");
        config.setVersion("v888");
        Map<String, Object> extMap = new HashMap<>();
        extMap.put("attalog", "attalog");
        config.setExtMap(extMap);
        config.setFilters(Lists.newArrayList("attalog"));
        config.setGroup("group");
        config.setMock(false);
        config.setMockClass("mock.interface");
        config.setWorkerPool("thread");
        config.setProxyType("jdk");
        config.setServiceInterface(GenericClient.class);
        config.setRequestTimeout(1234);
        config.setCallerServiceName("callerServiceName");
        ConfigManager.getInstance().getGlobalConfig().setNamespace("namespace");
        ConfigManager.getInstance().getGlobalConfig().setEnvName("envName");
        config.init();
        try {
            // check
            assertEquals(config.getNamingUrl(), "ip://127.0.0.1:8888");
            assertEquals(config.getExtMap().get("attalog"), "attalog");
            assertEquals(config.getFilters().get(0), "attalog");
            assertEquals(config.getGroup(), "group");
            assertFalse(config.getMock());
            assertEquals(config.getMockClass(), "mock.interface");
            assertEquals(config.getWorkerPool(), "thread");
            assertEquals(config.getProxyType(), "jdk");
            assertEquals(config.getServiceInterface(), GenericClient.class);
            assertEquals(config.getRequestTimeout(), 1234);
            assertEquals(config.getVersion(), "v888");
            assertEquals(config.getCalleeApp(), "");
            assertEquals(config.getCalleeServer(), "");
            assertEquals(config.getCalleeService(), "");
            ServiceId serviceId = config.toNamingServiceId();
            assertEquals(serviceId.getGroup(), "group");
            assertEquals(serviceId.getServiceName(), "127.0.0.1:8888");
            assertEquals(serviceId.getVersion(), "v888");
            assertEquals(serviceId.getCallerServiceName(), "callerServiceName");
            assertEquals(serviceId.getCallerNamespace(), "namespace");
            assertEquals(serviceId.getCallerEnvName(), "envName");
        } finally {
            config.stop();
        }
    }

    @Test
    public void testGetProxy() {
        BackendConfig config = new BackendConfig();
        config.setServiceInterface(GenericClient.class);
        config.setName("client");
        config.setNamingUrl("ip://127.0.0.1:12345");
        ConfigManager.getInstance().getClientConfig().getBackendConfigMap()
                .put("client", config);
        ConsumerConfig<GenericClient> consumerConfig = new ConsumerConfig<>();
        consumerConfig.setBackendConfig(config);
        consumerConfig.setServiceInterface(GenericClient.class);
        Object defaultProxy = config.getDefaultProxy();
        assertNotNull(defaultProxy);
        Object proxy = config.getProxy(consumerConfig);
        assertNotNull(proxy);
        proxy = config.getProxy(GenericClient.class);
        assertNotNull(proxy);
        proxy = config.getProxyWithSourceSet(GenericClient.class, "a");
        assertNotNull(proxy);
        proxy = config.getProxyWithSourceSet(consumerConfig, "b");
        assertNotNull(proxy);
        proxy = config.getProxyWithDestinationSet(consumerConfig, "b");
        assertNotNull(proxy);
        proxy = config.getProxyWithDestinationSet(GenericClient.class, "b");
        assertNotNull(proxy);
    }

    @Test
    public void testNotDefault() {
        ExtensionLoader
                .registerPlugin(new PluginConfig("attalog", Filter.class, RemoteLoggerTest.class));
        ExtensionLoader.registerPlugin(ThreadWorkerPool.newThreadWorkerPoolConfig("thread", 10, Boolean.FALSE));
        BackendConfig config = new BackendConfig();
        config.setName("trpc.calleeapp.calleeserver.calleeservice.calleemethod");
        config.setNamingUrl("ip://127.0.0.1:8888");
        config.setVersion("v888");
        config.setExtMap(ImmutableMap.of("attalog", (Object) "attalog"));
        config.setFilters(Lists.newArrayList("attalog"));
        config.setGroup("group");
        config.setMock(false);
        config.setMockClass("mock.interface");
        config.setWorkerPool("thread");
        config.setProxyType("jdk");
        config.setServiceInterface(Object.class);
        config.setRequestTimeout(1234);
        // check
        try {
            config.setCalleeApp("calleeapp1");
            config.setCalleeServer("calleeserver1");
            config.setCalleeService("calleeservice1");
            config.init();
            assertEquals(config.getCalleeApp(), "calleeapp1");
            assertEquals(config.getCalleeServer(), "calleeserver1");
            assertEquals(config.getCalleeService(), "calleeservice1");
            ServiceId serviceId = config.toNamingServiceId();
            assertEquals(serviceId.getGroup(), "group");
            assertEquals(serviceId.getServiceName(), "127.0.0.1:8888");
            assertEquals(serviceId.getVersion(), "v888");
        } finally {
            config.stop();
        }
    }

    public static final class RemoteLoggerTest extends RemoteLoggerFilter {

        @Override
        public String getPluginName() {
            return null;
        }

    }
}
