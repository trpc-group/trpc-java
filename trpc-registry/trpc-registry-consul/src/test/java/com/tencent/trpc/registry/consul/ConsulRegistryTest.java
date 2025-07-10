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

package com.tencent.trpc.registry.consul;

import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.Response;
import com.ecwid.consul.v1.catalog.CatalogServicesRequest;
import com.ecwid.consul.v1.health.HealthServicesRequest;
import com.ecwid.consul.v1.health.model.HealthService;
import com.tencent.trpc.core.common.config.PluginConfig;
import com.tencent.trpc.core.common.config.ProtocolConfig;
import com.tencent.trpc.core.exception.TRpcExtensionException;
import com.tencent.trpc.core.registry.RegisterInfo;
import com.tencent.trpc.core.worker.support.thread.ThreadWorkerPool;
import com.tencent.trpc.registry.center.NotifyListener;
import com.tencent.trpc.support.ConsulInstanceManager;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ConsulRegistryTest {

    NotifyListener notifyListener = new NotifyListener() {
        @Override
        public void destroy() throws TRpcExtensionException {

        }

        @Override
        public void notify(List<RegisterInfo> registerInfos) {

        }
    };

    @Test
    public void testDoRegistry() {
        Map<String, Object> extMap = new HashMap<>();
        extMap.put("addresses", "127.0.0.1:8088");

        ProtocolConfig protocolConfig = new ProtocolConfig();
        protocolConfig.setExtMap(extMap);
        ConsulRegistryCenter consulRegistry = new ConsulRegistryCenter();

        RegisterInfo registerInfo = new RegisterInfo("tcp", "127.0.0.1", 8080, "test", extMap);

        ConsulInstanceManager consulInstanceManager = new ConsulInstanceManager(protocolConfig);
        consulInstanceManager.resetClient(Mockito.mock(ConsulClient.class));
        consulRegistry.setConsulInstanceManager(consulInstanceManager);
        consulRegistry.doRegister(registerInfo);

        consulInstanceManager.setToken("token");
        consulRegistry.setConsulInstanceManager(consulInstanceManager);
        consulRegistry.doRegister(registerInfo);
    }


    @Test
    public void testDoRegistry01() {
        Map<String, Object> extMap = new HashMap<>();
        extMap.put("addresses", "127.0.0.1:8088");
        extMap.put("ttl_enabled", false);
        extMap.put("tag", "dev");
        extMap.put("health_check_url", "http://127.0.0.1:8080/heath");

        ConsulRegistryCenter consulRegistry = new ConsulRegistryCenter();
        PluginConfig pluginConfig = new PluginConfig("id", ThreadWorkerPool.class, extMap);
        consulRegistry.setPluginConfig(pluginConfig);

        ProtocolConfig protocolConfig = new ProtocolConfig();
        protocolConfig.setExtMap(extMap);

        ConsulInstanceManager consulInstanceManager = new ConsulInstanceManager(protocolConfig);
        consulInstanceManager.resetClient(Mockito.mock(ConsulClient.class));
        consulRegistry.setConsulInstanceManager(consulInstanceManager);
        RegisterInfo registerInfo = new RegisterInfo("tcp", "127.0.0.1", 8080, "test", extMap);
        consulRegistry.doRegister(registerInfo);

        consulInstanceManager.setToken("token");
        consulRegistry.setConsulInstanceManager(consulInstanceManager);
        consulRegistry.doRegister(registerInfo);
    }

    @Test
    public void testDoRegistry02() {
        Map<String, Object> extMap = new HashMap<>();
        extMap.put("addresses", "127.0.0.1:8088");
        extMap.put("ttl_enabled", false);
        extMap.put("tag", "dev");

        ConsulRegistryCenter consulRegistry = new ConsulRegistryCenter();
        PluginConfig pluginConfig = new PluginConfig("id", ThreadWorkerPool.class, extMap);
        consulRegistry.setPluginConfig(pluginConfig);

        ProtocolConfig protocolConfig = new ProtocolConfig();
        protocolConfig.setExtMap(extMap);

        ConsulInstanceManager consulInstanceManager = new ConsulInstanceManager(protocolConfig);
        consulInstanceManager.resetClient(Mockito.mock(ConsulClient.class));
        consulRegistry.setConsulInstanceManager(consulInstanceManager);

        RegisterInfo registerInfo = new RegisterInfo("tcp", "127.0.0.1", 8080, "test", extMap);
        consulRegistry.doRegister(registerInfo);

        consulInstanceManager.setToken("token");
        consulRegistry.setConsulInstanceManager(consulInstanceManager);
        consulRegistry.doRegister(registerInfo);
    }

    @Test
    public void testDoUnregister() {
        Map<String, Object> extMap = new HashMap<>();
        extMap.put("addresses", "127.0.0.1:8088");

        ProtocolConfig protocolConfig = new ProtocolConfig();
        protocolConfig.setExtMap(extMap);
        ConsulRegistryCenter consulRegistry = new ConsulRegistryCenter();

        ConsulInstanceManager consulInstanceManager = new ConsulInstanceManager(protocolConfig);
        consulInstanceManager.resetClient(Mockito.mock(ConsulClient.class));
        consulRegistry.setConsulInstanceManager(consulInstanceManager);

        RegisterInfo registerInfo = new RegisterInfo("tcp", "127.0.0.1", 8080, "test", extMap);
        consulRegistry.doUnregister(registerInfo);

        consulInstanceManager.setToken("token");
        consulRegistry.setConsulInstanceManager(consulInstanceManager);
        consulRegistry.doUnregister(registerInfo);
    }




    @Test
    public void testDoSubscribe() {
        Map<String, Object> extMap = new HashMap<>();
        extMap.put("addresses", "127.0.0.1:8088");

        ProtocolConfig protocolConfig = new ProtocolConfig();
        protocolConfig.setExtMap(extMap);
        ConsulRegistryCenter consulRegistry = new ConsulRegistryCenter();
        PluginConfig pluginConfig = new PluginConfig("id", ThreadWorkerPool.class, extMap);
        consulRegistry.setPluginConfig(pluginConfig);

        ConsulClient consulClient = Mockito.mock(ConsulClient.class);
        List<HealthService> healthServices = new ArrayList<>();
        Response<List<HealthService>> response = new Response<>(healthServices, 10L, true, 2L);
        PowerMockito.when(consulClient.getHealthServices(Mockito.anyString(), Mockito.any(HealthServicesRequest.class)))
                .thenReturn(response);

        ConsulInstanceManager consulInstanceManager = new ConsulInstanceManager(protocolConfig);
        consulInstanceManager.resetClient(consulClient);
        consulRegistry.setConsulInstanceManager(consulInstanceManager);

        RegisterInfo registerInfo = new RegisterInfo("tcp", "127.0.0.1", 8080, "test", extMap);
        consulRegistry.doSubscribe(registerInfo, notifyListener);
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        ConsulClient consulClient01 = Mockito.mock(ConsulClient.class);
        Map<String, List<String>> healthServices01 = new HashMap<>();
        Response<Map<String, List<String>>> response01 = new Response<>(healthServices01, 10L, true, 2L);
        PowerMockito.when(consulClient01.getCatalogServices(Mockito.any(CatalogServicesRequest.class)))
                .thenReturn(response01);
        ConsulInstanceManager consulInstanceManager01 = new ConsulInstanceManager(protocolConfig);
        consulInstanceManager01.resetClient(consulClient01);
        consulRegistry.setConsulInstanceManager(consulInstanceManager01);

        RegisterInfo registerInfo01 = new RegisterInfo("tcp", "127.0.0.1", 8080, "*", extMap);
        consulRegistry.doSubscribe(registerInfo01, notifyListener);
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    @Test
    public void testDoUnsubscribe() {
        Map<String, Object> extMap = new HashMap<>();
        extMap.put("addresses", "127.0.0.1:8088");

        ProtocolConfig protocolConfig = new ProtocolConfig();
        protocolConfig.setExtMap(extMap);

        ConsulRegistryCenter consulRegistry = new ConsulRegistryCenter();
        PluginConfig pluginConfig = new PluginConfig("id", ThreadWorkerPool.class, extMap);
        consulRegistry.setPluginConfig(pluginConfig);

        ConsulClient consulClient = Mockito.mock(ConsulClient.class);
        List<HealthService> healthServices = new ArrayList<>();
        Response<List<HealthService>> response = new Response<>(healthServices, 1L, true, 2L);
        PowerMockito.when(consulClient.getHealthServices(Mockito.anyString(), Mockito.any(HealthServicesRequest.class)))
                .thenReturn(response);

        ConsulInstanceManager consulInstanceManager = new ConsulInstanceManager(protocolConfig);
        consulInstanceManager.resetClient(consulClient);
        consulRegistry.setConsulInstanceManager(consulInstanceManager);

        RegisterInfo registerInfo = new RegisterInfo("tcp", "127.0.0.1", 8080, "test", extMap);
        consulRegistry.doSubscribe(registerInfo, notifyListener);
        consulRegistry.doUnsubscribe(registerInfo, notifyListener);
    }

    @Test
    public void testInit() {
        Map<String, Object> extMap = new HashMap<>();
        extMap.put("addresses", "127.0.0.1:8088");

        ProtocolConfig protocolConfig = new ProtocolConfig();
        protocolConfig.setExtMap(extMap);

        ConsulRegistryCenter consulRegistry = new ConsulRegistryCenter();
        PluginConfig pluginConfig = new PluginConfig("id", ThreadWorkerPool.class, extMap);
        consulRegistry.setPluginConfig(pluginConfig);
        ConsulInstanceManager consulInstanceManager = new ConsulInstanceManager(protocolConfig);
        ConsulClient consulClient = Mockito.spy(new ConsulClient());
        consulInstanceManager.resetClient(consulClient);
        consulRegistry.setConsulInstanceManager(consulInstanceManager);
        consulRegistry.init();
    }

    @Test
    public void testIsAvailable() {
        Map<String, Object> extMap = new HashMap<>();
        extMap.put("addresses", "127.0.0.1:8088");

        ConsulRegistryCenter consulRegistry = new ConsulRegistryCenter();
        PluginConfig pluginConfig = new PluginConfig("id", ThreadWorkerPool.class, extMap);
        consulRegistry.setPluginConfig(pluginConfig);

        ProtocolConfig protocolConfig = new ProtocolConfig();
        protocolConfig.setExtMap(extMap);

        ConsulInstanceManager consulInstanceManager = new ConsulInstanceManager(protocolConfig);
        consulInstanceManager.resetClient(Mockito.mock(ConsulClient.class));
        consulRegistry.setConsulInstanceManager(consulInstanceManager);
        consulRegistry.isAvailable();
    }

    @Test
    public void testZDestroy() {
        Map<String, Object> extMap = new HashMap<>();
        extMap.put("addresses", "127.0.0.1:8088");

        ConsulRegistryCenter consulRegistry = new ConsulRegistryCenter();
        PluginConfig pluginConfig = new PluginConfig("id", ThreadWorkerPool.class, extMap);
        consulRegistry.setPluginConfig(pluginConfig);

        ConsulClient consulClient = Mockito.mock(ConsulClient.class);

        List<HealthService> healthServices = new ArrayList<>();
        Response<List<HealthService>> response = new Response<>(healthServices, 1L, true, 2L);
        PowerMockito.when(consulClient.getHealthServices(Mockito.anyString(), Mockito.any(HealthServicesRequest.class)))
                .thenReturn(response);

        ProtocolConfig protocolConfig = new ProtocolConfig();
        protocolConfig.setExtMap(extMap);
        ConsulInstanceManager consulInstanceManager = new ConsulInstanceManager(protocolConfig);
        consulInstanceManager.resetClient(consulClient);
        consulRegistry.setConsulInstanceManager(consulInstanceManager);

        RegisterInfo registerInfo = new RegisterInfo("tcp", "127.0.0.1", 8080, "test", extMap);
        consulRegistry.doSubscribe(registerInfo, notifyListener);
        consulRegistry.doRegister(registerInfo);

        consulRegistry.destroy();
    }
}