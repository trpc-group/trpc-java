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

package com.tencent.trpc.integration.test.transport;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import com.tencent.trpc.core.rpc.RpcClientContext;
import com.tencent.trpc.integration.test.TrpcServerApplication;
import com.tencent.trpc.integration.test.stub.EchoAPI;
import com.tencent.trpc.integration.test.stub.EchoService.DelayedEchoRequest;
import com.tencent.trpc.integration.test.stub.EchoService.EchoRequest;
import com.tencent.trpc.integration.test.stub.EchoService.EchoResponse;
import com.tencent.trpc.spring.annotation.TRpcClient;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Transport related integration tests. Related configuration file: <code>application-transport.yml</code>
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = TrpcServerApplication.class)
@ActiveProfiles("transport")
public class TransportIntegrationTest {

    private final ExecutorService executor = Executors.newFixedThreadPool(1);
    @TRpcClient(id = "tcp-client")
    private EchoAPI tcpEchoAPI;
    @TRpcClient(id = "udp-client")
    private EchoAPI udpEchoAPI;
    @TRpcClient(id = "tcp-client-udp")
    private EchoAPI tcpToUdpEchoAPI;
    @TRpcClient(id = "udp-client-tcp")
    private EchoAPI udpToTcpEchoAPI;
    @TRpcClient(id = "tcp-client2")
    private EchoAPI tcpEchoAPI2;
    @TRpcClient(id = "http-client-7072")
    private EchoAPI http7072EchoAPI;
    @TRpcClient(id = "http-client-7073")
    private EchoAPI http7073EchoAPI;

    /**
     * Test for TCP transport
     */
    @Test
    public void testTcpNettyTransport() {
        String message = "tcp-hello";
        EchoResponse response = tcpEchoAPI.echo(new RpcClientContext(), EchoRequest.newBuilder()
                .setMessage(message)
                .build());
        assertEquals(message, response.getMessage());
    }

    /**
     * Test for UDP transport
     */
    @Test
    public void testUdpNettyTransport() {
        String message = "udp-hello";
        EchoResponse response = udpEchoAPI.echo(new RpcClientContext(), EchoRequest.newBuilder()
                .setMessage(message)
                .build());
        assertEquals(message, response.getMessage());
    }

    /**
     * Test for TCP client against UDP service
     */
    @Test
    public void testTcpToUdpNettyTransport() {
        String message = "tcp-hello";
        assertThrows(RuntimeException.class, () ->
                tcpToUdpEchoAPI.echo(new RpcClientContext(), EchoRequest.newBuilder()
                        .setMessage(message)
                        .build()));
    }

    /**
     * Test for UDP client against TCP service
     */
    @Test
    public void testUdpToTcpNettyTransport() {
        String message = "udp-hello";
        assertThrows(RuntimeException.class, () ->
                udpToTcpEchoAPI.echo(new RpcClientContext(), EchoRequest.newBuilder()
                        .setMessage(message)
                        .build()));
    }

    /**
     * Test for server-side idle-timeout
     */
    @Test
    public void testIdleTimeout() {
        assertThrows(RuntimeException.class, () ->
                tcpEchoAPI.delayedEcho(new RpcClientContext(), DelayedEchoRequest.newBuilder()
                        .setMessage("timeout")
                        .setDelaySeconds(2)
                        .build()));
    }

    /**
     * Test for server-side max connection
     */
    @Test
    public void testMaxConnection() throws InterruptedException {
        executor.submit(() -> tcpEchoAPI.delayedEcho(new RpcClientContext(), DelayedEchoRequest.newBuilder()
                .setMessage("timeout")
                .setDelaySeconds(1)
                .build()));
        Thread.sleep(100);
        assertThrows(RuntimeException.class, () -> tcpEchoAPI2.delayedEcho(new RpcClientContext(),
                DelayedEchoRequest.newBuilder()
                        .setMessage("timeout")
                        .setDelaySeconds(1)
                        .build()));
    }

    /**
     * Test for multiple ports HTTP server
     */
    @Test
    public void testHttpService() {
        String message = "http-hello";
        EchoResponse response = http7072EchoAPI.echo(new RpcClientContext(), EchoRequest.newBuilder()
                .setMessage(message)
                .build());
        assertEquals(message, response.getMessage());
        response = http7073EchoAPI.echo(new RpcClientContext(), EchoRequest.newBuilder()
                .setMessage(message)
                .build());
        assertEquals(message, response.getMessage());
    }

    /**
     * Test for request timeout of HTTP transport
     */
    @Test
    public void testHttpRequestTimeout() {
        assertThrows(RuntimeException.class, () -> http7072EchoAPI.delayedEcho(new RpcClientContext(),
                DelayedEchoRequest.newBuilder()
                        .setMessage("timeout")
                        .setDelaySeconds(6)
                        .build()));
    }
}
