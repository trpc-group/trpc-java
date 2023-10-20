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

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import com.tencent.trpc.core.common.config.ProtocolConfig;
import com.tencent.trpc.core.exception.TransportException;
import com.tencent.trpc.core.logger.Logger;
import com.tencent.trpc.core.logger.LoggerFactory;
import com.tencent.trpc.core.transport.AbstractServerTransport;
import com.tencent.trpc.core.transport.Channel;
import com.tencent.trpc.core.transport.ChannelHandler;
import com.tencent.trpc.core.transport.codec.ServerCodec;
import com.tencent.trpc.core.transport.handler.AcceptLimitHandler;
import com.tencent.trpc.transport.netty.exception.TRPCNettyBindException;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.ServerChannel;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollChannelOption;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.flush.FlushConsolidationHandler;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.Version;
import io.netty.util.concurrent.DefaultThreadFactory;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

/**
 * A Netty Tcp Server, which doing the follow things:
 * <pre>
 * 1.set the package size
 * 2.connection management
 * 3.idle connection monitor
 * </pre>
 */
public class NettyTcpServerTransport extends AbstractServerTransport {

    private static final Logger logger = LoggerFactory.getLogger(NettyTcpServerTransport.class);

    private ServerBootstrap bootstrap;
    private ServerChannel serverChannel;
    private ConcurrentMap<String, Channel> clientChannels;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;

    public NettyTcpServerTransport(ProtocolConfig config, ChannelHandler channelHandler,
            ServerCodec serverCodec) throws TransportException {
        super(config, channelHandler, serverCodec);
    }

    @Override
    protected void doOpen() {
        Class<? extends ServerChannel> channelClass;
        boolean useEpoll = Epoll.isAvailable() && config.useEpoll();
        logger.debug("trpc config ioThreads:{} bossThreads:{} useEpoll:{}", config.getIoThreads(),
                config.getBossThreads(), useEpoll);
        if (useEpoll) {
            logger.info("NettyServer use EpollEventLoopGroup using epoll");
            bossGroup = new EpollEventLoopGroup(config.getBossThreads(),
                    new DefaultThreadFactory("Netty-Epoll-TcpServerBoss"));
            workerGroup = new EpollEventLoopGroup(config.getIoThreads(),
                    new DefaultThreadFactory("Netty-Epoll-TcpServerWorker"));
            channelClass = EpollServerSocketChannel.class;
        } else {
            bossGroup = new NioEventLoopGroup(config.getBossThreads(),
                    new DefaultThreadFactory("Netty-NIO-TcpServerBoss"));
            workerGroup = new NioEventLoopGroup(config.getIoThreads(),
                    new DefaultThreadFactory("Netty-NIO-TcpServerWorker"));
            channelClass = NioServerSocketChannel.class;
        }

        NettyServerHandler serverHandler =
                new NettyServerHandler(new AcceptLimitHandler(getChannelHandler(), this), config,
                        true);

        clientChannels = serverHandler.getChannels();
        bootstrap = new ServerBootstrap();
        bootstrap.group(bossGroup, workerGroup).channel(channelClass)
                .option(EpollChannelOption.SO_REUSEPORT, useEpoll && config.getReusePort())
                .option(ChannelOption.SO_REUSEADDR, Boolean.TRUE);

        if (config.getBacklog() > 0) {
            bootstrap.option(ChannelOption.SO_BACKLOG, config.getBacklog());
        }
        bootstrap.childOption(ChannelOption.SO_KEEPALIVE, true)
                .childOption(ChannelOption.TCP_NODELAY, true)
                .childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
        if (config.getReceiveBuffer() > 0) {
            bootstrap.childOption(ChannelOption.SO_RCVBUF, config.getReceiveBuffer());
        }
        if (config.getSendBuffer() > 0) {
            bootstrap.childOption(ChannelOption.SO_SNDBUF, config.getSendBuffer());
        }
        final boolean flushConsolidationSwitch = config.getFlushConsolidation();
        final Integer explicitFlushAfterFlushes = config.getExplicitFlushAfterFlushes();

        bootstrap.childHandler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) throws Exception {
                ChannelPipeline p = ch.pipeline();
                IdleStateHandler idleHandler =
                        new IdleStateHandler(0, 0, config.getIdleTimeout(), MILLISECONDS);
                if (codec == null) {
                    p.addLast("server-idle", idleHandler);
                } else {
                    NettyCodecAdapter nettyCodec = NettyCodecAdapter
                            .createTcpCodecAdapter(codec, config);
                    p.addLast("encode", nettyCodec.getEncoder())//
                            .addLast("decode", nettyCodec.getDecoder())//
                            .addLast("server-idle", idleHandler);
                }
                if (flushConsolidationSwitch) {
                    p.addLast("flushConsolidationHandlers",
                            new FlushConsolidationHandler(explicitFlushAfterFlushes, true));
                }
                p.addLast("handler", serverHandler);
            }
        });
        logger.info("Using Netty Version: {}", Version.identify().entrySet());
        ChannelFuture channelFuture = multiOccupyPort();
        channelFuture.syncUninterruptibly();
        serverChannel = (ServerChannel) channelFuture.channel();
    }

    /**
     * SO_REUSEPORT supports multiple processes or threads to bind to the same port,
     * improving the performance of server programs and solving the following problems:
     * <pre>
     * 1.Allows multiple sockets to bind()/listen() to the same TCP/UDP port.
     * 2.each thread has its own server socket.
     * 3.there is no lock contention on the server socket.
     * 4.load balancing is implemented at the kernel level.
     * 5.from a security perspective, sockets listening on the same port can only be under the same user.
     * 6.multicast occupies a port.
     * </pre>
     *
     * @return ChannelFuture
     */
    private ChannelFuture multiOccupyPort() {
        ChannelFuture channelFuture = null;
        if (canMultiOccupyPort()) {
            for (int i = 0; i < config.getBossThreads(); i++) {
                channelFuture = bindPortReturnChannelFuture(channelFuture);
            }
        } else {
            channelFuture = bindPortReturnChannelFuture(channelFuture);
        }
        return channelFuture;
    }

    private ChannelFuture bindPortReturnChannelFuture(ChannelFuture channelFuture) {
        try {
            channelFuture = bootstrap.bind(bindAddress).await();
            if (!channelFuture.isSuccess()) {
                throw new TRPCNettyBindException(
                        "epoll bind port bootstrap bind fail port is " + bindAddress.getPort());
            }
        } catch (Exception e) {
            logger.warn("trpc netty bind listen port fail:{}", e.getMessage(), e);
        }
        return channelFuture;
    }

    private boolean canMultiOccupyPort() {
        return Epoll.isAvailable() && config.useEpoll() && config.getReusePort();
    }

    @Override
    public boolean isBound() {
        return serverChannel.isOpen() && serverChannel.isActive();
    }

    @Override
    public void doClose() {
        try {
            closeServerChannel();
        } catch (Throwable e) {
            logger.error("Netty tcp server close server channel [" + getLocalAddress() + "] failed", e);
        }
        try {
            closeClientChannel();
        } catch (Throwable e) {
            logger.warn("Netty tcp server close client channels failed", e);
        }
        try {
            if (bossGroup != null) {
                bossGroup.shutdownGracefully();
            }
            if (workerGroup != null) {
                workerGroup.shutdownGracefully();
            }
        } catch (Throwable e) {
            logger.warn("Netty server EventLoopGroup shutdown failed", e);
        }
    }

    private void closeServerChannel() {
        if (serverChannel != null) {
            ChannelFuture future = serverChannel.close();
            future.addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) {
                    if (!future.isSuccess()) {
                        logger.warn("Netty server close server channel[" + getLocalAddress()
                                        + "] failed",
                                future.cause());
                    }
                }
            });
        }
    }

    @Override
    public Set<Channel> getChannels() {
        Set<Channel> channels = new HashSet<>();
        Iterator<Map.Entry<String, Channel>> iterator = clientChannels.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Channel> entry = iterator.next();
            Channel channel = entry.getValue();
            if (channel.isConnected()) {
                channels.add(channel);
            } else {
                iterator.remove();
            }
        }
        return channels;
    }

    private void closeClientChannel() {
        Set<Channel> channels = getChannels();
        for (Channel channel : channels) {
            try {
                channel.close();
            } catch (Throwable e) {
                logger.warn("Netty server close client channel[" + channel.getRemoteAddress()
                        + "] failed", e);
            }
        }
        clientChannels.clear();
    }
}
