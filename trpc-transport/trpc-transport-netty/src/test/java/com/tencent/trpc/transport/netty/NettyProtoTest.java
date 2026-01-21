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

import com.tencent.trpc.core.common.config.ProtocolConfig;
import com.tencent.trpc.core.logger.Logger;
import com.tencent.trpc.core.logger.LoggerFactory;
import com.tencent.trpc.core.transport.ClientTransport;
import com.tencent.trpc.core.transport.ServerTransport;
import com.tencent.trpc.core.transport.handler.ChannelHandlerAdapter;
import com.tencent.trpc.core.utils.NetUtils;
import com.tencent.trpc.transport.netty.NettyTest.TestRequest;
import com.tencent.trpc.transport.netty.NettyTest.TestResponse;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test Netty Protocol
 */
public class NettyProtoTest extends AbstractNettyTest {

    public static final String MESSAGE = "hello";
    private static final Logger LOGGER = LoggerFactory.getLogger(NettyProtoTest.class);

    @BeforeEach
    public void before() {
    }

    @Test
    public void tcpTest() {
        tcpTest(true);
        tcpTest(false);
    }

    /**
     * Single tcp test function for the netty protocol.
     */
    public void tcpTest(boolean keepalive) {
        int serverPort = NetUtils.getAvailablePort(NetUtils.LOCAL_HOST, 18888);
        CountDownLatch latch = new CountDownLatch(2);
        ProtocolConfig serverConfig = new ProtocolConfig();
        final AtomicReference<String> serverReceive = new AtomicReference<String>();
        final AtomicReference<String> clientReceive = new AtomicReference<String>();
        serverConfig.setIp(NetUtils.LOCAL_HOST);
        serverConfig.setPort(serverPort);
        ServerTransport server =
                new NettyServerTransportFactory().create(serverConfig, new ChannelHandlerAdapter() {
                    @Override
                    public void received(com.tencent.trpc.core.transport.Channel channel,
                            Object message) {
                        serverReceive.set(((TestRequest) message).getBody());
                        latch.countDown();
                        channel
                                .send(new TestResponse((TestRequest) message,
                                        ((TestRequest) message).getBody()));
                    }
                }, new TransportServerCodecTest());
        server.open();

        ProtocolConfig clientConfig = new ProtocolConfig();
        clientConfig.setIp(NetUtils.LOCAL_HOST);
        clientConfig.setPort(serverPort);
        clientConfig.setIoThreadGroupShare(false);
        clientConfig.setKeepAlive(keepalive);
        clientConfig.setLazyinit(true);
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
        TestRequest request = new TestRequest(MESSAGE);
        client.send(request).whenComplete((r, t) -> {
            if (t != null) {
                t.printStackTrace();
            }
            System.out.println(r);
            System.out.println(t);
        });
        try {
            latch.await(100, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            LOGGER.error("error:", e);
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
            client.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
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
        ServerTransport server =
                new NettyServerTransportFactory().create(serverConfig, new ChannelHandlerAdapter() {
                    @Override
                    public void received(com.tencent.trpc.core.transport.Channel channel,
                            Object message) {
                        TestRequest req = ((TestRequest) message);
                        serverReceive.set(req.getBody());
                        latch.countDown();
                        TestResponse rsp = new TestResponse(req, req.getBody());
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
            LOGGER.error("error:", e);
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
}
