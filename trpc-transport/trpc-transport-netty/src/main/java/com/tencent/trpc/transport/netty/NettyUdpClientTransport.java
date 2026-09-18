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

import com.tencent.trpc.core.common.config.ProtocolConfig;
import com.tencent.trpc.core.transport.ChannelHandler;
import com.tencent.trpc.core.transport.codec.ClientCodec;
import com.tencent.trpc.core.utils.NetUtils;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.FixedRecvByteBufAllocator;
import io.netty.channel.epoll.EpollDatagramChannel;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.util.concurrent.DefaultThreadFactory;
import java.util.concurrent.CompletableFuture;

/**
 * A netty udp ClientTransport. Honours {@code ioMode=epoll} for the datagram path the
 * same way the TCP transport does, picking either {@link EpollDatagramChannel} +
 * {@link EpollEventLoopGroup} or {@link NioDatagramChannel} + {@link NioEventLoopGroup}.
 * The shared-group pool in {@link NettyAbstractClientTransport} is variant-aware, so
 * mixing {@code ioThreadGroupShare=true} with {@code ioMode=epoll} is now legal.
 *
 * <p><b>Long-connection scope on UDP.</b> UDP is connectionless: a datagram socket has no
 * notion of "half-dead" or "peer disconnected", and {@link io.netty.channel.Channel#isActive()}
 * stays {@code true} for the lifetime of the local socket. As a consequence, only part of
 * the long-connection hardening introduced for TCP applies here:</p>
 * <ul>
 *   <li><b>Applies (Layer 5):</b> the shared {@link EventLoopGroup} pool and the
 *       NIO/Epoll channel selection are real wins for UDP — fewer threads, native
 *       epoll datagram path on Linux.</li>
 *   <li><b>Does NOT apply (Layer 2 / 3 / 4):</b> the slot + {@code ensureChannelActive}
 *       reconnect machinery in {@link com.tencent.trpc.core.transport.AbstractClientTransport},
 *       the read-idle close handler, and the {@code TCP_KEEPIDLE / INTVL / CNT} tuning
 *       are all TCP-specific. For UDP they are either no-ops (the keepalive options are
 *       simply not set) or fast-path through (the slot is built once at
 *       {@code lazyinit} time and never invalidated, so {@code ensureChannelActive}
 *       always short-circuits at its first {@code !needsReconnect} check).</li>
 * </ul>
 * <p>The slot path being walked-but-never-firing on UDP is intentional: it keeps the
 * {@code AbstractClientTransport} contract uniform across protocols at a negligible
 * per-call overhead (one COW-list read + one boolean check). Readers should not assume
 * UDP enjoys the same half-dead detection or thundering-herd protection as TCP.</p>
 */
public class NettyUdpClientTransport extends NettyAbstractClientTransport {

    public NettyUdpClientTransport(ProtocolConfig config, ChannelHandler handler, ClientCodec clientCodec) {
        super(config, handler, clientCodec, "Netty-ShareUdpClientWorker");
    }

    @Override
    protected void doOpen() {
        final NettyClientHandler clientHandler =
                new NettyClientHandler(getChannelHandler(), config, false);
        this.bootstrap = new Bootstrap();
        boolean useEpoll = wantsEpoll(config);
        EventLoopGroup myEventLoopGroup;
        Class<? extends io.netty.channel.Channel> channelClass = useEpoll
                ? EpollDatagramChannel.class
                : NioDatagramChannel.class;
        if (Boolean.TRUE.equals(config.isIoThreadGroupShare())) {
            // Reference counter has already been incremented in the constructor; just use
            // the shared group of the matching variant here. This avoids any TOCTOU race
            // between constructor and doOpen.
            myEventLoopGroup = getSharedEventLoopGroup();
        } else if (useEpoll) {
            myEventLoopGroup = new EpollEventLoopGroup(config.getIoThreads(),
                    new DefaultThreadFactory(
                            "Netty-EpollUdpClientWorker-" + config.getIp() + ":" + config.getPort()));
        } else {
            myEventLoopGroup = new NioEventLoopGroup(config.getIoThreads(),
                    new DefaultThreadFactory(
                            "Netty-UdpClientWorker-" + config.getIp() + ":" + config.getPort()));
        }
        channelSet = clientHandler.getChannelSet();
        bootstrap.channel(channelClass).group(myEventLoopGroup)
                .option(ChannelOption.RCVBUF_ALLOCATOR,
                        new FixedRecvByteBufAllocator(config.getReceiveBuffer()))
                .handler(new ChannelInitializer<io.netty.channel.Channel>() {
                    @Override
                    protected void initChannel(io.netty.channel.Channel ch) throws Exception {
                        ChannelPipeline p = ch.pipeline();
                        if (codec == null) {
                            p.addLast("handler", clientHandler);
                        } else {
                            NettyCodecAdapter nettyCodec = NettyCodecAdapter
                                    .createUdpCodecAdapter(codec, config);
                            p.addLast("encode", nettyCodec.getEncoder())//
                                    .addLast("decode", nettyCodec.getDecoder())//
                                    .addLast("handler", clientHandler);
                        }
                    }
                });
    }

    @Override
    protected CompletableFuture<com.tencent.trpc.core.transport.Channel> make() {
        return NettyFutureUtils.fromConnectingFuture(bootstrap.bind(NetUtils.ANY_HOST, 0), config);
    }

    @Override
    protected boolean useChannelPool() {
        return true;
    }
}
