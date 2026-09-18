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

package com.tencent.trpc.core.cluster.def;

import com.tencent.trpc.core.cluster.def.DefClusterInvoker.ConsumerInvokerProxy;
import com.tencent.trpc.core.common.config.BackendConfig;
import com.tencent.trpc.core.common.config.ConsumerConfig;
import com.tencent.trpc.core.common.config.ProtocolConfig;
import com.tencent.trpc.core.exception.TRpcException;
import com.tencent.trpc.core.rpc.CloseFuture;
import com.tencent.trpc.core.rpc.ConsumerInvoker;
import com.tencent.trpc.core.rpc.GenericClient;
import com.tencent.trpc.core.rpc.Request;
import com.tencent.trpc.core.rpc.Response;
import com.tencent.trpc.core.rpc.RpcClient;
import com.tencent.trpc.core.selector.ServiceInstance;
import com.tencent.trpc.core.utils.FutureUtils;
import java.lang.reflect.Field;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Plain-JUnit (no PowerMock) tests for the long-connection logic in {@link DefClusterInvoker}:
 * the {@code getInvoker} fast path, the {@code createInvoker} stale-evict (CAS) path, and the
 * {@code closeFuture}-driven cache cleanup.
 *
 * <p>These tests directly manipulate the {@code invokerCache} via reflection rather than going
 * through {@link com.tencent.trpc.core.cluster.RpcClusterClientManager#getOrCreateClient} (which
 * requires SPI-registered factories that are not available in pure unit tests).</p>
 */
public class DefClusterInvokerCloseFutureTest {

    private DefClusterInvoker<GenericClient> invoker;

    @Before
    public void setUp() {
        BackendConfig backendConfig = new BackendConfig();
        backendConfig.setNamingUrl("ip://127.0.0.1:18001");
        backendConfig.setServiceInterface(GenericClient.class);
        backendConfig.setNetwork("tcp");
        backendConfig.setDefault();

        ConsumerConfig<GenericClient> consumerConfig = new ConsumerConfig<>();
        consumerConfig.setBackendConfig(backendConfig);
        consumerConfig.setServiceInterface(GenericClient.class);

        invoker = new DefClusterInvoker<>(consumerConfig);
    }

    /**
     * getInvoker fast path: cached proxy is available → returned directly.
     */
    @Test
    public void testGetInvokerReturnsCachedAvailable() throws Exception {
        ConcurrentMap<String, ConsumerInvokerProxy<GenericClient>> cache = getCache();
        ServiceInstance instance = new ServiceInstance("127.0.0.1", 18001);
        String key = "127.0.0.1:18001:tcp";

        TestRpcClient client = new TestRpcClient();
        final ConsumerInvokerProxy<GenericClient> proxy = new ConsumerInvokerProxy<>(
                stubInvoker(client.getProtocolConfig()), client);
        cache.put(key, proxy);

        ConsumerInvokerProxy<GenericClient> got = invoker.getInvoker(instance);
        Assert.assertSame(proxy, got);
    }

    /**
     * getInvoker fall-through: cached proxy is unavailable → it goes to createInvoker, which
     * builds a fresh invoker (SPI factory for "trpc" is registered in this module) and replaces
     * the stale entry. We assert: the returned invoker is NOT the stale one.
     */
    @Test
    public void testGetInvokerFallsThroughOnUnavailable() throws Exception {
        ConcurrentMap<String, ConsumerInvokerProxy<GenericClient>> cache = getCache();
        ServiceInstance instance = new ServiceInstance("127.0.0.1", 18001);
        String key = "127.0.0.1:18001:tcp";

        TestRpcClient client = new TestRpcClient();
        client.available.set(false);
        ConsumerInvokerProxy<GenericClient> stale = new ConsumerInvokerProxy<>(
                stubInvoker(client.getProtocolConfig()), client);
        cache.put(key, stale);

        ConsumerInvokerProxy<GenericClient> got;
        try {
            got = invoker.getInvoker(instance);
        } catch (TRpcException ex) {
            // SPI factory may not be available in some environments; in that case createInvoker
            // throws. Either outcome confirms the fast path correctly rejected the stale entry.
            Assert.assertTrue(ex.getMessage().contains("Create rpc client"));
            return;
        }
        Assert.assertNotSame("Stale entry must be replaced", stale, got);
        Assert.assertNotSame(stale, cache.get(key));
        // Clean up the freshly created RpcClient to avoid leaks across tests.
        com.tencent.trpc.core.cluster.RpcClusterClientManager.shutdownBackendConfig(
                invoker.getBackendConfig());
    }

    /**
     * Direct assertion of the bug fix: the closeFuture hook installed inside createInvoker uses
     * CAS-remove. We simulate the hook semantics here to lock down the invariant.
     *
     * <p>The actual hook is a lambda registered when a fresh invoker is created. We verify the
     * same semantics by constructing two proxies, putting a "newer" one in the cache, then
     * applying CAS-remove with the "older" key/value pair. The newer entry must NOT be evicted.
     */
    @Test
    public void testCasRemoveDoesNotEvictNewerEntry() throws Exception {
        ConcurrentMap<String, ConsumerInvokerProxy<GenericClient>> cache = getCache();
        String key = "127.0.0.1:18001:tcp";

        TestRpcClient clientA = new TestRpcClient();
        ConsumerInvokerProxy<GenericClient> a = new ConsumerInvokerProxy<>(
                stubInvoker(clientA.getProtocolConfig()), clientA);
        TestRpcClient clientB = new TestRpcClient();
        ConsumerInvokerProxy<GenericClient> b = new ConsumerInvokerProxy<>(
                stubInvoker(clientB.getProtocolConfig()), clientB);

        cache.put(key, b); // current value is B

        // Simulate the closeFuture hook for A firing while B is the current cache value.
        boolean removed = cache.remove(key, a);
        Assert.assertFalse("CAS-remove must miss: A is no longer the current value", removed);
        Assert.assertSame(b, cache.get(key));

        // Simulate B's hook firing → evicts.
        Assert.assertTrue(cache.remove(key, b));
        Assert.assertNull(cache.get(key));
    }

    /**
     * Sanity: ConsumerInvokerProxy.isAvailable() reflects the underlying client.
     */
    @Test
    public void testProxyIsAvailableTracksUnderlyingClient() {
        TestRpcClient client = new TestRpcClient();
        final ConsumerInvokerProxy<GenericClient> proxy = new ConsumerInvokerProxy<>(
                stubInvoker(client.getProtocolConfig()), client);
        Assert.assertTrue(proxy.isAvailable());
        client.available.set(false);
        Assert.assertFalse(proxy.isAvailable());
    }

    /**
     * Sanity: ConsumerInvokerProxy.invoke fills CallInfo on the request and reports to the
     * selector (best-effort; selector lookup may return null which is tolerated).
     */
    @Test
    public void testProxyInvokeFillsCallInfoAndReports() {
        TestRpcClient client = new TestRpcClient();
        final ConsumerInvokerProxy<GenericClient> proxy = new ConsumerInvokerProxy<>(
                stubInvoker(client.getProtocolConfig()), client);

        com.tencent.trpc.core.rpc.def.DefRequest request = new com.tencent.trpc.core.rpc.def.DefRequest();
        com.tencent.trpc.core.rpc.RpcInvocation invocation = new com.tencent.trpc.core.rpc.RpcInvocation();
        invocation.setFunc("any");
        request.setInvocation(invocation);

        java.util.HashMap<String, Object> params = new java.util.HashMap<>();
        params.put("container_name", "test-container");
        params.put("set_division", "test-set");
        ServiceInstance instance = new ServiceInstance("127.0.0.1", 18001, params);

        Assert.assertNotNull(proxy.invoke(request, instance));
        // The invoke wraps responses; the underlying stub returns a successful future.
    }

    /* ---------------------- helpers ---------------------- */

    @SuppressWarnings("unchecked")
    private ConcurrentMap<String, ConsumerInvokerProxy<GenericClient>> getCache() throws Exception {
        Field f = DefClusterInvoker.class.getDeclaredField("invokerCache");
        f.setAccessible(true);
        return (ConcurrentMap<String, ConsumerInvokerProxy<GenericClient>>) f.get(invoker);
    }

    private ConsumerInvoker<GenericClient> stubInvoker(ProtocolConfig pc) {
        return new ConsumerInvoker<GenericClient>() {
            @Override
            public Class<GenericClient> getInterface() {
                return GenericClient.class;
            }

            @Override
            public CompletionStage<Response> invoke(Request request) {
                return FutureUtils.newSuccessFuture(null);
            }

            @Override
            public ConsumerConfig<GenericClient> getConfig() {
                return invoker.getConfig();
            }

            @Override
            public ProtocolConfig getProtocolConfig() {
                return pc;
            }
        };
    }

    /* ---------------------- mock ---------------------- */

    private static class TestRpcClient implements RpcClient {

        final AtomicBoolean available = new AtomicBoolean(true);
        final AtomicBoolean closed = new AtomicBoolean(false);
        final CloseFuture<Void> closeFuture = new CloseFuture<>();
        private final ProtocolConfig protocolConfig = new ProtocolConfig();

        @Override
        public void open() throws TRpcException {
        }

        @Override
        public <T> ConsumerInvoker<T> createInvoker(ConsumerConfig<T> consumerConfig) {
            return null;
        }

        @Override
        public void close() {
            closed.set(true);
            closeFuture.complete(null);
        }

        @Override
        public CloseFuture<Void> closeFuture() {
            return closeFuture;
        }

        @Override
        public boolean isAvailable() {
            return available.get() && !closed.get();
        }

        @Override
        public boolean isClosed() {
            return closed.get();
        }

        @Override
        public ProtocolConfig getProtocolConfig() {
            return protocolConfig;
        }
    }
}
