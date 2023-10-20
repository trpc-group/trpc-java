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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.tencent.trpc.core.common.ConfigManager;
import com.tencent.trpc.core.extension.ExtensionLoader;
import com.tencent.trpc.core.filter.FilterManager;
import com.tencent.trpc.core.filter.spi.Filter;
import com.tencent.trpc.core.registry.MockRegistry;
import com.tencent.trpc.core.registry.spi.Registry;
import com.tencent.trpc.core.rpc.ProviderInvoker;
import com.tencent.trpc.core.rpc.RpcServer;
import com.tencent.trpc.core.rpc.RpcServerManager;
import com.tencent.trpc.core.rpc.anno.TRpcService;
import com.tencent.trpc.core.worker.WorkerPoolManager;
import com.tencent.trpc.core.worker.spi.WorkerPool;
import com.tencent.trpc.core.worker.support.thread.ThreadWorkerPool;
import java.util.HashMap;
import java.util.Map;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ExtensionLoader.class, RpcServerManager.class, FilterManager.class})
@PowerMockIgnore({"javax.management.*"})
public class ProviderConfigTest {

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
    public void testInitServiceInterfaceConfig() {
        ProviderConfig<TRpcServiceInterface> config = ProviderConfig.newInstance();
        config.setRef(new TRpcServiceInstance());
        config.setServiceConfig(new ServiceConfig());
        config.init();
        assertSame(config.getServiceInterface(), TRpcServiceInterface.class);
        ProviderConfig config2 = ProviderConfig.newInstance();
        config2.setServiceInterface(Integer.class);
        config2.setRef(new TRpcServiceInstance());
        try {
            config2.init();
            assertTrue(false);
        } catch (Exception ex) {
            assertTrue(ex instanceof IllegalArgumentException);
        }
        config2.isInited();
    }

    @Test
    public void testInit() {
        PowerMockito.mockStatic(ExtensionLoader.class);
        PowerMockito.mockStatic(RpcServerManager.class);
        ExtensionLoader mock = PowerMockito.mock(ExtensionLoader.class);
        PowerMockito.when(ExtensionLoader.getExtensionLoader(Filter.class)).thenReturn(mock);
        PowerMockito.when(mock.hasExtension(Mockito.anyString())).thenReturn(true);
        PowerMockito.when(ExtensionLoader.getPluginConfigMap()).then(v -> v.callRealMethod());
        ThreadWorkerPool threadWorkerPool = new ThreadWorkerPool();
        threadWorkerPool.setPluginConfig(WorkerPoolManager.DEF_CONSUMER_WORKER_POOL_CONFIG);
        threadWorkerPool.init();
        PowerMockito.when(ExtensionLoader.getExtensionLoader(WorkerPool.class)).thenReturn(mock);
        PowerMockito.when(mock.getExtension(Mockito.anyString())).thenReturn(threadWorkerPool);
        Filter mockFilter = PowerMockito.mock(Filter.class);
        PowerMockito.when(mock.getExtension(Mockito.eq("filterId"))).thenReturn(mockFilter);
        ExtensionLoader.getExtensionLoader(Filter.class);
        ServiceConfig serviceConfig = new ServiceConfig();
        serviceConfig.setName("protoid");
        serviceConfig.setIp("127.0.0.1");
        serviceConfig.setFilters(Lists.newArrayList("filterId"));
        ProviderConfig<Object> config = ProviderConfig.newInstance();
        serviceConfig.addProviderConfig(config);
        config.setServiceInterface(Object.class);
        config.setServiceConfig(serviceConfig);
        config.setRef(new Object());
        Map<String, PluginConfig> configMap = new HashMap<>();
        Map<String, Object> extMap = new HashMap<>();
        extMap.put("address_list", "10.0.0.1");
        configMap.put("registyId", new PluginConfig("registyId", MockRegistry.class, extMap));
        ConfigManager.getInstance().getPluginConfigMap().put(Registry.class, configMap);
        config.init();
        RpcServer rpcServerMock = PowerMockito.mock(RpcServer.class);
        PowerMockito.when(RpcServerManager.getOrCreateRpcServer(Mockito.argThat(new IsValid())))
                .thenReturn(rpcServerMock);
        PowerMockito.when(rpcServerMock.getProtocolConfig()).thenReturn(new ProtocolConfig());
        serviceConfig.export();
        Mockito.verify(rpcServerMock, Mockito.times(1)).export(Mockito.any(ProviderInvoker.class));
        Mockito.verify(rpcServerMock, Mockito.times(1)).open();
        assertTrue(serviceConfig.isExported());
        serviceConfig.unExport();
        assertFalse(serviceConfig.isExported());
        ExtensionLoader mockRegistryLoader = PowerMockito.mock(ExtensionLoader.class);
        PowerMockito.when(ExtensionLoader.getExtensionLoader(Registry.class))
                .thenReturn(mockRegistryLoader);
        Registry registry = PowerMockito.mock(Registry.class);
        PowerMockito.when(mockRegistryLoader.getExtension("registyId")).thenReturn(registry);
    }

    @Test
    public void test() {
        ServiceConfig serviceConfig = new ServiceConfig();
        ProviderConfig<Object> config = ProviderConfig.newInstance();
        config.setDisableDefaultFilter(false);
        config.setServiceConfig(serviceConfig);
        serviceConfig.setFilters(Lists.newArrayList("a"));
        serviceConfig.setBacklog(1111);
        Object ref = new Object();
        config.setRef(ref);
        config.setServiceInterface(Object.class);
        serviceConfig.setFlushConsolidation(true);
        serviceConfig.setBatchDecoder(true);
        serviceConfig.setExplicitFlushAfterFlushes(1024);
        assertFalse(serviceConfig.isExported());
        assertEquals(serviceConfig.getFilters().get(0), "a");
        assertEquals(serviceConfig.getBacklog(), 1111);
        assertEquals(config.getRef(), ref);
        assertEquals(config.getServiceInterface(), Object.class);
        assertEquals(serviceConfig.getFlushConsolidation(), true);
        assertEquals(serviceConfig.getBatchDecoder(), true);
        assertEquals(serviceConfig.getExplicitFlushAfterFlushes(), 1024);
        assertNull(config.getWorkerPoolObj());
        assertFalse(config.isSetDefault());
    }

    @Test
    public void testDefaultConfig() {
        ServiceConfig config = new ServiceConfig();
        config.setName("name");
        config.setIp("127.1.1.1");
        config.setPort(80);
        config.setFilters(Lists.newArrayList("serviceConfigFilter"));
        ProviderConfig providerConfig = new ProviderConfig();
        providerConfig.setServiceConfig(config);
        config.getProviderConfigs().add(providerConfig);
        config.setDefault();
        providerConfig.overrideConfigDefault(config);
        assertEquals("serviceConfigFilter", providerConfig.getFilters().get(0));
        assertFalse(providerConfig.getEnableLinkTimeout());
        assertEquals(Integer.MAX_VALUE, providerConfig.getRequestTimeout());
        assertEquals("trpc_provider_biz_def", providerConfig.getWorkerPool());
    }

    @Test
    public void testConfig() {
        ServiceConfig config = new ServiceConfig();
        config.setName("name");
        config.setIp("127.1.1.1");
        config.setPort(8080);
        config.setNic("nic");
        config.setProtocol("trpc");
        config.setSerialization("pb");
        config.setCompressor("gzip");
        config.setKeepAlive(false);
        config.setCharset("charset");
        config.setTransporter("transporter");
        config.setMaxConns(10);
        config.setBacklog(20);
        config.setFlushConsolidation(true);
        config.setBatchDecoder(true);
        config.setExplicitFlushAfterFlushes(2048);
        config.setNetwork("network");
        config.setReceiveBuffer(30);
        config.setSendBuffer(40);
        config.setPayload(50);
        config.setIdleTimeout(60);
        config.setLazyinit(true);
        config.setIoMode("mode");
        config.setFilters(Lists.newArrayList("serviceConfigFilter"));
        config.setIoThreadGroupShare(false);
        config.setIoThreads(90);
        config.setExtMap(ImmutableMap.of("a", "a1"));
        ProviderConfig providerConfig = new ProviderConfig();
        providerConfig.setRequestTimeout(2000);
        providerConfig.setEnableLinkTimeout(Boolean.TRUE);
        providerConfig.setWorkerPool("trpc_provider_biz_def");
        providerConfig.setFilters(Lists.newArrayList("providerConfigFilter"));
        providerConfig.setServiceConfig(config);
        config.setFilters(Lists.newArrayList("providerConfigFilter"));
        config.getProviderConfigs().add(providerConfig);
        config.setDefault();

        assertEquals("providerConfigFilter", providerConfig.getFilters().get(0));
        assertTrue(providerConfig.getEnableLinkTimeout());
        assertEquals(2000, providerConfig.getRequestTimeout());
        assertEquals("trpc_provider_biz_def", providerConfig.getWorkerPool());
    }

    @TRpcService(name = "service")
    public interface TRpcServiceInterface {

    }

    public static class TRpcServiceInstance implements TRpcServiceInterface {

    }

    private class IsValid extends ArgumentMatcher<ProtocolConfig> {

        @Override
        public boolean matches(Object o) {
            ProtocolConfig ox = (ProtocolConfig) o;
            return ox.getIp().contentEquals("127.0.0.1");
        }
    }
}
