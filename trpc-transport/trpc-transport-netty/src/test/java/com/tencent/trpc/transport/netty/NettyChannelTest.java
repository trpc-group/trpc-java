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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import com.tencent.trpc.core.common.config.BaseProtocolConfig;
import com.tencent.trpc.core.common.config.ProtocolConfig;
import com.tencent.trpc.core.compressor.support.GZipCompressor;
import com.tencent.trpc.core.transport.Channel;
import com.tencent.trpc.core.transport.ChannelHandler;
import com.tencent.trpc.core.transport.codec.ServerCodec;
import com.tencent.trpc.core.transport.handler.ChannelHandlerAdapter;
import com.tencent.trpc.core.utils.FutureUtils;
import com.tencent.trpc.core.utils.NetUtils;
import com.tencent.trpc.transport.netty.exception.TRPCNettyBindException;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import io.netty.channel.ChannelPromise;
import org.checkerframework.checker.units.qual.N;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.api.support.membermodification.MemberModifier;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

/**
 * Test the potential leak issue during the conversion process between Netty and CompletableFuture
 */
@RunWith(PowerMockRunner.class)
@PowerMockIgnore("javax.management.*")
@PrepareForTest({NettyTcpServerTransport.class})
public class NettyChannelTest {


    @Test
    public void doSend() throws InterruptedException {
        io.netty.channel.Channel mockChannel = Mockito.mock(io.netty.channel.Channel.class);
        NettyChannel nettyChannel = new NettyChannel(mockChannel, null);
        String message = "Hello, Netty!";
        ChannelPromise promise = Mockito.mock(ChannelPromise.class);
        when(mockChannel.writeAndFlush(message)).thenReturn(promise);
        when(promise.sync()).thenReturn(promise);

        final CountDownLatch latch = new CountDownLatch(1);
        nettyChannel.doSend(message).thenAccept(v -> latch.countDown());
    }

    @Test
    public void equalTest() {
        NettyChannel nilConstruct = new NettyChannel();
        NettyChannel otherChannel = nilConstruct;
        nilConstruct.equals(null);
        nilConstruct.equals(otherChannel);
    }

    @Test
    public void ioChannel() {
        ProtocolConfig config = new ProtocolConfig();
        NettyChannel channel = new NettyChannel(null, config);
        Assert.assertNull(channel.getIoChannel());
    }
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
            ProtocolConfig config = PowerMockito.mock(ProtocolConfig.class);
            PowerMockito.when(config.getBossThreads()).thenReturn(1);
            PowerMockito.when(config.getReusePort()).thenReturn(true);

            ChannelHandler chan = PowerMockito.mock(ChannelHandler.class);
            ServerCodec serverCodec = PowerMockito.mock(ServerCodec.class);

            InetSocketAddress bindAddress = PowerMockito.mock(InetSocketAddress.class);
            PowerMockito.when(bindAddress.getPort()).thenReturn(8080);

            ServerBootstrap bootstrap = PowerMockito.mock(ServerBootstrap.class);

            ChannelFuture channelFuture = PowerMockito.mock(ChannelFuture.class);

            PowerMockito.when(channelFuture.isSuccess()).thenReturn(true);

            PowerMockito.when(bootstrap.bind(Mockito.any(InetSocketAddress.class))).thenReturn(channelFuture);

            PowerMockito.when(channelFuture.await()).thenReturn(channelFuture);

            NettyTcpServerTransport nettyTcpServerTransport = new NettyTcpServerTransport(config, chan, serverCodec);
            NettyTcpServerTransport mockTcpServerTransport = PowerMockito.spy(nettyTcpServerTransport);

            MemberModifier.field(NettyTcpServerTransport.class, "bindAddress")
                    .set(mockTcpServerTransport, bindAddress);

            MemberModifier.field(NettyTcpServerTransport.class, "bootstrap")
                    .set(mockTcpServerTransport, bootstrap);

            MemberModifier.field(NettyTcpServerTransport.class, "config")
                    .set(mockTcpServerTransport, config);

            PowerMockito.when(mockTcpServerTransport, "canMultiOccupyPort").thenReturn(true);

            ChannelFuture connect = Whitebox.invokeMethod(mockTcpServerTransport, "multiOccupyPort");
            Assert.assertNotNull(connect);

            PowerMockito.when(channelFuture.isSuccess()).thenReturn(false);

            ChannelFuture future = Whitebox.invokeMethod(mockTcpServerTransport, "multiOccupyPort");
            Assert.assertNotNull(future);
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testNettyTcpServerTransport_multiOccupyPortFail() {
        try {
            ProtocolConfig config = PowerMockito.mock(ProtocolConfig.class);
            PowerMockito.when(config.getBossThreads()).thenReturn(1);
            PowerMockito.when(config.getReusePort()).thenReturn(true);
            ChannelHandler chan = PowerMockito.mock(ChannelHandler.class);
            ServerCodec serverCodec = PowerMockito.mock(ServerCodec.class);
            InetSocketAddress bindAddress = PowerMockito.mock(InetSocketAddress.class);
            PowerMockito.when(bindAddress.getPort()).thenReturn(8080);
            ServerBootstrap bootstrap = PowerMockito.mock(ServerBootstrap.class);

            NettyTcpServerTransport nettyTcpServerTransport = new NettyTcpServerTransport(config, chan, serverCodec);
            NettyTcpServerTransport mockTcpServerTransport = PowerMockito.spy(nettyTcpServerTransport);

            MemberModifier.field(NettyTcpServerTransport.class, "bindAddress")
                    .set(mockTcpServerTransport, bindAddress);

            MemberModifier.field(NettyTcpServerTransport.class, "bootstrap")
                    .set(mockTcpServerTransport, bootstrap);

            MemberModifier.field(NettyTcpServerTransport.class, "config")
                    .set(mockTcpServerTransport, config);
            ChannelFuture connect = Whitebox.invokeMethod(mockTcpServerTransport, "multiOccupyPort");
            Assert.assertNull(connect);


        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testTRPCNettyBindException() {
        TRPCNettyBindException trpcNettyBindException = new TRPCNettyBindException("exception");
        Assert.assertNotNull(trpcNettyBindException);

        BaseProtocolConfig baseProtocolConfig = new BaseProtocolConfig();
        baseProtocolConfig.setBossThreads(1);
        baseProtocolConfig.getBossThreads();

        ProtocolConfig protocolConfig = new ProtocolConfig();
        protocolConfig.setIp("127.0.0.1");
        protocolConfig.setIoThreads(2);
        protocolConfig.setBossThreads(1);
        assertTrue(protocolConfig.getBossThreads() > 0);
    }


    @Test
    public void testTRPCNettyBindException_fail() {
        try {
            ProtocolConfig protocolConfig = new ProtocolConfig();
            protocolConfig.setIp("127.0.0.1");
            protocolConfig.setIoThreads(1);
            protocolConfig.setBossThreads(2);
        } catch (Exception e) {
            Assert.assertNotNull(e.getMessage());
        }
    }

    @Test
    public void testTRPCNettyBindException_failCpu() {
        try {
            ProtocolConfig protocolConfig = new ProtocolConfig();
            protocolConfig.setIp("127.0.0.1");
            protocolConfig.setBossThreads(999999);
        } catch (Exception e) {
            Assert.assertNotNull(e.getMessage());
        }
    }

}
