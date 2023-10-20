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
import com.tencent.trpc.core.utils.ConcurrentHashSet;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;

@io.netty.channel.ChannelHandler.Sharable
public class NettyClientHandler extends ChannelDuplexHandler {

    private static final Logger logger = LoggerFactory.getLogger(NettyClientHandler.class);

    private final ConcurrentHashSet<Channel> channelSet = new ConcurrentHashSet<>();
    private final ChannelHandler handler;
    private final ProtocolConfig config;
    private final boolean isTcp;

    public NettyClientHandler(ChannelHandler handler, ProtocolConfig config, boolean isTcp) {
        this.handler = handler;
        this.isTcp = isTcp;
        this.config = config;
    }

    public static boolean isValid(ChannelHandlerContext ctx) {
        return ctx != null && ctx.channel() != null && (ctx.channel().isActive());
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        NettyChannel channel = NettyChannelManager.getOrAddChannel(ctx.channel(), config);
        try {
            channelSet.add(new NettyChannel(ctx.channel(), config));
            handler.connected(channel);
        } finally {
            NettyChannelManager.removeChannelIfDisconnected(ctx.channel());
        }
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        try {
            NettyChannel nettyChannel = NettyChannelManager.getOrAddChannel(ctx.channel(), config);
            handler.received(nettyChannel, msg);
            // closing connections in a short connection scenario.
            if (!config.isKeepAlive() && isValid(ctx) && isTcp) {
                ctx.close();
            }
        } finally {
            NettyChannelManager.removeChannelIfDisconnected(ctx.channel());
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        try {
            NettyChannel channel = NettyChannelManager.getOrAddChannel(ctx.channel(), config);
            handler.caught(channel, cause);
            // only close the channel in a TCP scenario.
            if (isValid(ctx) && isTcp) {
                ctx.close();
            }
        } finally {
            NettyChannelManager.removeChannelIfDisconnected(ctx.channel());
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        NettyChannel channel = NettyChannelManager.getOrAddChannel(ctx.channel(), config);
        try {
            channelSet.remove(new NettyChannel(ctx.channel(), config));
            handler.disconnected(channel);
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
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            NettyChannel channel = NettyChannelManager.getOrAddChannel(ctx.channel(), config);
            try {
                // only close the channel in a TCP scenario.
                if (isTcp) {
                    IdleState state = ((IdleStateEvent) evt).state();
                    logger.warn("Idle event(state=" + state + ") trigger, close channel" + channel);
                    channel.close();
                }
            } finally {
                NettyChannelManager.removeChannelIfDisconnected(ctx.channel());
            }
        }
        super.userEventTriggered(ctx, evt);
    }

    public ConcurrentHashSet<Channel> getChannelSet() {
        return channelSet;
    }
}
