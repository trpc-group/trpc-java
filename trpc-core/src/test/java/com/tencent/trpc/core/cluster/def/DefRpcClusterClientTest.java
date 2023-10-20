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

package com.tencent.trpc.core.cluster.def;

import com.tencent.trpc.core.common.config.BackendConfig;
import com.tencent.trpc.core.common.config.ConsumerConfig;
import com.tencent.trpc.core.rpc.GenericClient;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class DefRpcClusterClientTest {

    private DefRpcClusterClient defRpcClusterClient;

    private ConsumerConfig<GenericClient> consumerConfig;

    @Before
    public void setUp() {
        BackendConfig backendConfig = new BackendConfig();
        backendConfig.setServiceInterface(GenericClient.class);
        backendConfig.setProxyType("bytebuddy");
        consumerConfig = new ConsumerConfig<>();
        consumerConfig.setBackendConfig(backendConfig);
        consumerConfig.setServiceInterface(GenericClient.class);
        this.defRpcClusterClient = new DefRpcClusterClient(backendConfig);
        this.defRpcClusterClient.start();
    }

    @Test
    public void testGetProxy() {
        GenericClient proxy = defRpcClusterClient.getProxy(consumerConfig);
        Assert.assertNotNull(proxy);
    }

    @Test
    public void testGetConfig() {
        BackendConfig config = defRpcClusterClient.getConfig();
        Assert.assertEquals("bytebuddy", config.getProxyType());
    }

    @Test
    public void testStop() {
        Assert.assertTrue(defRpcClusterClient.isAvailable());
        Assert.assertFalse(defRpcClusterClient.isClosed());
        defRpcClusterClient.stop();
        Assert.assertTrue(defRpcClusterClient.isClosed());
    }
}