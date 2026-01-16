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

package com.tencent.trpc.core.cluster;

import static org.junit.Assert.assertEquals;

import com.tencent.trpc.core.common.config.BackendConfig;
import com.tencent.trpc.core.common.config.ConsumerConfig;
import com.tencent.trpc.core.common.config.ProtocolConfig;
import com.tencent.trpc.core.exception.TRpcException;
import com.tencent.trpc.core.rpc.CloseFuture;
import com.tencent.trpc.core.rpc.ConsumerInvoker;
import com.tencent.trpc.core.rpc.RpcClient;
import java.lang.reflect.Field;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;

public class RpcClusterClientManagerTest {

    @Test
    public void test() throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException,
            SecurityException, InterruptedException {
        BackendConfig backendConfig = new BackendConfig();
        backendConfig.setIdleTimeout(1);
        backendConfig.setNamingUrl("ip://127.0.0.1");
        ProtocolConfigTest config = new ProtocolConfigTest();
        RpcClient rpcClient = RpcClusterClientManager.getOrCreateClient(backendConfig, config);
        Assert.assertNotNull(rpcClient);
        Field field = RpcClusterClientManager.class.getDeclaredField("CLUSTER_MAP");
        field.setAccessible(true);
        Map<BackendConfig, Map> clusterMap = (Map<BackendConfig, Map>) field.get(null);
        assertEquals(1, clusterMap.get(backendConfig).size());
        Thread.sleep(10);
        RpcClusterClientManager.scanUnusedClient();
        assertEquals(0, clusterMap.get(backendConfig).size());
        BackendConfig backend = new BackendConfig();
        backend.setNamingUrl("ip://127.0.0.1:8081");
        RpcClusterClientManager.getOrCreateClient(backend, config);
        RpcClusterClientManager.shutdownBackendConfig(backend);
    }

    @Test
    public void testDebugLog() throws Exception {
        BackendConfig backendConfig = new BackendConfig();
        backendConfig.setIdleTimeout(100000);
        backendConfig.setNamingUrl("ip://127.0.0.1:8082");
        ProtocolConfigTest config = new ProtocolConfigTest();
        RpcClient rpcClient = RpcClusterClientManager.getOrCreateClient(backendConfig, config);
        Assert.assertNotNull(rpcClient);
        RpcClusterClientManager.scanUnusedClient();
        RpcClusterClientManager.shutdownBackendConfig(backendConfig);
    }

    private static class ProtocolConfigTest extends ProtocolConfig {

        @Override
        public RpcClient createClient() {
            return new RpcClient() {

                @Override
                public void open() throws TRpcException {
                }

                @Override
                public boolean isClosed() {
                    return false;
                }

                @Override
                public boolean isAvailable() {
                    return true;
                }

                @Override
                public ProtocolConfig getProtocolConfig() {
                    return ProtocolConfigTest.this;
                }

                @Override
                public void close() {
                }

                @Override
                public <T> ConsumerInvoker<T> createInvoker(ConsumerConfig<T> consumerConfig) {
                    return null;
                }

                @Override
                public CloseFuture<Void> closeFuture() {
                    return new CloseFuture<Void>();
                }
            };
        }
    }
}
