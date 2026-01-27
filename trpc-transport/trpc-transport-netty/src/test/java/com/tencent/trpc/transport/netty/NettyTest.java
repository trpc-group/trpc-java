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

package com.tencent.trpc.transport.netty;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.tencent.trpc.core.common.config.ProtocolConfig;
import com.tencent.trpc.core.rpc.AbstractRequest;
import com.tencent.trpc.core.rpc.AbstractResponse;
import com.tencent.trpc.core.rpc.Request;
import com.tencent.trpc.core.transport.ClientTransport;
import com.tencent.trpc.core.transport.ServerTransport;
import com.tencent.trpc.core.transport.handler.ChannelHandlerAdapter;
import com.tencent.trpc.core.utils.NetUtils;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test Netty Transport
 */
public class NettyTest extends AbstractNettyTest {

    public static final String MESSAGE = "hello";
    ServerTransport server;
    ClientTransport client;

    @BeforeEach
    public void before() {

    }

    @AfterEach
    public void after() {
        if (server != null) {
            server.close();
        }
        if (client != null) {
            client.close();
        }
        try {
            Thread.sleep(1);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void tcpMaxConnections() {
        int serverPort = NetUtils.getAvailablePort(NetUtils.LOCAL_HOST, 18888);
        ProtocolConfig serverConfig = new ProtocolConfig();
        serverConfig.setIp(NetUtils.LOCAL_HOST);
        serverConfig.setPort(serverPort);
        serverConfig.setNetwork("tcp");
        serverConfig.setMaxConns(2);
        serverConfig.setDefault();
        server = new NettyServerTransportFactory()
                .create(serverConfig, new ChannelHandlerAdapter() {
                    @Override
                    public void received(com.tencent.trpc.core.transport.Channel channel,
                            Object message) {
                    }
                }, new TransportServerCodecTest());
        server.open();

        ProtocolConfig clientConfig = new ProtocolConfig();
        clientConfig.setIp(NetUtils.LOCAL_HOST);
        clientConfig.setPort(serverPort);
        clientConfig.setNetwork("tcp");
        client = new NettyClientTransportFactory()
                .create(clientConfig, new ChannelHandlerAdapter() {
                    @Override
                    public void received(com.tencent.trpc.core.transport.Channel channel,
                            Object message) {
                    }
                }, new TransportClientCodecTest());
        client.open();
        for (int i = 0; i < 10; i++) {
            client.getChannel().toCompletableFuture().join();
        }
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        assertEquals(client.getChannels().size(), 2);
    }

    @Test
    public void udpTest() {
        int serverPort = NetUtils.getAvailablePort(NetUtils.LOCAL_HOST, 18888);
        CountDownLatch latch = new CountDownLatch(2);
        ProtocolConfig serverConfig = new ProtocolConfig();
        final AtomicReference<String> serverReceive = new AtomicReference<String>();
        final AtomicReference<String> clientReceive = new AtomicReference<String>();
        serverConfig.setIp(NetUtils.LOCAL_HOST);
        serverConfig.setPort(serverPort);
        serverConfig.setNetwork("udp");
        serverConfig.setDefault();
        ServerTransport server =
                new NettyServerTransportFactory().create(serverConfig, new ChannelHandlerAdapter() {
                    @Override
                    public void received(com.tencent.trpc.core.transport.Channel channel,
                            Object message) {
                        TestRequest req = ((TestRequest) message);
                        serverReceive.set(req.getBody());
                        latch.countDown();
                        TestResponse rsp = new TestResponse(req, req.getBody());
                        // rsp.setRemoteAddress(req.getRemoteAddress());
                        channel.send(rsp);
                    }
                }, new TransportServerCodecTest());
        server.open();

        ProtocolConfig clientConfig = new ProtocolConfig();
        clientConfig.setIp(NetUtils.LOCAL_HOST);
        clientConfig.setPort(serverPort);
        clientConfig.setNetwork("udp");
        ClientTransport client =
                new NettyClientTransportFactory().create(clientConfig, new ChannelHandlerAdapter() {
                    @Override
                    public void received(com.tencent.trpc.core.transport.Channel channel,
                            Object message) {
                        clientReceive.set(((TestResponse) message).getBody());
                        latch.countDown();
                    }
                }, new TransportClientCodecTest());
        client.open();
        TestRequest requset = new TestRequest(MESSAGE);
        requset.getMeta().setRemoteAddress(serverConfig.toInetSocketAddress());
        client.send(requset).whenComplete((r, t) -> {
            if (t != null) {
                t.printStackTrace();
            }
            System.out.println(r);
            System.out.println(t);
        });
        try {
            latch.await(1000, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Assertions.assertEquals(serverReceive.get(), MESSAGE);
        Assertions.assertEquals(clientReceive.get(), MESSAGE);
        try {
            server.toString();
            server.getChannels();
            server.isBound();
            server.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        try {
            client.toString();
            client.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static class TestRequest extends AbstractRequest {

        private String body;

        public TestRequest(String body) {
            super();
            this.body = body;
        }

        public String getBody() {
            return body;
        }

        public void setBody(String body) {
            this.body = body;
        }
    }

    public static class TestResponse extends AbstractResponse {

        private String body;
        private Request request;

        public TestResponse() {

        }

        public TestResponse(Request request, String body) {
            super();
            this.request = request;
            this.body = body;
        }

        public String getBody() {
            return body;
        }

        public void setBody(String body) {
            this.body = body;
        }

        @Override
        public long getRequestId() {
            return 0;
        }

        @Override
        public Request getRequest() {
            return request;
        }

        @Override
        public void setRequest(Request request) {

        }

    }
}
