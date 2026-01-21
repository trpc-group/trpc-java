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

package com.tencent.trpc.core.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tencent.trpc.core.configcenter.spi.ConfigurationLoader;
import com.tencent.trpc.core.filter.spi.Filter;
import com.tencent.trpc.core.logger.RemoteLoggerAdapter;
import com.tencent.trpc.core.metrics.spi.MetricsFactory;
import com.tencent.trpc.core.registry.spi.Registry;
import com.tencent.trpc.core.selector.spi.CircuitBreaker;
import com.tencent.trpc.core.selector.spi.Discovery;
import com.tencent.trpc.core.selector.spi.LoadBalance;
import com.tencent.trpc.core.selector.spi.Router;
import com.tencent.trpc.core.selector.spi.Selector;
import com.tencent.trpc.core.trace.spi.TracerFactory;
import com.tencent.trpc.core.worker.spi.WorkerPool;
import org.junit.jupiter.api.Test;

public class TRpcPluginTypeAliasTest {

    @Test
    public void test() {
        TRpcPluginTypeAlias.register("object", Object.class);
        assertTrue(TRpcPluginTypeAlias.getPluginInterface("object") == Object.class);
    }

    @Test
    public void testPluginName() {
        assertEquals("worker_pool", TRpcPluginTypeAlias.getPluginName(WorkerPool.class));
        assertEquals("config", TRpcPluginTypeAlias.getPluginName(ConfigurationLoader.class));
        assertEquals("filter", TRpcPluginTypeAlias.getPluginName(Filter.class));
        assertEquals("tracing", TRpcPluginTypeAlias.getPluginName(TracerFactory.class));
        assertEquals("selector", TRpcPluginTypeAlias.getPluginName(Selector.class));
        assertEquals("discovery", TRpcPluginTypeAlias.getPluginName(Discovery.class));
        assertEquals("loadbalance", TRpcPluginTypeAlias.getPluginName(LoadBalance.class));
        assertEquals("circuitbreaker", TRpcPluginTypeAlias.getPluginName(CircuitBreaker.class));
        assertEquals("router", TRpcPluginTypeAlias.getPluginName(Router.class));
        assertEquals("registry", TRpcPluginTypeAlias.getPluginName(Registry.class));
        assertEquals("remote_log", TRpcPluginTypeAlias.getPluginName(RemoteLoggerAdapter.class));
        assertEquals("metrics", TRpcPluginTypeAlias.getPluginName(MetricsFactory.class));
    }
}
