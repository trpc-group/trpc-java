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

package com.tencent.trpc.proto.http;

import static com.tencent.trpc.transport.http.common.Constants.HTTP_SCHEME;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.tencent.trpc.core.common.ConfigManager;
import com.tencent.trpc.core.common.config.BackendConfig;
import com.tencent.trpc.core.common.config.ConsumerConfig;
import com.tencent.trpc.core.common.config.ProviderConfig;
import com.tencent.trpc.core.common.config.ServerConfig;
import com.tencent.trpc.core.common.config.ServiceConfig;
import com.tencent.trpc.core.rpc.RpcClientContext;
import com.tencent.trpc.core.rpc.RpcContext;
import com.tencent.trpc.core.utils.NetUtils;
import com.tencent.trpc.proto.http.client.AbstractConsumerInvoker;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import tests.service.GreeterService;
import tests.service.HelloRequestProtocol.HelloRequest;
import tests.service.HelloRequestProtocol.HelloResponse;
import tests.service.TestBeanConvertWithGetMethodReq;
import tests.service.TestBeanConvertWithGetMethodRsp;

/**
 * HTTP-protocol counterpart of the tRPC concurrent multi-port test in {@code trpc-proto-standard}.
 *
 * <p>Setup: 10 standalone HTTP servers (jetty) on consecutive ports backed by
 * {@link PortAwareGreeterServiceImpl} that tags each response with its own listening port.
 * One shared {@link BackendConfig} lists all 10 endpoints in the comma-separated namingUrl;
 * 100 concurrent threads × 1000 requests each fan-out via {@code ip://} random load balance.</p>
 *
 * <p>Final assertions:
 * <ul>
 *     <li>every request succeeds and the echoed message matches the request payload,</li>
 *     <li>every backend port is hit at least once,</li>
 *     <li>distribution roughly balanced — random over N=10 with R=100000 trials,
 *         expected 10000 / port; tolerated range {@code [2000, 20000]}, far exceeding any
 *         realistic random outlier.</li>
 * </ul>
 * </p>
 */
public class HttpMultiPortNamingUrlConcurrentTest {

    private static final int BASE_PORT = 18500;
    private static final int SERVER_COUNT = 10;
    private static final int THREAD_COUNT = 100;
    private static final int CYCLE_PER_THREAD = 1000;

    private static final int REQUEST_TIMEOUT_MS = 60_000;
    private static final int MAX_CONNECTIONS = 20480;

    private static ServerConfig serverConfig;

    /**
     * Spin up {@value #SERVER_COUNT} HTTP providers on contiguous ports. Each provider returns
     * its own port number so the concurrent test can assert that requests are dispatched across
     * every endpoint resolved from the multi-port {@code ip://} naming URL.
     */
    @BeforeClass
    public static void startHttpServers() {
        ConfigManager.stopTest();
        ConfigManager.startTest();

        HashMap<String, ServiceConfig> providers = new HashMap<>();
        for (int i = 0; i < SERVER_COUNT; i++) {
            int port = BASE_PORT + i;
            ProviderConfig<GreeterService> pc = new ProviderConfig<>();
            pc.setServiceInterface(GreeterService.class);
            pc.setRef(new PortAwareGreeterServiceImpl(port));

            ServiceConfig serviceConfig = new ServiceConfig();
            serviceConfig.setName("multi-port-server-" + port);
            serviceConfig.getProviderConfigs().add(pc);
            serviceConfig.setIp(NetUtils.LOCAL_HOST);
            serviceConfig.setPort(port);
            serviceConfig.setProtocol(HTTP_SCHEME);
            serviceConfig.setTransporter("jetty");
            providers.put(serviceConfig.getName(), serviceConfig);
        }

        ServerConfig sc = new ServerConfig();
        sc.setServiceMap(providers);
        sc.setApp("http-multi-port-test");
        sc.setLocalIp(NetUtils.LOCAL_HOST);
        sc.init();
        serverConfig = sc;
    }

    @AfterClass
    public static void stopHttpServers() {
        if (serverConfig != null) {
            serverConfig.stop();
            serverConfig = null;
        }
        ConfigManager.stopTest();
    }

    /**
     * Reset the static {@code AbstractConsumerInvoker.TIMEOUT_MANAGER} so the {@code
     * HashedWheelTimer} is fresh — sibling tests in the same surefire run may have
     * stopped it via {@code ConfigManager.stopTest()}.
     */
    @Before
    public void resetTimeoutManager() {
        AbstractConsumerInvoker.reset();
    }

    @Test
    public void testHttpMultiPortNamingUrlConcurrent() throws InterruptedException {
        // Build "ip://127.0.0.1:p1,127.0.0.1:p2,...,127.0.0.1:p10".
        StringBuilder urlBuilder = new StringBuilder("ip://");
        for (int i = 0; i < SERVER_COUNT; i++) {
            if (i > 0) {
                urlBuilder.append(',');
            }
            urlBuilder.append(NetUtils.LOCAL_HOST).append(':').append(BASE_PORT + i);
        }
        String namingUrl = urlBuilder.toString();

        BackendConfig backendConfig = new BackendConfig();
        backendConfig.setName("http-multi-port-client");
        backendConfig.setNamingUrl(namingUrl);
        backendConfig.setProtocol("http");
        backendConfig.setRequestTimeout(REQUEST_TIMEOUT_MS);
        backendConfig.setMaxConns(MAX_CONNECTIONS);
        backendConfig.setConnsPerAddr(2);
        backendConfig.setKeepAlive(true);

        ConsumerConfig<GreeterService> consumerConfig = new ConsumerConfig<>();
        consumerConfig.setServiceInterface(GreeterService.class);
        consumerConfig.setBackendConfig(backendConfig);

        try {
            final GreeterService proxy = consumerConfig.getProxy();

            // Per-port hit counter aggregated across all worker threads.
            ConcurrentHashMap<Integer, AtomicInteger> portHits = new ConcurrentHashMap<>();
            for (int i = 0; i < SERVER_COUNT; i++) {
                portHits.put(BASE_PORT + i, new AtomicInteger(0));
            }

            CountDownLatch latch = new CountDownLatch(THREAD_COUNT);
            TestResult[] results = new TestResult[THREAD_COUNT];
            for (int t = 0; t < THREAD_COUNT; t++) {
                final TestResult r = new TestResult();
                results[t] = r;
                final int threadIndex = t;
                new Thread(() -> {
                    try {
                        for (int i = 0; i < CYCLE_PER_THREAD; i++) {
                            String reqPayload = "req-" + threadIndex + "-" + i;
                            RpcClientContext ctx = new RpcClientContext();
                            HelloResponse rsp = proxy.sayHello(ctx, HelloRequest.newBuilder()
                                    .setMessage(reqPayload)
                                    .build());
                            assertNotNull("response must not be null", rsp);
                            String message = rsp.getMessage();
                            // Server returns "<echoedPayload>|port=<actualPort>".
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
                }, "http-concurrent-caller-" + t).start();
            }

            // 300s upper bound for HTTP — slower per-request than tRPC due to HTTP framing.
            boolean done = latch.await(300, TimeUnit.SECONDS);
            assertTrue("concurrent calls timed out before completion", done);

            for (int i = 0; i < results.length; i++) {
                TestResult r = results[i];
                assertTrue("worker thread " + i + " failed: "
                        + (r.ex == null ? "<no exception>" : r.ex.toString()), r.succ);
            }

            // ---- aggregate assertions ----
            int totalRequests = THREAD_COUNT * CYCLE_PER_THREAD;
            int sum = 0;
            Set<Integer> hitPorts = new HashSet<>();
            for (int i = 0; i < SERVER_COUNT; i++) {
                int port = BASE_PORT + i;
                int hits = portHits.get(port).get();
                sum += hits;
                if (hits > 0) {
                    hitPorts.add(port);
                }
                // Random over 10 with 100000 trials → expected 10000/server.
                // [2000, 20000] leaves >>3-sigma headroom; CI-safe.
                assertTrue("port " + port + " never received a request", hits > 0);
                assertTrue("port " + port + " too few hits: " + hits, hits >= 2000);
                assertTrue("port " + port + " too many hits: " + hits, hits <= 20000);
            }
            assertEquals("total responses should equal total requests", totalRequests, sum);
            assertEquals("all 10 backend ports must be hit", SERVER_COUNT, hitPorts.size());
        } finally {
            backendConfig.stop();
        }
    }

    /**
     * Service impl that tags every response with its own listening port so the test can verify
     * the actual server that handled each request.
     */
    private static class PortAwareGreeterServiceImpl implements GreeterService {

        private final int port;

        PortAwareGreeterServiceImpl(int port) {
            this.port = port;
        }

        @Override
        public HelloResponse sayHello(RpcContext context, HelloRequest request) {
            String message = request.getMessage();
            return HelloResponse.newBuilder()
                    .setMessage(message + "|port=" + port)
                    .build();
        }

        @Override
        public String sayBlankHello(RpcContext context, HelloRequest request) {
            return "";
        }

        @Override
        public TestBeanConvertWithGetMethodRsp sayHelloNonPbType(RpcContext context,
                TestBeanConvertWithGetMethodReq request) {
            return new TestBeanConvertWithGetMethodRsp(request.getMessage(),
                    request.getStatus(), request.getComments());
        }
    }

    private static class TestResult {

        boolean succ;
        Throwable ex;
    }
}
