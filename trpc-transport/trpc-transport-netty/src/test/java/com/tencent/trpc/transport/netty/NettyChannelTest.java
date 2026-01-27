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

import com.google.common.collect.Lists;
import com.tencent.trpc.core.common.config.BaseProtocolConfig;
import com.tencent.trpc.core.common.config.ProtocolConfig;
import com.tencent.trpc.core.transport.Channel;
import com.tencent.trpc.core.transport.ChannelHandler;
import com.tencent.trpc.core.transport.codec.ServerCodec;
import com.tencent.trpc.core.transport.handler.ChannelHandlerAdapter;
import com.tencent.trpc.core.utils.FutureUtils;
import com.tencent.trpc.core.utils.NetUtils;
import com.tencent.trpc.transport.netty.exception.TRPCNettyBindException;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.mockito.Mockito;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Test the potential leak issue during the conversion process between Netty and CompletableFuture
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class NettyChannelTest {

    /**
     * Test whether the connection release is OK under concurrent scenarios.
     */
    @Test
    public void testTrpcNetty() throws Exception {
        ProtocolConfig serverConfig = newServerConfig(true);
        ProtocolConfig clientConfig = newClientConfig(true, false, serverConfig.getPort());
        ChannelHandlerAdapter clientChannelHandler = new ChannelHandlerAdapter();
        ChannelHandlerAdapter serverChannelHandler = new ChannelHandlerAdapter();
        NettyTcpClientTransport client = new NettyTcpClientTransport(clientConfig,
                clientChannelHandler, new TransportClientCodecTest());
        NettyTcpServerTransport server = new NettyTcpServerTransport(serverConfig,
                serverChannelHandler, new TransportServerCodecTest());
        try {
            server.open();
            client.open();
            int threads = 5;
            int times = 500;
            CountDownLatch latch = new CountDownLatch(threads);
            final AtomicLong created = new AtomicLong();
            final AtomicLong closed = new AtomicLong();
            for (int i = 0; i < threads; i++) {
                new Thread() {
                    public void run() {
                        int i = times;
                        try {
                            while (i-- > 0) {
                                CompletionStage<Channel> channel = client.getChannel();
                                created.incrementAndGet();
                                System.out.println(">>>>>>>>>>>>>> " + created);
                                try {
                                    if (!channel.toCompletableFuture().cancel(true)) {
                                        channel.toCompletableFuture().whenComplete((r, t) -> {
                                            r.close();
                                        });
                                    }
                                    closed.incrementAndGet();
                                } catch (Exception ex) {
                                    ex.printStackTrace();
                                }
                                try {
                                    Thread.sleep(10);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                        } finally {
                            latch.countDown();
                        }
                    }
                }.start();
            }
            latch.await();
            System.out.println("start sleep");
            Thread.sleep(5000);
            System.out.println(">>>>channle-size:" + client.getChannels().size());
            System.out.println(">>>>channle created size:" + created.get());
            System.out.println(">>>>channle closed size:" + closed.get());
            System.out.println(">>>>client connected cnt:" + clientChannelHandler.getConnectedCnt());
            System.out.println(">>>>client disconnected cnt:" + clientChannelHandler.getDisconnectedCnt());
            System.out.println(">>>>server connected cnt:" + serverChannelHandler.getConnectedCnt());
            System.out.println(">>>>server disconnected cnt:" + serverChannelHandler.getDisconnectedCnt());
            assertEquals(client.getChannels().size(), 0);
        } finally {
            server.close();
            client.close();
        }
    }

    private static ProtocolConfig newServerConfig(boolean isTcp) {
        ProtocolConfig config = new ProtocolConfig();
        config.setIp(NetUtils.LOCAL_HOST);
        config.setPort(NetUtils.getAvailablePort());
        config.setNetwork(isTcp ? "tcp" : "udp");
        return config;
    }

    private static ProtocolConfig newClientConfig(boolean isTcp, boolean isLazy, int port) {
        return newClientConfig(isTcp, isLazy, port, 4);
    }

    private static ProtocolConfig newClientConfig(boolean isTcp, boolean isLazy, int port,
            int perAddr) {
        ProtocolConfig config = new ProtocolConfig();
        config.setIp(NetUtils.LOCAL_HOST);
        config.setConnsPerAddr(perAddr);
        config.setLazyinit(isLazy);
        config.setNetwork(isTcp ? "tcp" : "udp");
        config.setPort(port);
        return config;
    }

    @Test
    public void testTcpConnectionClose1() throws InterruptedException {
        testConnectionClose0(true, true);
    }

    @Test
    public void testTcpConnectionClose2() {
        testConnectionClose0(true, false);
    }

    @Test
    public void testTcpConnectionClose3() {
        testConnectionClose0(false, true);
    }

    @Test
    public void testTcpConnectionClose4() {
        testConnectionClose0(false, false);
    }

    private void testConnectionClose0(boolean isTcp, boolean isLazy) {
        ProtocolConfig serverConfig = newServerConfig(isTcp);
        ProtocolConfig clientConfig = newClientConfig(isTcp, isLazy, serverConfig.getPort());
        NettyTcpClientTransport client = new NettyTcpClientTransport(clientConfig,
                new ChannelHandlerAdapter(), new TransportClientCodecTest());
        NettyTcpServerTransport server = new NettyTcpServerTransport(serverConfig,
                new ChannelHandlerAdapter(), new TransportServerCodecTest());
        try {
            server.open();
            client.open();
            int tryTime = 10;
            List<CompletionStage<Channel>> result = Lists.newArrayList();
            for (int i = 0; i < tryTime; i++) {
                result.add(client.getChannel());
            }
            sleep(100);
            assertEquals(client.getChannels().size(), 4);// ensure no more connections
            System.out.println(">>>>>>>>>>>>>>>.start check");
            client.close();
            sleep(100);
            assertEquals(client.getChannels().size(), 0);// ensure no leak connections
        } finally {
            server.close();
            client.close();
        }
    }

    @Test
    public void testConnectionClose0() throws Exception {
        ProtocolConfig serverConfig = newServerConfig(true);
        ProtocolConfig clientConfig = newClientConfig(true, true, serverConfig.getPort());
        NettyTcpClientTransport client = new NettyTcpClientTransport(clientConfig,
                new ChannelHandlerAdapter(), new TransportClientCodecTest());
        NettyTcpServerTransport server = new NettyTcpServerTransport(serverConfig,
                new ChannelHandlerAdapter(), new TransportServerCodecTest());
        try {
            server.open();
            client.open();
            ChannelFuture connect = client.getBootstrap()
                    .connect(serverConfig.toInetSocketAddress());
            connect.addListener(f -> {
                if (!f.isSuccess()) {
                    f.cause().printStackTrace();
                }
            });
            connect.await();
            assertEquals(connect.isSuccess(), true);
            CompletableFuture<com.tencent.trpc.core.transport.Channel> target = FutureUtils
                    .newFuture();
            target.cancel(true);
            NettyFutureUtils.adaptConnectingFuture(connect, target, clientConfig);
            sleep(10);
            assertEquals(connect.channel().isActive(), false);
        } finally {
            server.close();
            client.close();
        }
    }

    private void sleep(long m) {
        try {
            Thread.sleep(m);
        } catch (InterruptedException e1) {
            e1.printStackTrace();
        }
    }


    @Test
    public void testNettyTcpServerTransport_multiOccupyPort() {
        try {
            ProtocolConfig config = Mockito.mock(ProtocolConfig.class);
            Mockito.when(config.getBossThreads()).thenReturn(1);
            Mockito.when(config.getReusePort()).thenReturn(true);

            ChannelHandler chan = Mockito.mock(ChannelHandler.class);
            ServerCodec serverCodec = Mockito.mock(ServerCodec.class);

            InetSocketAddress bindAddress = Mockito.mock(InetSocketAddress.class);
            Mockito.when(bindAddress.getPort()).thenReturn(8080);

            ServerBootstrap bootstrap = Mockito.mock(ServerBootstrap.class);

            ChannelFuture channelFuture = Mockito.mock(ChannelFuture.class);

            Mockito.when(channelFuture.isSuccess()).thenReturn(true);

            Mockito.when(bootstrap.bind(Mockito.any(InetSocketAddress.class))).thenReturn(channelFuture);

            Mockito.when(channelFuture.await()).thenReturn(channelFuture);

            NettyTcpServerTransport nettyTcpServerTransport = new NettyTcpServerTransport(config, chan, serverCodec);
            NettyTcpServerTransport mockTcpServerTransport = Mockito.spy(nettyTcpServerTransport);

            setField(mockTcpServerTransport, "bindAddress", bindAddress);
            setField(mockTcpServerTransport, "bootstrap", bootstrap);
            setField(mockTcpServerTransport, "config", config);
            ChannelFuture connect = invokeMethod(mockTcpServerTransport, "multiOccupyPort");
            Assertions.assertNotNull(connect);

            Mockito.when(channelFuture.isSuccess()).thenReturn(false);

            ChannelFuture future = invokeMethod(mockTcpServerTransport, "multiOccupyPort");
            Assertions.assertNotNull(future);
        } catch (Exception e) {
            e.printStackTrace();
            Assertions.fail(e.getMessage());
        }
    }

    @Test
    public void testNettyTcpServerTransport_multiOccupyPortFail() {
        try {
            ProtocolConfig config = Mockito.mock(ProtocolConfig.class);
            Mockito.when(config.getBossThreads()).thenReturn(1);
            Mockito.when(config.getReusePort()).thenReturn(true);
            ChannelHandler chan = Mockito.mock(ChannelHandler.class);
            ServerCodec serverCodec = Mockito.mock(ServerCodec.class);
            InetSocketAddress bindAddress = Mockito.mock(InetSocketAddress.class);
            Mockito.when(bindAddress.getPort()).thenReturn(8080);
            ServerBootstrap bootstrap = Mockito.mock(ServerBootstrap.class);

            NettyTcpServerTransport nettyTcpServerTransport = new NettyTcpServerTransport(config, chan, serverCodec);
            NettyTcpServerTransport mockTcpServerTransport = Mockito.spy(nettyTcpServerTransport);

            setField(mockTcpServerTransport, "bindAddress", bindAddress);
            setField(mockTcpServerTransport, "bootstrap", bootstrap);
            setField(mockTcpServerTransport, "config", config);
            ChannelFuture connect = invokeMethod(mockTcpServerTransport, "multiOccupyPort");
            Assertions.assertNull(connect);


        } catch (Exception e) {
            Assertions.fail(e.getMessage());
        }
    }

    @Test
    public void testTRPCNettyBindException() {
        TRPCNettyBindException trpcNettyBindException = new TRPCNettyBindException("exception");
        Assertions.assertNotNull(trpcNettyBindException);

        BaseProtocolConfig baseProtocolConfig = new BaseProtocolConfig();
        baseProtocolConfig.setBossThreads(1);
        baseProtocolConfig.getBossThreads();

        ProtocolConfig protocolConfig = new ProtocolConfig();
        protocolConfig.setIp("127.0.0.1");
        protocolConfig.setIoThreads(2);
        protocolConfig.setBossThreads(1);
        Assertions.assertTrue(protocolConfig.getBossThreads() > 0);
    }


    @Test
    public void testTRPCNettyBindException_fail() {
        try {
            ProtocolConfig protocolConfig = new ProtocolConfig();
            protocolConfig.setIp("127.0.0.1");
            protocolConfig.setIoThreads(1);
            protocolConfig.setBossThreads(2);
        } catch (Exception e) {
            Assertions.assertNotNull(e.getMessage());
        }
    }

    @Test
    public void testTRPCNettyBindException_failCpu() {
        try {
            ProtocolConfig protocolConfig = new ProtocolConfig();
            protocolConfig.setIp("127.0.0.1");
            protocolConfig.setBossThreads(999999);
        } catch (Exception e) {
            Assertions.assertNotNull(e.getMessage());
        }
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Class<?> clazz = target.getClass();
        Field field = null;
        while (clazz != null && field == null) {
            try {
                field = clazz.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            }
        }
        if (field == null) {
            throw new NoSuchFieldException(fieldName);
        }
        field.setAccessible(true);
        field.set(target, value);
    }

    private <T> T invokeMethod(Object target, String methodName) throws Exception {
        Class<?> clazz = target.getClass();
        Method method = null;
        while (clazz != null && method == null) {
            try {
                method = clazz.getDeclaredMethod(methodName);
            } catch (NoSuchMethodException e) {
                clazz = clazz.getSuperclass();
            }
        }
        if (method == null) {
            throw new NoSuchMethodException(methodName);
        }
        method.setAccessible(true);
        return (T) method.invoke(target);
    }

}
