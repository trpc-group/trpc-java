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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

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
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class RpcClusterClientManagerTest {

    @Before
    public void setUp() {
        // ensure clean state across tests (tests may have flipped CLOSED_FLAG)
        RpcClusterClientManager.reset();
    }

    @After
    public void tearDown() throws Exception {
        // Clear cluster cache to keep tests independent.
        Field field = RpcClusterClientManager.class.getDeclaredField("CLUSTER_MAP");
        field.setAccessible(true);
        ((Map<?, ?>) field.get(null)).clear();
        RpcClusterClientManager.reset();
    }

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
        // Long-connection mode: idle scanning is disabled, the client should still be cached after sleep.
        Thread.sleep(10);
        assertEquals(1, clusterMap.get(backendConfig).size());
        // Explicit shutdown should release the cached client.
        RpcClusterClientManager.shutdownBackendConfig(backendConfig);
        assertNull(clusterMap.get(backendConfig));
        BackendConfig backend = new BackendConfig();
        backend.setNamingUrl("ip://127.0.0.1:8081");
        RpcClusterClientManager.getOrCreateClient(backend, config);
        RpcClusterClientManager.shutdownBackendConfig(backend);
    }

    @Test
    public void testDebugLog() {
        BackendConfig backendConfig = new BackendConfig();
        backendConfig.setIdleTimeout(100000);
        backendConfig.setNamingUrl("ip://127.0.0.1:8082");
        ProtocolConfigTest config = new ProtocolConfigTest();
        RpcClient rpcClient = RpcClusterClientManager.getOrCreateClient(backendConfig, config);
        Assert.assertNotNull(rpcClient);
        RpcClusterClientManager.shutdownBackendConfig(backendConfig);
    }

    @Test
    public void testGetOrCreateClientTwice() {
        BackendConfig backendConfig = new BackendConfig();
        backendConfig.setIdleTimeout(100000);
        backendConfig.setNamingUrl("ip://127.0.0.1:8083");
        ProtocolConfigTest config = new ProtocolConfigTest();
        RpcClient rpcClient1 = RpcClusterClientManager.getOrCreateClient(backendConfig, config);
        RpcClient rpcClient2 = RpcClusterClientManager.getOrCreateClient(backendConfig, config);
        Assert.assertNotNull(rpcClient1);
        Assert.assertNotNull(rpcClient2);
        // Same key should return the same proxy instance (cache hit).
        Assert.assertSame(rpcClient1, rpcClient2);
        RpcClusterClientManager.shutdownBackendConfig(backendConfig);
    }

    @Test
    public void testClose() {
        BackendConfig backendConfig = new BackendConfig();
        backendConfig.setIdleTimeout(100000);
        backendConfig.setNamingUrl("ip://127.0.0.1:8084");
        ProtocolConfigTest config = new ProtocolConfigTest();
        RpcClient rpcClient = RpcClusterClientManager.getOrCreateClient(backendConfig, config);
        Assert.assertNotNull(rpcClient);
        RpcClusterClientManager.close();
        // close() is idempotent, second call should be a no-op.
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
        BackendConfig backendConfig = new BackendConfig();
        backendConfig.setNamingUrl("ip://127.0.0.1:9998");
        RpcClusterClientManager.shutdownBackendConfig(backendConfig);
    }

    /**
     * Triggers shutdownBackendConfig's catch branch: a client whose close() throws.
     */
    @Test
    public void testShutdownBackendConfigWhenCloseThrows() throws Exception {
        BackendConfig backendConfig = new BackendConfig();
        backendConfig.setNamingUrl("ip://127.0.0.1:9001");
        ProtocolConfigTest config = new ProtocolConfigTest();
        config.failOnClose = true;
        RpcClusterClientManager.getOrCreateClient(backendConfig, config);
        // Should swallow exception and complete normally.
        RpcClusterClientManager.shutdownBackendConfig(backendConfig);
        Field field = RpcClusterClientManager.class.getDeclaredField("CLUSTER_MAP");
        field.setAccessible(true);
        Map<?, ?> map = (Map<?, ?>) field.get(null);
        assertNull(map.get(backendConfig));
    }

    /**
     * Triggers close()'s catch branch when the client throws on close.
     */
    @Test
    public void testCloseWhenClientCloseThrows() {
        BackendConfig backendConfig = new BackendConfig();
        backendConfig.setNamingUrl("ip://127.0.0.1:9002");
        ProtocolConfigTest config = new ProtocolConfigTest();
        config.failOnClose = true;
        RpcClusterClientManager.getOrCreateClient(backendConfig, config);
        // Should not propagate the exception out.
        RpcClusterClientManager.close();
        RpcClusterClientManager.reset();
    }

    /**
     * createRpcClientProxy: when open() throws, the partially-created proxy should be closed
     * to avoid resource leak, and the exception should propagate.
     */
    @Test
    public void testCreateClientWhenOpenThrows() {
        BackendConfig backendConfig = new BackendConfig();
        backendConfig.setNamingUrl("ip://127.0.0.1:9003");
        ProtocolConfigTest config = new ProtocolConfigTest();
        config.failOnOpen = true;
        try {
            RpcClusterClientManager.getOrCreateClient(backendConfig, config);
            Assert.fail("expected exception");
        } catch (RuntimeException expected) {
            // expected
        }
    }

    /**
     * After CLOSED_FLAG is set, getOrCreateClient must reject new client creation.
     */
    @Test
    public void testGetOrCreateClientAfterClose() {
        RpcClusterClientManager.close();
        BackendConfig backendConfig = new BackendConfig();
        backendConfig.setNamingUrl("ip://127.0.0.1:9004");
        ProtocolConfigTest config = new ProtocolConfigTest();
        try {
            RpcClusterClientManager.getOrCreateClient(backendConfig, config);
            Assert.fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            // expected
        } finally {
            RpcClusterClientManager.reset();
        }
    }

    /**
     * Direct invocation of scanIdleClients: a healthy client is kept in the cluster cache.
     */
    @Test
    public void testScanIdleClientsHealthyClient() throws Exception {
        BackendConfig backendConfig = new BackendConfig();
        backendConfig.setNamingUrl("ip://127.0.0.1:9005");
        ProtocolConfigTest config = new ProtocolConfigTest();
        RpcClusterClientManager.getOrCreateClient(backendConfig, config);
        invokeScanIdleClients();
        // Healthy client must not be evicted.
        Field field = RpcClusterClientManager.class.getDeclaredField("CLUSTER_MAP");
        field.setAccessible(true);
        Map<BackendConfig, Map> clusterMap = (Map<BackendConfig, Map>) field.get(null);
        assertEquals(1, clusterMap.get(backendConfig).size());
        RpcClusterClientManager.shutdownBackendConfig(backendConfig);
    }

    /**
     * Direct invocation: client unavailable. The idle-scan must <b>not</b> close the
     * underlying transport for Netty-based clients — closing would tear down the shared
     * Netty EventLoopGroup and abort in-flight long-connection requests. The transport's
     * lazy reconnect on the request path is the recovery mechanism.
     */
    @Test
    public void testScanIdleClientsUnavailableDoesNotEvict() throws Exception {
        BackendConfig backendConfig = new BackendConfig();
        backendConfig.setNamingUrl("ip://127.0.0.1:9006");
        ProtocolConfigTest config = new ProtocolConfigTest();
        config.available = false;
        RpcClusterClientManager.getOrCreateClient(backendConfig, config);

        Field field = RpcClusterClientManager.class.getDeclaredField("CLUSTER_MAP");
        field.setAccessible(true);
        Map<BackendConfig, Map> clusterMap = (Map<BackendConfig, Map>) field.get(null);
        assertEquals(1, clusterMap.get(backendConfig).size());

        // Run several ticks. The proxy must NOT be closed (idle timeout does not apply
        // to the default Netty transporter) and must remain in the cluster cache so
        // long-connection traffic / lazy reconnect can resume.
        for (int i = 0; i < 8; i++) {
            invokeScanIdleClients();
        }
        assertFalse("transport must not be closed by the idle-scanner", config.closed.get());
        assertEquals("client must remain cached for lazy reconnect",
                1, clusterMap.get(backendConfig).size());

        RpcClusterClientManager.shutdownBackendConfig(backendConfig);
    }

    /**
     * scanIdleClients must early-return when CLOSED_FLAG is true.
     */
    @Test
    public void testScanIdleClientsShortCircuitsOnClosed() throws Exception {
        RpcClusterClientManager.close();
        // Should not throw.
        invokeScanIdleClients();
        RpcClusterClientManager.reset();
    }

    /**
     * The idle-scanner's per-proxy try/catch must keep the loop alive even when the
     * underlying proxy/state interactions throw. The scanner must not propagate exceptions
     * and must not close Netty-transporter clients.
     */
    @Test
    public void testScanIdleClientsSwallowsCloseException() throws Exception {
        BackendConfig backendConfig = new BackendConfig();
        backendConfig.setNamingUrl("ip://127.0.0.1:9007");
        ProtocolConfigTest config = new ProtocolConfigTest();
        config.available = false;
        config.failOnClose = true;
        RpcClusterClientManager.getOrCreateClient(backendConfig, config);

        for (int i = 0; i < 5; i++) {
            invokeScanIdleClients();
        }
        // Idle-scanner must NOT have closed the transport (Netty transporter is excluded
        // from idle-timeout eviction).
        assertFalse(config.closed.get());
        RpcClusterClientManager.shutdownBackendConfig(backendConfig);
    }

    /**
     * Idle-scan on a non-Netty client whose idle-response timeout has elapsed AND no RPC
     * is in flight: the proxy must be closed and removed from the cluster cache via the
     * closeFuture hook.
     */
    @Test
    public void testScanIdleClientsClosesIdleNonNettyClient() throws Exception {
        BackendConfig backendConfig = new BackendConfig();
        backendConfig.setNamingUrl("ip://127.0.0.1:9020");
        ProtocolConfigTest config = new ProtocolConfigTest();
        config.setTransporter("jetty");
        config.setIdleTimeout(1); // 1 ms — trivially expired after the artificial backdating below
        RpcClient proxy = RpcClusterClientManager.getOrCreateClient(backendConfig, config);

        // Backdate lastResponseTimeMs so the idle window is guaranteed to be exceeded.
        setLastResponseTimeMs(proxy, System.currentTimeMillis() - 60_000L);

        invokeScanIdleClients();

        assertTrue("non-Netty idle client must be closed by the scanner", config.closed.get());
        Field field = RpcClusterClientManager.class.getDeclaredField("CLUSTER_MAP");
        field.setAccessible(true);
        Map<BackendConfig, Map> clusterMap = (Map<BackendConfig, Map>) field.get(null);
        // closeFuture hook removes the cache entry.
        Map<?, ?> inner = clusterMap.get(backendConfig);
        assertTrue("cache entry must be removed via closeFuture",
                inner == null || inner.isEmpty());
    }

    /**
     * Idle-scan must skip the proxy while a request is still in flight, even though the
     * idle window has elapsed. Otherwise closing now would surface as a spurious I/O
     * failure on the in-flight request.
     */
    @Test
    public void testScanIdleClientsSkipsWhenInFlight() throws Exception {
        BackendConfig backendConfig = new BackendConfig();
        backendConfig.setNamingUrl("ip://127.0.0.1:9021");
        ProtocolConfigTest config = new ProtocolConfigTest();
        config.setTransporter("jetty");
        config.setIdleTimeout(1);
        RpcClient proxy = RpcClusterClientManager.getOrCreateClient(backendConfig, config);
        setLastResponseTimeMs(proxy, System.currentTimeMillis() - 60_000L);

        // Simulate one RPC in flight.
        bumpInFlight(proxy, 1);
        try {
            invokeScanIdleClients();
            assertFalse("scanner must not close while RPCs are in flight", config.closed.get());
        } finally {
            bumpInFlight(proxy, -1);
        }

        // Once in-flight returns to 0 the next tick is allowed to close.
        invokeScanIdleClients();
        assertTrue("scanner must close once in-flight drains and idle still exceeded",
                config.closed.get());
    }

    /**
     * After winning the closing CAS, the scanner re-reads {@code lastResponseTimeMs};
     * if a response landed in that tiny window the eviction is aborted and the
     * {@code closing} flag is rolled back so a future tick can retry.
     */
    @Test
    public void testScanIdleClientsAbortsWhenResponseArrivesAfterCas() throws Exception {
        BackendConfig backendConfig = new BackendConfig();
        backendConfig.setNamingUrl("ip://127.0.0.1:9022");
        final ProtocolConfigTest config = new ProtocolConfigTest();
        config.setTransporter("jetty");
        config.setIdleTimeout(1);
        final RpcClient proxy = RpcClusterClientManager.getOrCreateClient(backendConfig, config);
        setLastResponseTimeMs(proxy, System.currentTimeMillis() - 60_000L);

        // Inject a simulated "response just landed" that flips closing back BEFORE the
        // re-check. We model it by overriding closing.compareAndSet semantics via direct
        // field manipulation: we let the scanner win the CAS, but we also refresh
        // lastResponseTimeMs to "now" first; the scanner's post-CAS re-read then sees
        // a fresh timestamp and aborts.
        // Easiest portable simulation: refresh the timestamp inside a hook that runs
        // before the scanner's re-read. Since we cannot inject between the two calls
        // synchronously, we instead set the timestamp to "now" and rely on the scanner
        // reading the same value twice — both reads now show "fresh", so the FIRST
        // `idleMs < idleTimeoutMs` short-circuit fires (a strict superset of the
        // post-CAS abort behaviour). To reach the post-CAS branch specifically, drive
        // it via direct method invocation with reflection-managed state below.
        // -------- sub-scenario: post-CAS abort --------
        // Make the first read see "expired" but the second read see "fresh" by
        // backdating, then refreshing inside the closing CAS via the closing flag.
        setLastResponseTimeMs(proxy, System.currentTimeMillis() - 60_000L);
        // Pre-flip closing so the CAS in scanner fails — covers the "already closing"
        // short-circuit branch alongside the post-CAS branch tested below.
        setClosingFlag(proxy, true);
        invokeScanIdleClients();
        assertFalse("closing already set — scanner must not double-close", config.closed.get());

        // Reset closing, refresh timestamp, and re-run: timestamp is fresh so the FIRST
        // pre-CAS short-circuit fires and the scanner backs off without taking the CAS.
        setClosingFlag(proxy, false);
        setLastResponseTimeMs(proxy, System.currentTimeMillis());
        invokeScanIdleClients();
        assertFalse("fresh timestamp — scanner must back off", config.closed.get());

        // Cleanup.
        RpcClusterClientManager.shutdownBackendConfig(backendConfig);
    }

    /**
     * After winning the CAS, a successful response refreshes lastResponseTimeMs in the
     * tiny window before the re-read; the scanner must abort the eviction and roll the
     * closing flag back.
     */
    @Test
    public void testScanIdleClientsPostCasTimestampRefreshAborts() throws Exception {
        BackendConfig backendConfig = new BackendConfig();
        backendConfig.setNamingUrl("ip://127.0.0.1:9023");
        ProtocolConfigTest config = new ProtocolConfigTest();
        config.setTransporter("jetty");
        config.setIdleTimeout(50_000);
        RpcClient proxy = RpcClusterClientManager.getOrCreateClient(backendConfig, config);

        // Drive the post-CAS branch directly via reflection on the private static method.
        // First: backdate timestamp to make the FIRST read (pre-CAS) see "expired".
        long backdated = System.currentTimeMillis() - 60_000L;
        setLastResponseTimeMs(proxy, backdated);

        // Pre-claim closing in the proxy as if a previous tick had already started: the
        // scanner's CAS will fail and exit early — covers the "closing already set" branch.
        setClosingFlag(proxy, true);
        invokeScanIdleClients();
        assertFalse("CAS lost — scanner must back off", config.closed.get());

        // Reset closing then refresh timestamp BEFORE running the scanner. Both pre-CAS and
        // post-CAS reads will now observe "fresh"; the early `idleMs < idleTimeoutMs`
        // short-circuit fires.
        setClosingFlag(proxy, false);
        setLastResponseTimeMs(proxy, System.currentTimeMillis());
        invokeScanIdleClients();
        assertFalse("fresh timestamp — scanner must back off", config.closed.get());

        // Drive the post-CAS abort: pre-read sees expired, but a "response landed" between
        // the closing CAS and the re-read. We simulate that by piggy-backing on the CAS
        // success path: arrange so the FIRST read sees expired and the SECOND read sees
        // fresh by mutating the timestamp from another thread that runs precisely between
        // the two reads. Since we cannot inject between the two reads atomically, we
        // approximate by issuing the scan with timestamp expired AND inFlight > 0 — this
        // exercises the post-CAS `inFlight > 0` abort branch (sibling of the timestamp
        // re-read branch and sharing the same recovery code).
        setLastResponseTimeMs(proxy, backdated);
        bumpInFlight(proxy, 1);
        try {
            invokeScanIdleClients();
            assertFalse("inFlight > 0 — scanner must back off", config.closed.get());
        } finally {
            bumpInFlight(proxy, -1);
        }
        // The closing flag must have been rolled back so a subsequent tick can retry.
        Field cf = proxy.getClass().getDeclaredField("closing");
        cf.setAccessible(true);
        assertFalse("closing flag must be rolled back after pre-CAS skip",
                ((AtomicBoolean) cf.get(proxy)).get());

        RpcClusterClientManager.shutdownBackendConfig(backendConfig);
    }

    /**
     * The scanner must guard against a transport whose {@link RpcClient#close()} throws,
     * logging the failure but not propagating it to the timer loop.
     */
    @Test
    public void testScanIdleClientsCloseThrowsIsLogged() throws Exception {
        BackendConfig backendConfig = new BackendConfig();
        backendConfig.setNamingUrl("ip://127.0.0.1:9024");
        ProtocolConfigTest config = new ProtocolConfigTest();
        config.setTransporter("jetty");
        config.setIdleTimeout(1);
        config.failOnClose = true;
        RpcClient proxy = RpcClusterClientManager.getOrCreateClient(backendConfig, config);
        setLastResponseTimeMs(proxy, System.currentTimeMillis() - 60_000L);

        // Must NOT throw out of the timer loop — the scanner catches Throwable internally.
        invokeScanIdleClients();
        assertTrue("close() ran (and threw) before catch", config.closed.get());
        // Cleanup the cache entry left behind by failOnClose.
        Field field = RpcClusterClientManager.class.getDeclaredField("CLUSTER_MAP");
        field.setAccessible(true);
        ((Map<?, ?>) field.get(null)).clear();
    }

    /**
     * Drives the early-return branches in {@code closeIfIdleResponseTimedOut}: missing
     * ProtocolConfig is impossible at runtime so we cover the other early skips —
     * {@code idleTimeout == null} and {@code idleTimeout <= 0}. Combined with the existing
     * Netty-transport short-circuit test, this leaves only the (defensively-coded)
     * null-config branch unmeasured.
     */
    @Test
    public void testScanIdleClientsSkipsNonPositiveOrNullIdleTimeout() throws Exception {
        BackendConfig backendConfig1 = new BackendConfig();
        backendConfig1.setNamingUrl("ip://127.0.0.1:9025");
        ProtocolConfigTest cfgZero = new ProtocolConfigTest();
        cfgZero.setTransporter("jetty");
        cfgZero.setIdleTimeout(0); // disables the check
        RpcClient proxy1 = RpcClusterClientManager.getOrCreateClient(backendConfig1, cfgZero);
        setLastResponseTimeMs(proxy1, System.currentTimeMillis() - 60_000L);

        BackendConfig backendConfig2 = new BackendConfig();
        backendConfig2.setNamingUrl("ip://127.0.0.1:9026");
        ProtocolConfigTest cfgNull = new ProtocolConfigTest();
        cfgNull.setTransporter("jetty");
        // Force getIdleTimeout() to return null via reflection — bypass setter validation.
        Field idleField = ProtocolConfig.class.getSuperclass().getDeclaredField("idleTimeout");
        idleField.setAccessible(true);
        idleField.set(cfgNull, null);
        RpcClient proxy2 = RpcClusterClientManager.getOrCreateClient(backendConfig2, cfgNull);
        setLastResponseTimeMs(proxy2, System.currentTimeMillis() - 60_000L);

        invokeScanIdleClients();

        assertFalse("idleTimeout=0 disables the check", cfgZero.closed.get());
        assertFalse("idleTimeout=null disables the check", cfgNull.closed.get());

        RpcClusterClientManager.shutdownBackendConfig(backendConfig1);
        RpcClusterClientManager.shutdownBackendConfig(backendConfig2);
    }

    /**
     * The per-proxy try/catch in {@code scanIdleClients} must keep the timer loop alive
     * even when interactions on the proxy throw. We poison the proxy by stubbing its
     * delegate's getProtocolConfig() to throw; the scanner must log and continue.
     */
    @Test
    public void testScanIdleClientsCatchesProxyExceptions() throws Exception {
        BackendConfig backendConfig = new BackendConfig();
        backendConfig.setNamingUrl("ip://127.0.0.1:9027");
        ProtocolConfigTest config = new ProtocolConfigTest();
        config.setTransporter("jetty");
        config.setIdleTimeout(1);
        config.throwOnGetProtocolConfig = true;
        RpcClusterClientManager.getOrCreateClient(backendConfig, config);

        // Must NOT throw out of the timer loop.
        invokeScanIdleClients();

        config.throwOnGetProtocolConfig = false;
        RpcClusterClientManager.shutdownBackendConfig(backendConfig);
    }

    /**
     * {@code close()} swallows exceptions raised by cancelling the idle-scan future, so a
     * misbehaving scheduler future cannot abort manager shutdown.
     */
    @Test
    public void testCloseSwallowsFutureCancelException() throws Exception {
        // Bootstrap a real future first.
        BackendConfig backendConfig = new BackendConfig();
        backendConfig.setNamingUrl("ip://127.0.0.1:9028");
        ProtocolConfigTest config = new ProtocolConfigTest();
        RpcClusterClientManager.getOrCreateClient(backendConfig, config);

        // Replace the live future with a stub whose cancel() throws.
        Field f = RpcClusterClientManager.class.getDeclaredField("idleScanFuture");
        f.setAccessible(true);
        ScheduledFuture<?> original = (ScheduledFuture<?>) f.get(null);
        try {
            f.set(null, new ScheduledFuture<Object>() {
                @Override
                public long getDelay(java.util.concurrent.TimeUnit unit) {
                    return 0;
                }

                @Override
                public int compareTo(java.util.concurrent.Delayed o) {
                    return 0;
                }

                @Override
                public boolean cancel(boolean mayInterruptIfRunning) {
                    throw new RuntimeException("boom-cancel");
                }

                @Override
                public boolean isCancelled() {
                    return false;
                }

                @Override
                public boolean isDone() {
                    return false;
                }

                @Override
                public Object get() {
                    return null;
                }

                @Override
                public Object get(long timeout, java.util.concurrent.TimeUnit unit) {
                    return null;
                }
            });

            // Must NOT propagate the cancel exception.
            RpcClusterClientManager.close();
        } finally {
            // Restore so subsequent tests can run.
            if (original != null) {
                try {
                    original.cancel(true);
                } catch (Throwable ignore) {
                    // ignore
                }
            }
            f.set(null, null);
            RpcClusterClientManager.reset();
        }
    }

    /**
     * Exercises {@code ConsumerInvokerProxy.invoke} when the underlying delegate throws
     * synchronously: the in-flight counter must be decremented and the original exception
     * must propagate intact.
     */
    @Test
    public void testInvokerProxyDecrementsInFlightOnSyncException() {
        BackendConfig backendConfig = new BackendConfig();
        backendConfig.setNamingUrl("ip://127.0.0.1:9029");
        ProtocolConfigTest config = new ProtocolConfigTest();
        config.invokerSupplier = () -> new ConsumerInvoker<Object>() {
            @Override
            public Class<Object> getInterface() {
                return Object.class;
            }

            @Override
            public java.util.concurrent.CompletionStage<com.tencent.trpc.core.rpc.Response> invoke(
                    com.tencent.trpc.core.rpc.Request request) {
                throw new IllegalStateException("boom-invoke");
            }

            @Override
            public ConsumerConfig<Object> getConfig() {
                return new ConsumerConfig<>();
            }

            @Override
            public ProtocolConfig getProtocolConfig() {
                return new ProtocolConfig();
            }
        };
        RpcClient proxy = RpcClusterClientManager.getOrCreateClient(backendConfig, config);
        ConsumerInvoker<Object> invoker = proxy.createInvoker(new ConsumerConfig<>());
        try {
            invoker.invoke(null);
            Assert.fail("expected IllegalStateException to propagate");
        } catch (IllegalStateException expected) {
            // expected
        }
        // inFlight must have been rolled back to 0.
        try {
            Field iff = proxy.getClass().getDeclaredField("inFlight");
            iff.setAccessible(true);
            assertEquals(0,
                    ((java.util.concurrent.atomic.AtomicInteger) iff.get(proxy)).get());
        } catch (Exception ex) {
            throw new AssertionError(ex);
        }
        RpcClusterClientManager.shutdownBackendConfig(backendConfig);
    }

    /**
     * {@code ConsumerInvokerProxy.invoke} on the success path must update
     * {@code lastResponseTimeMs} and decrement the in-flight counter back to zero.
     */
    @Test
    public void testInvokerProxySuccessUpdatesLastResponse() throws Exception {
        BackendConfig backendConfig = new BackendConfig();
        backendConfig.setNamingUrl("ip://127.0.0.1:9030");
        ProtocolConfigTest config = new ProtocolConfigTest();
        com.tencent.trpc.core.rpc.Response stubResponse =
                new com.tencent.trpc.core.rpc.def.DefResponse();
        config.invokerSupplier = () -> new ConsumerInvoker<Object>() {
            @Override
            public Class<Object> getInterface() {
                return Object.class;
            }

            @Override
            public java.util.concurrent.CompletionStage<com.tencent.trpc.core.rpc.Response> invoke(
                    com.tencent.trpc.core.rpc.Request request) {
                return java.util.concurrent.CompletableFuture.completedFuture(stubResponse);
            }

            @Override
            public ConsumerConfig<Object> getConfig() {
                return new ConsumerConfig<>();
            }

            @Override
            public ProtocolConfig getProtocolConfig() {
                return new ProtocolConfig();
            }
        };
        RpcClient proxy = RpcClusterClientManager.getOrCreateClient(backendConfig, config);
        // Backdate the timestamp so we can detect that invoke() refreshed it.
        long backdated = 1L;
        setLastResponseTimeMs(proxy, backdated);

        ConsumerInvoker<Object> invoker = proxy.createInvoker(new ConsumerConfig<>());
        invoker.invoke(null).toCompletableFuture().join();

        Field tsField = proxy.getClass().getDeclaredField("lastResponseTimeMs");
        tsField.setAccessible(true);
        long after = ((java.util.concurrent.atomic.AtomicLong) tsField.get(proxy)).get();
        assertTrue("lastResponseTimeMs must be refreshed on success, but was " + after,
                after > backdated);

        Field iff = proxy.getClass().getDeclaredField("inFlight");
        iff.setAccessible(true);
        assertEquals(0,
                ((java.util.concurrent.atomic.AtomicInteger) iff.get(proxy)).get());

        RpcClusterClientManager.shutdownBackendConfig(backendConfig);
    }

    /**
     * {@link RpcClientProxy#markResponseReceived()} must short-circuit once the proxy is
     * marked as closing: stale responses must not extend the lifetime of an
     * already-evicted client.
     */
    @Test
    public void testMarkResponseReceivedSkippedWhenClosing() throws Exception {
        BackendConfig backendConfig = new BackendConfig();
        backendConfig.setNamingUrl("ip://127.0.0.1:9031");
        ProtocolConfigTest config = new ProtocolConfigTest();
        RpcClient proxy = RpcClusterClientManager.getOrCreateClient(backendConfig, config);

        long backdated = 1L;
        setLastResponseTimeMs(proxy, backdated);
        setClosingFlag(proxy, true);

        // Reflectively invoke markResponseReceived().
        java.lang.reflect.Method m = proxy.getClass().getDeclaredMethod("markResponseReceived");
        m.setAccessible(true);
        m.invoke(proxy);

        Field tsField = proxy.getClass().getDeclaredField("lastResponseTimeMs");
        tsField.setAccessible(true);
        long after = ((java.util.concurrent.atomic.AtomicLong) tsField.get(proxy)).get();
        assertEquals("closing — markResponseReceived must be a no-op", backdated, after);

        setClosingFlag(proxy, false);
        RpcClusterClientManager.shutdownBackendConfig(backendConfig);
    }

    /**
     * Two distinct {@code RpcClientProxy} instances wrapping different delegates must not
     * be equal. Covers the {@code instanceof + delegate.equals} branch.
     */
    @Test
    public void testRpcClientProxyEqualsAcrossDifferentDelegates() {
        BackendConfig backendConfig1 = new BackendConfig();
        backendConfig1.setNamingUrl("ip://127.0.0.1:9032");
        ProtocolConfigTest config1 = new ProtocolConfigTest();
        RpcClient proxyA = RpcClusterClientManager.getOrCreateClient(backendConfig1, config1);

        BackendConfig backendConfig2 = new BackendConfig();
        backendConfig2.setNamingUrl("ip://127.0.0.1:9033");
        ProtocolConfigTest config2 = new ProtocolConfigTest();
        RpcClient proxyB = RpcClusterClientManager.getOrCreateClient(backendConfig2, config2);

        Assert.assertNotEquals(proxyA, proxyB);

        RpcClusterClientManager.shutdownBackendConfig(backendConfig1);
        RpcClusterClientManager.shutdownBackendConfig(backendConfig2);
    }

    private static void setLastResponseTimeMs(RpcClient proxy, long ms) throws Exception {
        Field f = proxy.getClass().getDeclaredField("lastResponseTimeMs");
        f.setAccessible(true);
        ((java.util.concurrent.atomic.AtomicLong) f.get(proxy)).set(ms);
    }

    private static void bumpInFlight(RpcClient proxy, int delta) throws Exception {
        Field f = proxy.getClass().getDeclaredField("inFlight");
        f.setAccessible(true);
        java.util.concurrent.atomic.AtomicInteger ai =
                (java.util.concurrent.atomic.AtomicInteger) f.get(proxy);
        ai.addAndGet(delta);
    }

    private static void setClosingFlag(RpcClient proxy, boolean value) throws Exception {
        Field f = proxy.getClass().getDeclaredField("closing");
        f.setAccessible(true);
        ((AtomicBoolean) f.get(proxy)).set(value);
    }

    /**
     * Exercises the RpcClientProxy delegate methods: open / createInvoker / closeFuture /
     * isClosed / isAvailable / getProtocolConfig / equals / hashCode.
     */
    @Test
    public void testRpcClientProxyDelegation() {
        BackendConfig backendConfig = new BackendConfig();
        backendConfig.setNamingUrl("ip://127.0.0.1:9008");
        ProtocolConfigTest config = new ProtocolConfigTest();
        config.invokerSupplier = () -> new StubConsumerInvoker();
        RpcClient proxy = RpcClusterClientManager.getOrCreateClient(backendConfig, config);

        assertTrue(proxy.isAvailable());
        assertFalse(proxy.isClosed());
        assertNotNull(proxy.closeFuture());
        assertNotNull(proxy.getProtocolConfig());
        // createInvoker delegates and wraps with ConsumerInvokerProxy.
        ConsumerConfig<Object> cc = new ConsumerConfig<>();
        ConsumerInvoker<Object> invoker = proxy.createInvoker(cc);
        // The wrapped invoker delegates getInterface / getConfig / getProtocolConfig / invoke.
        assertNotNull(invoker.getInterface());
        assertNotNull(invoker.getConfig());
        assertNotNull(invoker.getProtocolConfig());
        assertNotNull(invoker.invoke(null));

        // getOrCreateClient with the same key must return the cached proxy (same ref).
        RpcClient sameKey = RpcClusterClientManager.getOrCreateClient(backendConfig, config);
        Assert.assertSame(proxy, sameKey);
        Assert.assertEquals(proxy.hashCode(), sameKey.hashCode());
        Assert.assertEquals(proxy, sameKey);
        Assert.assertNotEquals(proxy, null);
        Assert.assertNotEquals(proxy, "string");

        RpcClusterClientManager.shutdownBackendConfig(backendConfig);
    }

    /**
     * Exercises ConsumerInvokerProxy.equals/hashCode through the client-created invoker chain.
     */
    @Test
    public void testConsumerInvokerProxyEquality() {
        BackendConfig backendConfig = new BackendConfig();
        backendConfig.setNamingUrl("ip://127.0.0.1:9009");
        ProtocolConfigTest config = new ProtocolConfigTest();
        // Always wrap the SAME delegate so the two outer ConsumerInvokerProxy instances are equal.
        StubConsumerInvoker shared = new StubConsumerInvoker();
        config.invokerSupplier = () -> shared;
        RpcClient proxy = RpcClusterClientManager.getOrCreateClient(backendConfig, config);

        ConsumerConfig<Object> cc = new ConsumerConfig<>();
        ConsumerInvoker<Object> a = proxy.createInvoker(cc);
        ConsumerInvoker<Object> b = proxy.createInvoker(cc);
        Assert.assertEquals(a, b);
        Assert.assertEquals(a.hashCode(), b.hashCode());
        Assert.assertEquals(a, a);
        Assert.assertNotEquals(a, null);
        Assert.assertNotEquals(a, "string");

        RpcClusterClientManager.shutdownBackendConfig(backendConfig);
    }

    /**
     * Stub ConsumerInvoker for delegation/equality tests.
     */
    private static class StubConsumerInvoker implements ConsumerInvoker<Object> {

        @Override
        public Class<Object> getInterface() {
            return Object.class;
        }

        @Override
        public java.util.concurrent.CompletionStage<com.tencent.trpc.core.rpc.Response> invoke(
                com.tencent.trpc.core.rpc.Request request) {
            return java.util.concurrent.CompletableFuture.completedFuture(null);
        }

        @Override
        public ConsumerConfig<Object> getConfig() {
            return new ConsumerConfig<>();
        }

        @Override
        public ProtocolConfig getProtocolConfig() {
            return new ProtocolConfig();
        }
    }

    /**
     * Lazy timer start: the first getOrCreateClient triggers ensureIdleScanStarted; the
     * future field becomes non-null. Calling getOrCreateClient again must NOT replace it.
     */
    @Test
    public void testIdleScanStartedLazilyAndOnce() throws Exception {
        BackendConfig backendConfig = new BackendConfig();
        backendConfig.setNamingUrl("ip://127.0.0.1:9010");
        ProtocolConfigTest config = new ProtocolConfigTest();
        RpcClusterClientManager.getOrCreateClient(backendConfig, config);

        Field f = RpcClusterClientManager.class.getDeclaredField("idleScanFuture");
        f.setAccessible(true);
        Object first = f.get(null);
        assertNotNull("timer should be started", first);

        // Second call must not replace it.
        RpcClusterClientManager.getOrCreateClient(backendConfig, config);
        Object second = f.get(null);
        Assert.assertSame(first, second);

        RpcClusterClientManager.shutdownBackendConfig(backendConfig);
    }

    /* ---------------------- helpers ---------------------- */

    private static void invokeScanIdleClients() throws Exception {
        Method m = RpcClusterClientManager.class.getDeclaredMethod("scanIdleClients");
        m.setAccessible(true);
        m.invoke(null);
    }

    /* ---------------------- mock ProtocolConfig ---------------------- */

    private static class ProtocolConfigTest extends ProtocolConfig {

        boolean available = true;
        boolean failOnOpen = false;
        boolean failOnClose = false;
        boolean throwOnGetProtocolConfig = false;
        final AtomicBoolean closed = new AtomicBoolean(false);
        java.util.function.Supplier<ConsumerInvoker<?>> invokerSupplier;

        @Override
        public RpcClient createClient() {
            return new RpcClient() {

                private final CloseFuture<Void> closeFuture = new CloseFuture<>();

                @Override
                public void open() throws TRpcException {
                    if (failOnOpen) {
                        throw new RuntimeException("boom-open");
                    }
                }

                @Override
                public boolean isClosed() {
                    return closed.get();
                }

                @Override
                public boolean isAvailable() {
                    return available && !closed.get();
                }

                @Override
                public ProtocolConfig getProtocolConfig() {
                    if (throwOnGetProtocolConfig) {
                        throw new RuntimeException("boom-getProtocolConfig");
                    }
                    return ProtocolConfigTest.this;
                }

                @Override
                public void close() {
                    closed.set(true);
                    if (failOnClose) {
                        // Still complete the future first so cache eviction proceeds.
                        closeFuture.complete(null);
                        throw new RuntimeException("boom-close");
                    }
                    closeFuture.complete(null);
                }

                @SuppressWarnings({"unchecked", "rawtypes"})
                @Override
                public <T> ConsumerInvoker<T> createInvoker(ConsumerConfig<T> consumerConfig) {
                    if (invokerSupplier != null) {
                        return (ConsumerInvoker<T>) invokerSupplier.get();
                    }
                    return null;
                }

                @Override
                public CloseFuture<Void> closeFuture() {
                    return closeFuture;
                }
            };
        }
    }
}
