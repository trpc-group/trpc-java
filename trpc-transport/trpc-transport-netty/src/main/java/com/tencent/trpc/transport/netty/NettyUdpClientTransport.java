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
import com.tencent.trpc.core.transport.ChannelHandler;
import com.tencent.trpc.core.transport.codec.ClientCodec;
import com.tencent.trpc.core.utils.NetUtils;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.FixedRecvByteBufAllocator;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.util.concurrent.DefaultThreadFactory;
import java.util.concurrent.CompletableFuture;

/**
 * A netty udp ClientTransport
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
        NioEventLoopGroup myEventLoopGroup;
        if (!config.isIoThreadGroupShare()) {
            myEventLoopGroup = new NioEventLoopGroup(config.getIoThreads(),
                    new DefaultThreadFactory(
                            "Netty-UdpClientWorker-" + config.getIp() + ":" + config.getPort()));
        } else {
            myEventLoopGroup = SHARE_EVENT_LOOP_GROUP;
            SHARE_EVENT_LOOP_GROUP_USED_NUMS.incrementAndGet();
        }
        channelSet = clientHandler.getChannelSet();
        bootstrap.channel(NioDatagramChannel.class).group(myEventLoopGroup)
                .option(ChannelOption.RCVBUF_ALLOCATOR,
                        new FixedRecvByteBufAllocator(config.getReceiveBuffer()))
                .handler(new ChannelInitializer<NioDatagramChannel>() {
                    @Override
                    protected void initChannel(NioDatagramChannel ch) throws Exception {
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
