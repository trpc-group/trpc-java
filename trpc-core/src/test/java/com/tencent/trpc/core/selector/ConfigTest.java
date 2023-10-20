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
import com.tencent.trpc.core.common.config.PluginConfig;
import com.tencent.trpc.core.rpc.AbstractRequest;
import com.tencent.trpc.core.rpc.Request;
import com.tencent.trpc.core.rpc.RpcClientContext;
import com.tencent.trpc.core.rpc.RpcInvocation;
import com.tencent.trpc.core.selector.mock.MockCircuitBreaker;
import com.tencent.trpc.core.selector.mock.MockDiscovery;
import com.tencent.trpc.core.selector.mock.MockLoadBalance;
import com.tencent.trpc.core.selector.support.def.AssembleSelector;
import com.tencent.trpc.core.selector.support.def.AssembleSelectorConfig;
import com.tencent.trpc.core.worker.WorkerPoolManager;
import java.util.HashMap;
import java.util.Map;
import org.mockito.Mockito;

public class ConfigTest {

    public static PluginConfig circuitBreakerConfig = getCircuitBreakerConfig();

    public static PluginConfig workerPoolConfig = getPoolConfig();

    public static PluginConfig loadBalanceConfig = getLoadBalanceConfig();

    public static PluginConfig discoveryConfig = getDiscoveryConfig();

    public static PluginConfig asssembleSelectorConfig = getSelectorConfig();

    public static void registe() {
        ConfigManager.getInstance().registerPlugin(asssembleSelectorConfig);
        ConfigManager.getInstance().registerPlugin(circuitBreakerConfig);
        ConfigManager.getInstance().registerPlugin(workerPoolConfig);
        ConfigManager.getInstance().registerPlugin(loadBalanceConfig);
        ConfigManager.getInstance().registerPlugin(discoveryConfig);
    }

    public static PluginConfig getAssembleSelectorConfig() {
        PluginConfig pluginConfig = new PluginConfig(AssembleSelector.NAME, AssembleSelector.class,
                getSelectorConfig().getProperties());
        return pluginConfig;
    }

    public static PluginConfig getSelectorConfig() {
        Map<String, Object> extMap = new HashMap<>();
        extMap.put(AssembleSelectorConfig.DISCOVERY, "mock");
        extMap.put(AssembleSelectorConfig.CIRCUIT_BREAKER, circuitBreakerConfig.getName());
        extMap.put(AssembleSelectorConfig.WORK_POOL, workerPoolConfig.getName());
        extMap.put(AssembleSelectorConfig.LOAD_BALANCE, loadBalanceConfig.getName());
        PluginConfig selectorConfig =
                new PluginConfig(AssembleSelector.NAME, AssembleSelector.class, extMap);
        return selectorConfig;
    }

    public static PluginConfig getDiscoveryConfig() {
        PluginConfig pluginConfig = new PluginConfig("mock", MockDiscovery.class, new HashMap<>());
        return pluginConfig;
    }

    public static PluginConfig getLoadBalanceConfig() {
        PluginConfig pluginConfig = new PluginConfig("mock", MockLoadBalance.class,
                new HashMap<>());
        return pluginConfig;
    }

    public static PluginConfig getCircuitBreakerConfig() {
        PluginConfig circuitBreakerConfig =
                new PluginConfig("mock", MockCircuitBreaker.class, new HashMap<>());
        return circuitBreakerConfig;
    }

    public static PluginConfig getPoolConfig() {
        return WorkerPoolManager.DEF_NAMING_WORKER_POOL_CONFIG;
    }

    public static ServiceId getServiceId() {
        ServiceId serviceId = new ServiceId();
        serviceId.setServiceName("test");
        serviceId.setGroup("test");
        serviceId.setVersion("1.0.0");
        serviceId.setParameters(new HashMap<>());
        return serviceId;
    }

    public static RpcClientContext getRpcContext() {
        return new RpcClientContext();
    }

    public static RpcInvocation getRpcInvocation() {
        RpcInvocation mock = Mockito.mock(RpcInvocation.class);

        return mock;
    }

    public static Request getRpcRequest() {
        MockRequest mockRequest = new MockRequest();
        mockRequest.setInvocation(getRpcInvocation());
        mockRequest.setContext(new RpcClientContext());
        return mockRequest;
    }

    private static class MockRequest extends AbstractRequest {

    }
}
