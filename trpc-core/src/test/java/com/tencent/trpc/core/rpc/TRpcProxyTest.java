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

package com.tencent.trpc.core.rpc;

import com.tencent.trpc.core.common.ConfigManager;
import com.tencent.trpc.core.common.Constants;
import com.tencent.trpc.core.common.config.BackendConfig;
import com.tencent.trpc.core.common.config.NamingOptions;
import com.tencent.trpc.core.exception.TRpcException;
import com.tencent.trpc.core.extension.ExtensionLoader;
import com.tencent.trpc.core.selector.ServiceId;
import com.tencent.trpc.core.selector.ServiceInstance;
import com.tencent.trpc.core.selector.spi.Selector;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class TRpcProxyTest {

    private static final String ASSEMBLE_CLIENT_NAME = "assembleClient";
    private static final String POLARIS_CLIENT_NAME = "polarisClient";
    private static final String SET_NAME = "aa.bb.cc";

    @BeforeEach
    public void setUp() throws Exception {
        ConfigManager.stopTest();
        ConfigManager.startTest();
        BackendConfig backendConfig = new BackendConfig();
        backendConfig.setServiceInterface(GenericClient.class);
        backendConfig.setName("client");
        backendConfig.setNamingUrl("ip://127.0.0.1:12345");
        ConfigManager.getInstance().getClientConfig().getBackendConfigMap()
                .put("client", backendConfig);
    }

    @AfterEach
    public void after() {
        ConfigManager.stopTest();
    }

    @Test
    public void testGetProxy() {
        Assertions.assertNotNull(TRpcProxy.getProxy("client"));
        Assertions.assertNull(TRpcProxy.getProxy("client1"));
    }

    @Test
    public void testGetProxyWithClass() {
        Assertions.assertNotNull(TRpcProxy.getProxy("client", GenericClient.class));
        Assertions.assertNull(TRpcProxy.getProxy("client1", GenericClient.class));
    }

    @Test
    public void testGetProxyWithSourceSet() {
        Assertions.assertNotNull(TRpcProxy.getProxyWithSourceSet("client", SET_NAME));
        Assertions.assertNull(TRpcProxy.getProxyWithSourceSet("client1", SET_NAME));
    }

    @Test
    public void testGetProxyWithSourceSetWithClass() {
        Assertions.assertNotNull(TRpcProxy.getProxyWithSourceSet("client", GenericClient.class, SET_NAME));
        Assertions.assertNull(TRpcProxy.getProxyWithSourceSet("client1", GenericClient.class, SET_NAME));
    }

    @Test
    public void testGetProxyWithDestinationSet() {
        Assertions.assertNotNull(TRpcProxy.getProxyWithDestinationSet("client", SET_NAME));
        Assertions.assertNull(TRpcProxy.getProxyWithDestinationSet("client1", SET_NAME));
    }

    @Test
    public void testGetProxyWithDestSet() {
        Assertions.assertNotNull(TRpcProxy.getProxyWithDestinationSet("client", GenericClient.class, SET_NAME));
        Assertions.assertNull(TRpcProxy.getProxyWithDestinationSet("client1", GenericClient.class, SET_NAME));
    }

    @Test
    public void testAssembleDestinationSet() {
        BackendConfig backendConfig = new BackendConfig();
        backendConfig.setServiceInterface(GenericClient.class);
        backendConfig.setName(ASSEMBLE_CLIENT_NAME);
        backendConfig.setNamingUrl("assemble://test");
        ConfigManager.getInstance().getClientConfig().getBackendConfigMap()
                .put(ASSEMBLE_CLIENT_NAME, backendConfig);
        Assertions.assertNotNull(
                TRpcProxy.getProxyWithDestinationSet(ASSEMBLE_CLIENT_NAME, GenericClient.class, SET_NAME));
        Assertions.assertEquals(SET_NAME, backendConfig.getExtMap().get(NamingOptions.DESTINATION_SET));
    }

    @Test
    public void testPolarisDestinationSet() {
        BackendConfig backendConfig = new BackendConfig();
        backendConfig.setServiceInterface(GenericClient.class);
        backendConfig.setName(POLARIS_CLIENT_NAME);
        backendConfig.setNamingUrl("polaris://test");
        ConfigManager.getInstance().getClientConfig().getBackendConfigMap()
                .put(POLARIS_CLIENT_NAME, backendConfig);
        Selector simpleSelector = new Selector() {

            @Override
            public CompletionStage<ServiceInstance> asyncSelectOne(ServiceId serviceId, Request request) {
                return null;
            }

            @Override
            public CompletionStage<List<ServiceInstance>> asyncSelectAll(ServiceId serviceId, Request request) {
                return null;
            }

            @Override
            public void report(ServiceInstance serviceInstance, int code, long costMs) throws TRpcException {

            }
        };
        ExtensionLoader.getExtensionLoader(Selector.class).addExtension("polaris", simpleSelector.getClass());
        Assertions.assertNotNull(
                TRpcProxy.getProxyWithDestinationSet(POLARIS_CLIENT_NAME, GenericClient.class, SET_NAME));
        Object metadataObj = Optional.ofNullable(backendConfig.getNamingMap())
                .map(namingMap -> namingMap.get(Constants.METADATA))
                .orElse(null);
        Assertions.assertTrue(metadataObj instanceof Map);
        Map<String, String> metadata = (Map<String, String>) metadataObj;
        Assertions.assertEquals(SET_NAME, metadata.get(Constants.POLARIS_PLUGIN_SET_NAME_KEY));
        Assertions.assertEquals(Constants.POLARIS_PLUGIN_ENABLE_SET,
                metadata.get(Constants.POLARIS_PLUGIN_ENABLE_SET_KEY));
    }
}
