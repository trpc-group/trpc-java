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
import java.lang.reflect.Method;
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

    @Test
    public void testGetOrCreateClientTwice() throws Exception {
        BackendConfig backendConfig = new BackendConfig();
        backendConfig.setIdleTimeout(100000);
        backendConfig.setNamingUrl("ip://127.0.0.1:8083");
        ProtocolConfigTest config = new ProtocolConfigTest();
        RpcClient rpcClient1 = RpcClusterClientManager.getOrCreateClient(backendConfig, config);
        RpcClient rpcClient2 = RpcClusterClientManager.getOrCreateClient(backendConfig, config);
        Assert.assertNotNull(rpcClient1);
        Assert.assertNotNull(rpcClient2);
        RpcClusterClientManager.shutdownBackendConfig(backendConfig);
    }

    @Test
    public void testClose() throws Exception {
        BackendConfig backendConfig = new BackendConfig();
        backendConfig.setIdleTimeout(100000);
        backendConfig.setNamingUrl("ip://127.0.0.1:8084");
        ProtocolConfigTest config = new ProtocolConfigTest();
        RpcClient rpcClient = RpcClusterClientManager.getOrCreateClient(backendConfig, config);
        Assert.assertNotNull(rpcClient);
        RpcClusterClientManager.close();
        RpcClusterClientManager.reset();
    }

    @Test
    public void testShutdownNonExistBackend() {
        BackendConfig backendConfig = new BackendConfig();
        backendConfig.setNamingUrl("ip://127.0.0.1:9999");
        RpcClusterClientManager.shutdownBackendConfig(backendConfig);
    }

    @Test
    public void testScanWithEmptyCluster() {
        RpcClusterClientManager.scanUnusedClient();
    }

    /**
     * An idle client which still has in-flight requests must be kept, otherwise those requests would be
     * forcibly failed with "Client(...) stop" by {@code DefResponseFutureManager#closeClient}.
     * Once the requests are done, the client is expected to be cleaned in the next round.
     */
    @Test
    public void testScanSkipsClientWithInFlightRequest() throws Exception {
        BackendConfig backendConfig = new BackendConfig();
        backendConfig.setIdleTimeout(1);
        backendConfig.setNamingUrl("ip://127.0.0.1:8085");
        InFlightProtocolConfigTest config = new InFlightProtocolConfigTest("127.0.0.1", 8085);
        RpcClusterClientManager.getOrCreateClient(backendConfig, config);
        Map<String, Object> innerMap = innerClientsOf(backendConfig);
        assertEquals(1, innerMap.size());

        Thread.sleep(10);
        config.setPendingRequestCount(1);
        RpcClusterClientManager.scanUnusedClient();
        assertEquals("idle client with in-flight request should not be cleaned", 1, innerMap.size());

        config.setPendingRequestCount(0);
        RpcClusterClientManager.scanUnusedClient();
        assertEquals("idle client without in-flight request should be cleaned", 0, innerMap.size());
        RpcClusterClientManager.shutdownBackendConfig(backendConfig);
    }

    /**
     * A client may be used by a business thread after it has been picked as unused but before it is really
     * closed. In that case it must be put back instead of being closed.
     */
    @Test
    public void testScanRescuesClientUsedAgainBeforeClosing() throws Exception {
        BackendConfig backendConfig = new BackendConfig();
        backendConfig.setIdleTimeout(1);
        backendConfig.setNamingUrl("ip://127.0.0.1:8086");

        ProtocolConfigTest configA = new ProtocolConfigTest("127.0.0.1", 8087);
        ProtocolConfigTest configB = new ProtocolConfigTest("127.0.0.1", 8088);
        RpcClusterClientManager.getOrCreateClient(backendConfig, configA);
        RpcClusterClientManager.getOrCreateClient(backendConfig, configB);

        Map<String, Object> innerMap = innerClientsOf(backendConfig);
        assertEquals(2, innerMap.size());
        Object clientA = innerMap.get(configA.toUniqId());
        Object clientB = innerMap.get(configB.toUniqId());

        // Whichever client is closed first refreshes the other one, which simulates a business thread
        // using it during the cleaning process. The refreshed one must be rescued.
        configA.setOnClose(() -> updateLastUsedNanos(clientB));
        configB.setOnClose(() -> updateLastUsedNanos(clientA));

        Thread.sleep(10);
        RpcClusterClientManager.scanUnusedClient();
        assertEquals("the client used again before closing should be rescued", 1, innerMap.size());
        RpcClusterClientManager.shutdownBackendConfig(backendConfig);
    }

    /**
     * A failure while closing one client must not break the cleaning of the others.
     */
    @Test
    public void testScanContinuesWhenClosingFails() throws Exception {
        BackendConfig backendConfig = new BackendConfig();
        backendConfig.setIdleTimeout(1);
        backendConfig.setNamingUrl("ip://127.0.0.1:8089");
        ProtocolConfigTest config = new ProtocolConfigTest("127.0.0.1", 8090);
        config.setOnClose(() -> {
            throw new IllegalStateException("close failed");
        });
        RpcClusterClientManager.getOrCreateClient(backendConfig, config);
        Map<String, Object> innerMap = innerClientsOf(backendConfig);
        assertEquals(1, innerMap.size());

        Thread.sleep(10);
        RpcClusterClientManager.scanUnusedClient();
        assertEquals("client should be removed even if closing it throws", 0, innerMap.size());
        RpcClusterClientManager.shutdownBackendConfig(backendConfig);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> innerClientsOf(BackendConfig backendConfig) throws Exception {
        Field field = RpcClusterClientManager.class.getDeclaredField("CLUSTER_MAP");
        field.setAccessible(true);
        Map<BackendConfig, Map<String, Object>> clusterMap =
                (Map<BackendConfig, Map<String, Object>>) field.get(null);
        Map<String, Object> innerMap = clusterMap.get(backendConfig);
        Assert.assertNotNull(innerMap);
        return innerMap;
    }

    private static void updateLastUsedNanos(Object clientProxy) {
        try {
            Method method = clientProxy.getClass().getMethod("updateLastUsedNanos");
            method.setAccessible(true);
            method.invoke(clientProxy);
        } catch (Exception ex) {
            throw new IllegalStateException("failed to update lastUsedNanos", ex);
        }
    }

    private static class ProtocolConfigTest extends ProtocolConfig {

        private Runnable onClose = () -> {
        };

        ProtocolConfigTest() {
        }

        ProtocolConfigTest(String ip, int port) {
            setIp(ip);
            setPort(port);
            setNetwork("tcp");
        }

        void setOnClose(Runnable onClose) {
            this.onClose = onClose;
        }

        @Override
        public RpcClient createClient() {
            return new TestRpcClient(this, () -> onClose.run());
        }
    }

    private static class InFlightProtocolConfigTest extends ProtocolConfig {

        private volatile int pendingRequestCount;

        InFlightProtocolConfigTest(String ip, int port) {
            setIp(ip);
            setPort(port);
            setNetwork("tcp");
        }

        void setPendingRequestCount(int pendingRequestCount) {
            this.pendingRequestCount = pendingRequestCount;
        }

        @Override
        public RpcClient createClient() {
            return new TestRpcClient(this, () -> {
            }) {
                @Override
                public int getPendingRequestCount() {
                    return pendingRequestCount;
                }
            };
        }
    }

    /**
     * Test client which does not override {@link RpcClient#getPendingRequestCount()}, so the default
     * implementation (no in-flight request) is exercised as well.
     */
    private static class TestRpcClient implements RpcClient {

        private final ProtocolConfig protocolConfig;

        private final Runnable onClose;

        TestRpcClient(ProtocolConfig protocolConfig, Runnable onClose) {
            this.protocolConfig = protocolConfig;
            this.onClose = onClose;
        }

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
            return protocolConfig;
        }

        @Override
        public void close() {
            onClose.run();
        }

        @Override
        public <T> ConsumerInvoker<T> createInvoker(ConsumerConfig<T> consumerConfig) {
            return null;
        }

        @Override
        public CloseFuture<Void> closeFuture() {
            return new CloseFuture<Void>();
        }
    }
}
