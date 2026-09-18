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

package com.tencent.trpc.proto.standard.concurrenttest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.google.protobuf.ByteString;
import com.tencent.trpc.core.cluster.RpcClusterClientManager;
import com.tencent.trpc.core.common.ConfigManager;
import com.tencent.trpc.core.common.config.BackendConfig;
import com.tencent.trpc.core.common.config.ProviderConfig;
import com.tencent.trpc.core.common.config.ServiceConfig;
import com.tencent.trpc.core.rpc.RpcClientContext;
import com.tencent.trpc.core.rpc.RpcServerContext;
import com.tencent.trpc.core.transport.AbstractClientTransport;
import com.tencent.trpc.core.transport.Channel;
import com.tencent.trpc.core.transport.ClientTransport;
import com.tencent.trpc.proto.standard.common.HelloRequestProtocol.HelloRequest;
import com.tencent.trpc.proto.standard.common.HelloRequestProtocol.HelloResponse;
import com.tencent.trpc.proto.support.DefResponseFutureManager;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Verifies that {@link BackendConfig#setNamingUrl(String)} configured with a comma-separated list
 * of {@code ip:port} entries fan-outs requests to all backends (random load balance) under heavy
 * concurrency.
 *
 * <p>Setup: 10 standalone tRPC servers on consecutive ports; one shared {@link BackendConfig}
 * whose namingUrl lists all 10 endpoints; 100 concurrent threads × 1000 requests each.</p>
 *
 * <p>Each server impl echoes back its own listening port so the client can group responses by the
 * actually-served port. Final assertions:
 * <ul>
 *     <li>every request succeeds with the exact echoed payload,</li>
 *     <li>all 10 backend ports get hit at least once (proving namingUrl actually distributes
 *         traffic, not pinning to one endpoint),</li>
 *     <li>distribution is roughly balanced — random selector over N=10 with R=100000 requests
 *         gives an expected 10000 per server; we tolerate {@code [2000, 20000]} per server which
 *         is a generous bound far above any realistic random outlier.</li>
 * </ul>
 */
public class MultiPortNamingUrlConcurrentTest {

    private static final int BASE_TCP_PORT = 12500;
    private static final int SERVER_COUNT = 10;
    private static final int THREAD_COUNT = 100;
    private static final int CYCLE_PER_THREAD = 1000;
    /**
     * Concurrency profile for {@link #testIdleTimeoutChannelRecycle()}: {@value #IDLE_THREAD_COUNT}
     * threads × {@value #IDLE_CYCLE_PER_THREAD} requests per round, matching the stress
     * profile of {@link #testMultiPortNamingUrlConcurrent()}. The total wall-clock is
     * dominated by the 20s idle-wait between the two rounds.
     */
    private static final int IDLE_THREAD_COUNT = 100;
    private static final int IDLE_CYCLE_PER_THREAD = 1000;
    private static final int IDLE_CONNS_PER_ADDR = 5;

    private final List<ServiceConfig> serviceConfigs = new ArrayList<>(SERVER_COUNT);

    @Before
    public void before() {
        ConfigManager.stopTest();
        ConfigManager.startTest();
        startServers();
    }

    @After
    public void stop() {
        for (ServiceConfig serviceConfig : serviceConfigs) {
            try {
                serviceConfig.unExport();
            } catch (Exception ignore) {
                // ignore
            }
        }
        serviceConfigs.clear();
        ConfigManager.stopTest();
    }

    @Test
    public void testMultiPortNamingUrlConcurrent() throws InterruptedException {
        // Build the comma-separated namingUrl: "ip://127.0.0.1:p1,127.0.0.1:p2,..."
        StringBuilder urlBuilder = new StringBuilder("ip://");
        for (int i = 0; i < SERVER_COUNT; i++) {
            if (i > 0) {
                urlBuilder.append(',');
            }
            urlBuilder.append("127.0.0.1:").append(BASE_TCP_PORT + i);
        }
        String namingUrl = urlBuilder.toString();

        BackendConfig backendConfig = new BackendConfig();
        DefResponseFutureManager.reset();
        backendConfig.setNamingUrl(namingUrl);
        // One long connection per backend addr is enough; keeps the test deterministic.
        backendConfig.setConnsPerAddr(5);
        backendConfig.setNetwork("tcp");
        // Generous client-side timeout so a slow JIT warm-up can't fail individual calls.
        backendConfig.setRequestTimeout(60_000);

        final ConcurrentTestServiceApi proxy = backendConfig.getProxy(ConcurrentTestServiceApi.class);

        // Per-port hit counter aggregated across all threads.
        ConcurrentHashMap<Integer, AtomicInteger> portHits = new ConcurrentHashMap<>();
        for (int i = 0; i < SERVER_COUNT; i++) {
            portHits.put(BASE_TCP_PORT + i, new AtomicInteger(0));
        }

        CountDownLatch latch = new CountDownLatch(THREAD_COUNT);
        List<TestResult> results = new ArrayList<>(THREAD_COUNT);
        for (int t = 0; t < THREAD_COUNT; t++) {
            final TestResult r = new TestResult();
            results.add(r);
            final int threadIndex = t;
            new Thread(() -> {
                try {
                    for (int i = 0; i < CYCLE_PER_THREAD; i++) {
                        String reqPayload = "req-" + threadIndex + "-" + i;
                        RpcClientContext context = new RpcClientContext();
                        HelloResponse response = proxy.sayHello(context, HelloRequest.newBuilder()
                                .setMessage(ByteString.copyFromUtf8(reqPayload))
                                .build());
                        // Server impl returns "<echoedPayload>|port=<actualPort>"
                        String message = response.getMessage().toStringUtf8();
                        int sep = message.lastIndexOf("|port=");
                        assertTrue("response missing port marker: " + message, sep > 0);
                        String echoed = message.substring(0, sep);
                        int port = Integer.parseInt(message.substring(sep + "|port=".length()));
                        assertEquals("echoed payload must match request", reqPayload, echoed);
                        AtomicInteger counter = portHits.get(port);
                        assertTrue("response from unexpected port: " + port, counter != null);
                        counter.incrementAndGet();
                    }
                    r.succ = true;
                } catch (Throwable ex) {
                    r.succ = false;
                    r.ex = ex;
                    ex.printStackTrace();
                } finally {
                    latch.countDown();
                }
            }, "concurrent-caller-" + t).start();
        }
        // 200s upper bound; full run on a laptop usually finishes in a few seconds.
        boolean done = latch.await(200, TimeUnit.SECONDS);
        assertTrue("concurrent calls timed out before completion", done);

        for (int i = 0; i < results.size(); i++) {
            TestResult r = results.get(i);
            assertTrue("worker thread " + i + " failed: "
                    + (r.ex == null ? "<no exception>" : r.ex.toString()), r.succ);
        }

        // ---- final aggregate assertions ----
        int totalRequests = THREAD_COUNT * CYCLE_PER_THREAD;
        int sum = 0;
        Set<Integer> hitPorts = new HashSet<>();
        for (int i = 0; i < SERVER_COUNT; i++) {
            int port = BASE_TCP_PORT + i;
            int hits = portHits.get(port).get();
            sum += hits;
            if (hits > 0) {
                hitPorts.add(port);
            }
            // Random over 10 with 100000 trials → expected 10000/server.
            // Lower bound 2000 / upper bound 20000 leaves >>3-sigma headroom; CI-safe.
            assertTrue("port " + port + " never received a request", hits > 0);
            assertTrue("port " + port + " too few hits: " + hits, hits >= 2000);
            assertTrue("port " + port + " too many hits: " + hits, hits <= 20000);
        }
        assertEquals("total responses should equal total requests", totalRequests, sum);
        assertEquals("all 10 backend ports must be hit", SERVER_COUNT, hitPorts.size());
    }

    /**
     * Verifies the long-connection idle-recycle hand-off end-to-end under concurrency:
     * <ol>
     *     <li>configure {@code idleTimeout=10000} (10s) and {@code connsPerAddr=5} on the
     *         BackendConfig — each backend gets 5 long connections,</li>
     *     <li>fire round 1 with {@value #IDLE_THREAD_COUNT} threads × {@value #IDLE_CYCLE_PER_THREAD}
     *         requests each, so every slot gets warmed up to a live channel,</li>
     *     <li>snapshot the underlying netty channels for every cached transport,</li>
     *     <li>sleep 20s — well past idleTimeout — so READ_IDLE must fire on every channel,
     *         {@code IdleCloseHandler} must invalidate the slot and close the channel,</li>
     *     <li>assert every snapshotted channel has flipped to {@code !isConnected()},</li>
     *     <li>fire round 2 with the same concurrency profile — slots are blank placeholders so
     *         {@code ensureChannelActive} must rebuild fresh connections concurrently without
     *         a thundering-herd storm and every request must succeed,</li>
     *     <li>assert the post-second-round channels are entirely fresh identities not
     *         present in the original snapshot.</li>
     * </ol>
     *
     * <p>{@code connsPerAddr=5} gives 10 backends × 5 conns = 50 long connections. With
     * {@value #IDLE_THREAD_COUNT} concurrent threads firing requests, the test also
     * exercises the lock-internal double-check in
     * {@code AbstractClientTransport.ensureChannelActive}: after the idle handler invalidated
     * a slot, multiple threads may try to rebuild it simultaneously — the double-check
     * must collapse them onto exactly one physical reconnect per slot.</p>
     */
    @Test
    public void testIdleTimeoutChannelRecycle() throws Exception {
        // Build the namingUrl from the server farm started in {@link #before()}.
        StringBuilder urlBuilder = new StringBuilder("ip://");
        for (int i = 0; i < SERVER_COUNT; i++) {
            if (i > 0) {
                urlBuilder.append(',');
            }
            urlBuilder.append("127.0.0.1:").append(BASE_TCP_PORT + i);
        }

        BackendConfig backendConfig = new BackendConfig();
        DefResponseFutureManager.reset();
        backendConfig.setNamingUrl(urlBuilder.toString());
        backendConfig.setNetwork("tcp");
        // 5 long connections per backend ⇒ 10 × 5 = 50 channels in the cluster cache.
        backendConfig.setConnsPerAddr(IDLE_CONNS_PER_ADDR);
        backendConfig.setRequestTimeout(60_000);
        // 10s — comfortably above EventLoop scheduling jitter, well below the 20s wait below.
        backendConfig.setIdleTimeout(10_000);

        try {
            ConcurrentTestServiceApi proxy = backendConfig.getProxy(ConcurrentTestServiceApi.class);

            // ---- round 1: warm up every backend so each slot has a live long connection ----
            runConcurrentRequests(proxy, "warmup", IDLE_THREAD_COUNT, IDLE_CYCLE_PER_THREAD);

            // Snapshot every netty channel currently held in the cluster cache for this backend.
            // Expectation: exactly SERVER_COUNT × connsPerAddr live channels.
            Set<Channel> beforeIdleChannels = collectLiveChannels(backendConfig);
            int expectedConns = SERVER_COUNT * IDLE_CONNS_PER_ADDR;
            assertEquals("warm-up must produce exactly SERVER_COUNT × connsPerAddr live channels",
                    expectedConns, beforeIdleChannels.size());
            for (Channel ch : beforeIdleChannels) {
                assertTrue("warm-up channel must be live before sleep, ch=" + ch,
                        ch.isConnected());
            }

            // ---- sleep past idleTimeout: READ_IDLE must fire on every live channel ----
            // 20s = 2 × idleTimeout, leaves headroom for slow CI EventLoop scheduling.
            Thread.sleep(20_000);

            // ---- assert every snapshotted channel has been recycled by the idle handler ----
            // A best-effort drain wait: even after sleep, close() is async on the EventLoop;
            // we re-poll for up to 3s before giving up.
            long deadline = System.currentTimeMillis() + 3_000;
            while (System.currentTimeMillis() < deadline) {
                boolean allClosed = true;
                for (Channel ch : beforeIdleChannels) {
                    if (ch.isConnected()) {
                        allClosed = false;
                        break;
                    }
                }
                if (allClosed) {
                    break;
                }
                Thread.sleep(100);
            }
            for (Channel ch : beforeIdleChannels) {
                assertFalse("channel should have been closed by idle handler, but still active: "
                        + ch, ch.isConnected());
            }

            // ---- round 2: concurrent requests must all succeed via lazy reconnect ----
            // This also stresses the lock-internal double-check in ensureChannelActive: after
            // the idle handler invalidated every slot, IDLE_THREAD_COUNT threads compete to
            // rebuild them — the double-check must collapse them onto exactly one physical
            // reconnect per slot (no thundering-herd).
            runConcurrentRequests(proxy, "post-idle", IDLE_THREAD_COUNT, IDLE_CYCLE_PER_THREAD);

            // ---- the second round must have produced fresh channels distinct from the snapshot ----
            Set<Channel> afterReconnectChannels = collectLiveChannels(backendConfig);
            assertEquals("post-idle round must rebuild SERVER_COUNT × connsPerAddr live channels",
                    expectedConns, afterReconnectChannels.size());
            // Every post-idle channel must be a fresh identity — the original snapshot was
            // entirely closed by the idle handler so there should be zero overlap.
            IdentityHashMap<Channel, Boolean> before = new IdentityHashMap<>();
            for (Channel ch : beforeIdleChannels) {
                before.put(ch, Boolean.TRUE);
            }
            for (Channel ch : afterReconnectChannels) {
                assertFalse("post-idle channel must be a fresh identity, but matched a closed "
                        + "one from the warm-up snapshot: " + ch, before.containsKey(ch));
            }
        } finally {
            try {
                RpcClusterClientManager.shutdownBackendConfig(backendConfig);
            } catch (Throwable ignore) {
                // ignore
            }
        }
    }

    /**
     * Run {@code threads × cyclesPerThread} concurrent requests through {@code proxy}, asserting
     * the response payload is echoed correctly. The label is mixed into the payload so logs
     * and failures from different rounds are easy to tell apart.
     */
    private static void runConcurrentRequests(ConcurrentTestServiceApi proxy, String label,
            int threads, int cyclesPerThread) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(threads);
        List<TestResult> results = new ArrayList<>(threads);
        for (int t = 0; t < threads; t++) {
            final TestResult r = new TestResult();
            results.add(r);
            final int threadIndex = t;
            new Thread(() -> {
                try {
                    for (int i = 0; i < cyclesPerThread; i++) {
                        String reqPayload = label + "-" + threadIndex + "-" + i;
                        HelloResponse response = proxy.sayHello(new RpcClientContext(),
                                HelloRequest.newBuilder()
                                        .setMessage(ByteString.copyFromUtf8(reqPayload))
                                        .build());
                        String message = response.getMessage().toStringUtf8();
                        int sep = message.lastIndexOf("|port=");
                        if (sep <= 0 || !reqPayload.equals(message.substring(0, sep))) {
                            throw new AssertionError(
                                    "unexpected response payload, expected=" + reqPayload
                                            + ", got=" + message);
                        }
                    }
                    r.succ = true;
                } catch (Throwable ex) {
                    r.succ = false;
                    r.ex = ex;
                    ex.printStackTrace();
                } finally {
                    latch.countDown();
                }
            }, "idle-test-" + label + "-" + t).start();
        }
        boolean done = latch.await(120, TimeUnit.SECONDS);
        assertTrue("[" + label + "] concurrent calls timed out before completion", done);
        for (int i = 0; i < results.size(); i++) {
            TestResult r = results.get(i);
            assertTrue("[" + label + "] worker thread " + i + " failed: "
                    + (r.ex == null ? "<no exception>" : r.ex.toString()), r.succ);
        }
    }

    /**
     * Walk {@link RpcClusterClientManager}'s cache for {@code backendConfig}, drill down through
     * {@code RpcClientProxy → DefRpcClient → ClientTransport → channels[]} and return every
     * live {@code Channel} currently published in any slot.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Set<Channel> collectLiveChannels(BackendConfig backendConfig) throws Exception {
        Field clusterMapField = RpcClusterClientManager.class.getDeclaredField("CLUSTER_MAP");
        clusterMapField.setAccessible(true);
        Map<BackendConfig, Map<String, Object>> clusterMap =
                (Map<BackendConfig, Map<String, Object>>) clusterMapField.get(null);
        Map<String, Object> proxyMap = clusterMap.get(backendConfig);
        if (proxyMap == null) {
            return new HashSet<>();
        }
        Set<Channel> live = new HashSet<>();
        Field delegateField = null;
        for (Object proxy : proxyMap.values()) {
            if (delegateField == null) {
                delegateField = proxy.getClass().getDeclaredField("delegate");
                delegateField.setAccessible(true);
            }
            Object delegate = delegateField.get(proxy);
            if (delegate == null) {
                continue;
            }
            // DefRpcClient.transport (private final ClientTransport)
            Field transportField;
            try {
                transportField = delegate.getClass().getDeclaredField("transport");
            } catch (NoSuchFieldException ignore) {
                continue;
            }
            transportField.setAccessible(true);
            Object transport = transportField.get(delegate);
            if (!(transport instanceof ClientTransport)) {
                continue;
            }
            // AbstractClientTransport.channels (List<ChannelFutureItem>)
            Field channelsField;
            try {
                channelsField = AbstractClientTransport.class.getDeclaredField("channels");
            } catch (NoSuchFieldException ignore) {
                continue;
            }
            channelsField.setAccessible(true);
            List<?> slots = (List<?>) channelsField.get(transport);
            if (slots == null) {
                continue;
            }
            Field futureField = AbstractClientTransport.ChannelFutureItem.class
                    .getDeclaredField("channelFuture");
            futureField.setAccessible(true);
            for (Object slot : slots) {
                if (slot == null) {
                    continue;
                }
                Object cf = futureField.get(slot);
                if (cf == null) {
                    continue;
                }
                java.util.concurrent.CompletableFuture<Channel> future =
                        (java.util.concurrent.CompletableFuture<Channel>) cf;
                if (!future.isDone() || future.isCompletedExceptionally()) {
                    continue;
                }
                Channel ch = future.join();
                if (ch != null) {
                    live.add(ch);
                }
            }
        }
        return live;
    }

    private void startServers() {
        for (int i = 0; i < SERVER_COUNT; i++) {
            int port = BASE_TCP_PORT + i;
            ProviderConfig<ConcurrentTestService> providerConfig = new ProviderConfig<>();
            providerConfig.setRef(new PortAwareEchoServiceImpl(port));

            ServiceConfig serviceConfig = new ServiceConfig();
            serviceConfig.setIp("127.0.0.1");
            serviceConfig.setNetwork("tcp");
            serviceConfig.setPort(port);
            serviceConfig.setEnableLinkTimeout(true);
            // Generous server-side timeout to avoid spurious timeouts on slow CI.
            serviceConfig.setRequestTimeout(60_000);
            serviceConfig.addProviderConfig(providerConfig);
            serviceConfig.export();
            serviceConfigs.add(serviceConfig);
        }
    }

    /**
     * Service impl that tags every response with its own listening port so the test can verify
     * the actual server that handled each request.
     */
    private static class PortAwareEchoServiceImpl implements ConcurrentTestService {

        private final int port;

        PortAwareEchoServiceImpl(int port) {
            this.port = port;
        }

        @Override
        public HelloResponse sayHello(RpcServerContext context, HelloRequest request) {
            String echoed = request.getMessage().toStringUtf8();
            String tagged = echoed + "|port=" + port;
            return HelloResponse.newBuilder()
                    .setMessage(ByteString.copyFromUtf8(tagged))
                    .build();
        }
    }

    private static class TestResult {

        boolean succ;
        Throwable ex;
    }
}
