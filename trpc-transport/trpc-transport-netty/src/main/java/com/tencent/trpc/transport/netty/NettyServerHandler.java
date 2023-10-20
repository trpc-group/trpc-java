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
import com.tencent.trpc.core.transport.Channel;
import com.tencent.trpc.core.transport.ChannelHandler;
import com.tencent.trpc.core.utils.NetUtils;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import java.net.InetSocketAddress;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@io.netty.channel.ChannelHandler.Sharable
public class NettyServerHandler extends ChannelDuplexHandler {

    private static final Logger logger = LoggerFactory.getLogger(NettyServerHandler.class);

    /**
     * Client channel maps, and the key is the address of a client, like ip:port.
     */
    private final ConcurrentMap<String, Channel> clientChannels = new ConcurrentHashMap<>();
    private final ChannelHandler handler;
    private final ProtocolConfig config;
    private final boolean isTcp;

    public NettyServerHandler(ChannelHandler handler, ProtocolConfig config, boolean isTcp) {
        this.handler = handler;
        this.config = config;
        this.isTcp = isTcp;
    }

    private Channel removeClientChannel(ChannelHandlerContext ctx) {
        if (isTcp) {
            return clientChannels
                    .remove(NetUtils.toIpPort((InetSocketAddress) ctx.channel().remoteAddress()));
        }
        return null;
    }

    private Channel addClientChannel(ChannelHandlerContext ctx, NettyChannel channel) {
        if (channel != null && isTcp) {
            return clientChannels
                    .put(NetUtils.toIpPort((InetSocketAddress) ctx.channel().remoteAddress()),
                            channel);
        }
        return null;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        NettyChannel channel = NettyChannelManager.getOrAddChannel(ctx.channel(), config);
        try {
            addClientChannel(ctx, channel);
            handler.connected(channel);
        } finally {
            NettyChannelManager.removeChannelIfDisconnected(ctx.channel());
        }
    }


    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise)
            throws Exception {
        super.write(ctx, msg, promise);
        NettyChannel channel = NettyChannelManager.getOrAddChannel(ctx.channel(), config);
        try {
            handler.send(channel, msg);
        } finally {
            NettyChannelManager.removeChannelIfDisconnected(ctx.channel());
        }
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        try {
            NettyChannel nettyChannel = NettyChannelManager.getOrAddChannel(ctx.channel(), config);
            handler.received(nettyChannel, msg);
        } finally {
            NettyChannelManager.removeChannelIfDisconnected(ctx.channel());
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        NettyChannel channel = NettyChannelManager.getOrAddChannel(ctx.channel(), config);
        try {
            removeClientChannel(ctx);
            handler.disconnected(channel);
        } finally {
            NettyChannelManager.removeChannelIfDisconnected(ctx.channel());
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        try {
            NettyChannel channel = NettyChannelManager.getOrAddChannel(ctx.channel(), config);
            handler.caught(channel, cause);
            // only close the channel in a TCP scenario.
            if (ctx.channel().isActive() && isTcp) {
                logger.error("close channel:{}, the exception caught:", channel, cause);
                ctx.close();
            }
        } finally {
            NettyChannelManager.removeChannelIfDisconnected(ctx.channel());
        }
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            NettyChannel channel = NettyChannelManager.getOrAddChannel(ctx.channel(), config);
            try {
                IdleState state = ((IdleStateEvent) evt).state();
                logger.warn("idle event[{}] trigger, close channel:{}", state, channel);
                channel.close();
            } finally {
                NettyChannelManager.removeChannelIfDisconnected(ctx.channel());
            }
        }
        super.userEventTriggered(ctx, evt);
    }

    public ConcurrentMap<String, Channel> getChannels() {
        return clientChannels;
    }

}
