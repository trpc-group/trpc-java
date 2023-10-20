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

package com.tencent.trpc.registry.discovery;


import static com.tencent.trpc.registry.common.ConfigConstants.REGISTRY_CENTER_REGISTER_CONSUMER_KEY;

import com.tencent.trpc.core.common.config.PluginConfig;
import com.tencent.trpc.core.exception.TRpcExtensionException;
import com.tencent.trpc.core.logger.Logger;
import com.tencent.trpc.core.logger.LoggerFactory;
import com.tencent.trpc.core.registry.RegisterInfo;
import com.tencent.trpc.core.selector.ServiceId;
import com.tencent.trpc.registry.center.AbstractRegistryCenter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class RegistryDiscoveryTest {

    private static final Logger logger = LoggerFactory.getLogger(RegistryDiscoveryTest.class);

    private static RegistryDiscovery discovery;
    private static AbstractRegistryCenter clientRegistry;

    @Before
    public void setUp() throws Exception {
        clientRegistry = new AbstractRegistryCenter() {
            @Override
            public void init() throws TRpcExtensionException {
                logger.debug("client registry test init");
            }
        };
        Map<String, Object> properties = new HashMap<>();
        properties.put(REGISTRY_CENTER_REGISTER_CONSUMER_KEY, true);
        clientRegistry.setPluginConfig(new PluginConfig("zookeeper", AbstractRegistryCenter.class, properties));
        ServiceId serviceId = new ServiceId();
        serviceId.setServiceName("test.service1");
        discovery = new RegistryDiscovery(serviceId, clientRegistry);
    }

    private RegisterInfo buildRegisterInfo(int port) {
        RegisterInfo registerInfo = new RegisterInfo("trpc", "0.0.0.0", port,
                "test.service1");
        return registerInfo;
    }

    @Test
    public void testSelfRegistry() {
        Assert.assertEquals(1, clientRegistry.getRegisteredRegisterInfos().size());
    }

    @Test
    public void testSubscribe() {
        Assert.assertEquals(1, clientRegistry.getSubscribedRegisterInfos().size());
    }

    @Test
    public void testNotify() {
        List<RegisterInfo> registerInfos = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            registerInfos.add(buildRegisterInfo(12000 + i));
        }
        discovery.notify(registerInfos);
        Assert.assertEquals(10, discovery.getServiceInstances().size());
    }

    @Test
    public void testList() {
        this.testNotify();
        ServiceId serviceId = new ServiceId();
        serviceId.setServiceName("test.service1");
        Assert.assertEquals(10, discovery.list(serviceId).size());
        serviceId.setServiceName("test.service2");
        Assert.assertEquals(0, discovery.list(serviceId).size());

    }

    @Test
    public void testAsyncList() throws ExecutionException, InterruptedException {
        this.testNotify();
        ServiceId serviceId = new ServiceId();
        serviceId.setServiceName("test.service1");

        Executor executor = Executors.newSingleThreadExecutor();

        Assert.assertEquals(10, discovery.asyncList(serviceId, executor).toCompletableFuture()
                .get().size());
        serviceId.setServiceName("test.service2");
        Assert.assertEquals(0,
                discovery.asyncList(serviceId, executor).toCompletableFuture().get().size());

    }

    @Test
    public void destroy() {
        discovery.destroy();
    }
}
