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

package com.tencent.trpc.core.selector;

import com.tencent.trpc.core.common.ConfigManager;
import com.tencent.trpc.core.common.Lifecycle;
import com.tencent.trpc.core.common.config.PluginConfig;
import com.tencent.trpc.core.exception.TRpcException;
import com.tencent.trpc.core.extension.ExtensionLoader;
import com.tencent.trpc.core.rpc.Request;
import com.tencent.trpc.core.rpc.RpcClientContext;
import com.tencent.trpc.core.selector.circuitbreaker.CircuitBreakerManager;
import com.tencent.trpc.core.selector.discovery.DiscoveryManager;
import com.tencent.trpc.core.selector.loadbalance.AbstractLoadBalance;
import com.tencent.trpc.core.selector.loadbalance.LoadBalanceManager;
import com.tencent.trpc.core.selector.mock.MockDiscovery;
import com.tencent.trpc.core.selector.spi.CircuitBreaker;
import com.tencent.trpc.core.selector.spi.Discovery;
import com.tencent.trpc.core.selector.spi.LoadBalance;
import com.tencent.trpc.core.selector.spi.Selector;
import com.tencent.trpc.core.selector.support.def.AssembleSelector;
import com.tencent.trpc.core.selector.support.def.AssembleSelectorConfig;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

/**
 * assemble selector Polaris Service Discovery Test
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({DiscoveryManager.class, LoadBalanceManager.class, CircuitBreakerManager.class})
@PowerMockIgnore({"javax.management.*"})
public class ClusterNamingTest {

    private static final String HOST_PREFIX = "127.0.0.";
    private static final int PORT_BASE = 1230;
    private PluginConfig selectorConfig;
    private PluginConfig assembleSelectorConfig;
    private ServiceId serviceId;
    private RpcClientContext rpcContext;
    private Request request;
    private List<ServiceInstance> manyInstanceList;

    /**
     * 运行测试函数之前执行初始化
     */
    @Before
    public void init() {
        ConfigManager.stopTest();

        this.selectorConfig = ConfigTest.getSelectorConfig();
        this.serviceId = ConfigTest.getServiceId();
        this.rpcContext = ConfigTest.getRpcContext();
        this.request = ConfigTest.getRpcRequest();
        this.manyInstanceList = genServiceInstanceList(10);
        this.assembleSelectorConfig = ConfigTest.asssembleSelectorConfig;

        Discovery discovery = DiscoveryManager.getManager()
                .get(AssembleSelectorConfig
                        .parse(this.assembleSelectorConfig.getName(),
                                this.assembleSelectorConfig.getProperties())
                        .getDiscovery());
        ((MockDiscovery) discovery).setServiceInstances(manyInstanceList);

        ConfigTest.registe();
        ConfigManager.startTest();
    }

    @After
    public void after() {
        ConfigManager.stopTest();
    }


    @Test
    public void testReport() {
        AssembleSelector defaultClusterNaming = new AssembleSelector();
        defaultClusterNaming.setPluginConfig(assembleSelectorConfig);
        defaultClusterNaming.init();
        defaultClusterNaming.report(this.manyInstanceList.get(0), 0, 0);
    }

    @Test
    public void testOneFuture() {
        Selector clusterNaming =
                ExtensionLoader.getExtensionLoader(Selector.class)
                        .getExtension(selectorConfig.getName());
        rpcContext.setHashVal("hashVal");
        CompletionStage<ServiceInstance> oneFuture = clusterNaming
                .asyncSelectOne(serviceId, request);

        AtomicReference<Throwable> errorRef = new AtomicReference<>();
        CompletionStage<ServiceInstance> stage = oneFuture.whenComplete((res, e) -> {
            if (e != null) {
                errorRef.set(e);
                e.printStackTrace();
            } else {
                System.out.println("one future res:" + res);
            }
        });
        CompletableFuture.allOf(stage.toCompletableFuture()).join();

        if (clusterNaming instanceof Lifecycle) {
            ((Lifecycle) clusterNaming).stop();
        }
        Assert.assertNull(errorRef.get());
    }


    @Test
    public void testListFuture() {
        AssembleSelector defaultClusterNaming = (AssembleSelector) ExtensionLoader
                .getExtensionLoader(Selector.class).getExtension(assembleSelectorConfig.getName());
        CompletionStage<List<ServiceInstance>> listFuture =
                defaultClusterNaming.asyncSelectAll(serviceId, request);

        AtomicReference<Throwable> errorRef = new AtomicReference<>();
        CompletionStage<List<ServiceInstance>> stage = listFuture.whenComplete((res, e) -> {
            if (e != null) {
                errorRef.set(e);
                e.printStackTrace();
            } else {
                System.out.println("list future res:" + res);
            }
        });
        CompletableFuture.allOf(stage.toCompletableFuture()).join();

        Assert.assertNull(errorRef.get());
    }

    private void sleep(long milliMs) {
        try {
            Thread.sleep(milliMs);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private List<ServiceInstance> genServiceInstanceList(int size) {
        List<ServiceInstance> list = new ArrayList<>();
        for (int i = 1; i <= size; ++i) {
            list.add(genServiceInstance(i));
        }
        return list;
    }

    private ServiceInstance genServiceInstance(int i) {
        return new ServiceInstance(HOST_PREFIX + i, PORT_BASE + i, new HashMap<>());
    }

    private Discovery mockDiscovery() {
        Discovery discovery = Mockito.mock(Discovery.class);
        Mockito.when(discovery.asyncList(Mockito.any(), Mockito.any()))
                .thenReturn(CompletableFuture.supplyAsync(() -> {
                    this.sleep(10);
                    return manyInstanceList;
                }));

        return discovery;
    }

    private CircuitBreaker mockCircuitBreaker() {
        CircuitBreaker breaker = Mockito.mock(CircuitBreaker.class);

        Mockito.when(breaker.allowRequest(Mockito.any())).thenAnswer(a -> {
            int i = ThreadLocalRandom.current().nextInt(10);
            return i > 3;
        });

        return breaker;
    }

    private LoadBalance mockLoadBalance() {
        return new TestLoadBalance();
    }

    static class TestLoadBalance extends AbstractLoadBalance {

        @Override
        protected ServiceInstance doSelect(List<ServiceInstance> instances, Request req)
                throws TRpcException {
            String hashVal = req.getMeta().getHashVal();
            int idx = (hashVal == null ? 0 : Math.abs(hashVal.hashCode()) % instances.size());
            return instances.get(idx);
        }

    }
}
