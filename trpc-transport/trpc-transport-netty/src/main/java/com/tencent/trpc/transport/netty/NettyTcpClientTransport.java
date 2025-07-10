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

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import com.tencent.trpc.core.common.config.ProtocolConfig;
import com.tencent.trpc.core.logger.Logger;
import com.tencent.trpc.core.logger.LoggerFactory;
import com.tencent.trpc.core.transport.ChannelHandler;
import com.tencent.trpc.core.transport.codec.ClientCodec;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.DefaultThreadFactory;

import java.util.concurrent.CompletableFuture;

/**
 * A netty tcp ClientTransport
 */
public class NettyTcpClientTransport extends NettyAbstractClientTransport {

    public NettyTcpClientTransport(ProtocolConfig config, ChannelHandler handler, ClientCodec clientCodec) {
        super(config, handler, clientCodec, "Netty-ShareTcpClientWorker");
    }

    @Override
    protected void doOpen() {
        bootstrap = new Bootstrap();
        NioEventLoopGroup myEventLoopGroup;
        if (!config.isIoThreadGroupShare()) {
            myEventLoopGroup = new NioEventLoopGroup(config.getIoThreads(),
                    new DefaultThreadFactory(
                            "Netty-TcpClientWorker-" + config.getIp() + ":" + config.getPort()));
        } else {
            myEventLoopGroup = SHARE_EVENT_LOOP_GROUP;
            SHARE_EVENT_LOOP_GROUP_USED_NUMS.incrementAndGet();
        }
        bootstrap.group(myEventLoopGroup).channel(NioSocketChannel.class)
                .option(ChannelOption.SO_KEEPALIVE, true).option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, config.getConnTimeout());
        if (config.getReceiveBuffer() > 0) {
            bootstrap.option(ChannelOption.SO_RCVBUF, config.getReceiveBuffer());
        }
        if (config.getSendBuffer() > 0) {
            bootstrap.option(ChannelOption.SO_SNDBUF, config.getSendBuffer());
        }
        final NettyClientHandler clientHandler =
                new NettyClientHandler(getChannelHandler(), config, true);
        channelSet = clientHandler.getChannelSet();
        bootstrap.handler(new ChannelInitializer<NioSocketChannel>() {
            @Override
            protected void initChannel(NioSocketChannel ch) {

                IdleStateHandler clientIdleHandler =
                        new IdleStateHandler(0, config.getIdleTimeout(), 0, MILLISECONDS);
                ChannelPipeline p = ch.pipeline();
                if (codec == null) {
                    p.addLast("client-idle", clientIdleHandler).addLast("handler", clientHandler);
                } else {
                    NettyCodecAdapter nettyCodec = NettyCodecAdapter
                            .createTcpCodecAdapter(codec, config);
                    p.addLast("encode", nettyCodec.getEncoder())
                            .addLast("decode", nettyCodec.getDecoder())
                            .addLast("client-idle", clientIdleHandler)
                            .addLast("handler", clientHandler);
                }
            }
        });
    }

    @Override
    protected CompletableFuture<com.tencent.trpc.core.transport.Channel> make() {
        return NettyFutureUtils.fromConnectingFuture(bootstrap.connect(getRemoteAddress()), config);
    }

    public Bootstrap getBootstrap() {
        return bootstrap;
    }

    @Override
    protected boolean useChannelPool() {
        return config.isKeepAlive();
    }
}
