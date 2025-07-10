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

package com.tencent.trpc.transport.netty;

import com.google.common.collect.Lists;
import com.tencent.trpc.core.common.config.ProtocolConfig;
import com.tencent.trpc.core.exception.TransportException;
import com.tencent.trpc.core.logger.Logger;
import com.tencent.trpc.core.logger.LoggerFactory;
import com.tencent.trpc.core.transport.AbstractServerTransport;
import com.tencent.trpc.core.transport.Channel;
import com.tencent.trpc.core.transport.ChannelHandler;
import com.tencent.trpc.core.transport.codec.ServerCodec;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.FixedRecvByteBufAllocator;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollChannelOption;
import io.netty.channel.epoll.EpollDatagramChannel;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.util.concurrent.DefaultThreadFactory;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * A netty udp ServerTransport
 */
public class NettyUdpServerTransport extends AbstractServerTransport {

    private static final Logger LOG = LoggerFactory.getLogger(NettyTcpServerTransport.class);
    protected Bootstrap bootstrap;
    private EventLoopGroup workerGroup;
    private List<DatagramChannel> channelList =
            Lists.newArrayListWithExpectedSize(Runtime.getRuntime().availableProcessors());

    public NettyUdpServerTransport(ProtocolConfig config, ChannelHandler channelHandler,
            ServerCodec serverCodec) throws TransportException {
        super(config, channelHandler, serverCodec);
    }

    @Override
    protected void doOpen() throws TransportException {
        Class<? extends DatagramChannel> channelClass;
        bootstrap = new Bootstrap();
        boolean useEpoll = Epoll.isAvailable() && config.useEpoll();
        if (useEpoll) {
            workerGroup = new EpollEventLoopGroup(config.getIoThreads(),
                    new DefaultThreadFactory("Netty-UdpServerWorker"));
            channelClass = EpollDatagramChannel.class;
        } else {
            workerGroup = new NioEventLoopGroup(config.getIoThreads(),
                    new DefaultThreadFactory("Netty-UdpServerWorker"));
            channelClass = NioDatagramChannel.class;
        }
        bootstrap.channel(channelClass).group(workerGroup)
                .option(EpollChannelOption.SO_REUSEPORT, useEpoll)
                .option(ChannelOption.SO_REUSEADDR, true)
                .option(ChannelOption.RCVBUF_ALLOCATOR, new FixedRecvByteBufAllocator(config.getReceiveBuffer()));
        NettyServerHandler handler = new NettyServerHandler(getChannelHandler(), config, false);
        bootstrap.handler(new ChannelInitializer<io.netty.channel.Channel>() {
            protected void initChannel(io.netty.channel.Channel ch) throws Exception {
                ChannelPipeline p = ch.pipeline();
                if (codec != null) {
                    NettyCodecAdapter nettyCodec = NettyCodecAdapter
                            .createUdpCodecAdapter(codec, config);
                    p.addLast("encode", nettyCodec.getEncoder());
                    p.addLast("decode", nettyCodec.getDecoder());
                }
                p.addLast("handler", handler);
            }

            ;
        });
        int bindNums = (useEpoll ? config.getIoThreads() : 1);
        // because there is only one thread to handle encoding and decoding in UDP implementation,
        // here we use SO_REUSEPORT to improve UDP performance.
        for (int i = 0; i < bindNums; ++i) {
            ChannelFuture future = bootstrap.bind(bindAddress);
            try {
                future.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            if (!future.isSuccess()) {
                throw new TransportException(
                        "Server bind failed, addr(ip=" + bindAddress + ") may been binded ",
                        future.cause());
            }
            channelList.add((DatagramChannel) future.channel());
        }
    }

    @Override
    public ChannelHandler getChannelHandler() {
        return channelHandler;
    }

    @Override
    public boolean isBound() {
        for (DatagramChannel each : channelList) {
            if (each.isActive()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Set<Channel> getChannels() {
        return Collections.emptySet();
    }

    @Override
    protected void doClose() {
        try {
            closeServerChannel();
        } catch (Throwable e) {
            LOG.error("netty udp server close local channel [" + getLocalAddress() + "] failed", e);
        }
        try {
            if (workerGroup != null) {
                workerGroup.shutdownGracefully();
            }
        } catch (Throwable e) {
            LOG.warn("netty udp server eventLoopGroup shutdown failed", e);
        }
    }

    private void closeServerChannel() {
        for (DatagramChannel each : channelList) {
            try {
                ChannelFuture future = each.close();
                future.addListener((ChannelFutureListener) future1 -> {
                    if (!future1.isSuccess()) {
                        LOG.warn("netty udp server close local channel [" + getLocalAddress()
                                        + "] failed",
                                future1.cause());
                    }
                });
            } catch (Throwable ex) {
                LOG.warn("netty udp server close local channel [" + getLocalAddress() + "] failed",
                        ex);
            }
        }
    }
}
