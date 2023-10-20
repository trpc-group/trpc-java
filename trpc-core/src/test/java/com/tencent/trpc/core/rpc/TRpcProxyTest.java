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
import com.tencent.trpc.core.common.config.BackendConfig;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class TRpcProxyTest {

    @Before
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

    @After
    public void after() {
        ConfigManager.stopTest();
    }

    @Test
    public void testGetProxy() {
        Assert.assertNotNull(TRpcProxy.getProxy("client"));
        Assert.assertNull(TRpcProxy.getProxy("client1"));
    }

    @Test
    public void testGetProxyWithClass() {
        Assert.assertNotNull(TRpcProxy.getProxy("client", GenericClient.class));
        Assert.assertNull(TRpcProxy.getProxy("client1", GenericClient.class));
    }

    @Test
    public void testGetProxyWithSourceSet() {
        Assert.assertNotNull(TRpcProxy.getProxyWithSourceSet("client", "a"));
        Assert.assertNull(TRpcProxy.getProxyWithSourceSet("client1", "a"));
    }

    @Test
    public void testGetProxyWithSourceSetWithClass() {
        Assert.assertNotNull(TRpcProxy.getProxyWithSourceSet("client", GenericClient.class, "a"));
        Assert.assertNull(TRpcProxy.getProxyWithSourceSet("client1", GenericClient.class, "a"));
    }

    @Test
    public void testGetProxyWithDestinationSet() {
        Assert.assertNotNull(TRpcProxy.getProxyWithDestinationSet("client", "a"));
        Assert.assertNull(TRpcProxy.getProxyWithDestinationSet("client1", "a"));
    }

    @Test
    public void testGetProxyWithDestSet() {
        Assert.assertNotNull(TRpcProxy.getProxyWithDestinationSet("client", GenericClient.class, "a"));
        Assert.assertNull(TRpcProxy.getProxyWithDestinationSet("client1", GenericClient.class, "a"));
    }

}