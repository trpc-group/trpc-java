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
import com.tencent.trpc.core.transport.AbstractClientTransport;
import com.tencent.trpc.core.transport.Channel;
import com.tencent.trpc.core.transport.ChannelHandler;
import com.tencent.trpc.core.transport.codec.ClientCodec;
import com.tencent.trpc.core.utils.ConcurrentHashSet;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.concurrent.DefaultThreadFactory;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class NettyAbstractClientTransport extends AbstractClientTransport {

    private static final Object LOCK = new Object();

    /**
     * Hold the number of shared NioEventLoopGroup
     */
    protected static final AtomicInteger SHARE_EVENT_LOOP_GROUP_USED_NUMS = new AtomicInteger(0);
    /**
     * Shared NioEventLoopGroup
     */
    protected static volatile NioEventLoopGroup SHARE_EVENT_LOOP_GROUP;

    protected Bootstrap bootstrap;

    protected ConcurrentHashSet<Channel> channelSet = new ConcurrentHashSet<>();

    public NettyAbstractClientTransport(ProtocolConfig config, ChannelHandler handler,
            ClientCodec clientCodec, String defaultThreadPoolName) {
        super(config, handler, clientCodec);
        if (SHARE_EVENT_LOOP_GROUP == null) {
            synchronized (LOCK) {
                if (SHARE_EVENT_LOOP_GROUP == null) {
                    SHARE_EVENT_LOOP_GROUP = new NioEventLoopGroup(
                            config.getIoThreads(), new DefaultThreadFactory(defaultThreadPoolName)
                    );
                }
            }
        }
    }

    @Override
    protected void doClose() {
        if (bootstrap != null) {
            if (!config.isIoThreadGroupShare()) {
                bootstrap.config().group().shutdownGracefully();
            } else {
                closeShareEventLoopGroup();
            }
        }
    }

    @Override
    public Set<Channel> getChannels() {
        Set<Channel> channels = new HashSet<>();
        for (Channel each : channelSet) {
            if (each.isConnected()) {
                channels.add(each);
            }
        }
        return channels;
    }

    private void closeShareEventLoopGroup() {
        if (SHARE_EVENT_LOOP_GROUP_USED_NUMS.decrementAndGet() <= 0 && SHARE_EVENT_LOOP_GROUP != null) {
            synchronized (LOCK) {
                if (SHARE_EVENT_LOOP_GROUP_USED_NUMS.get() <= 0 && SHARE_EVENT_LOOP_GROUP != null) {
                    SHARE_EVENT_LOOP_GROUP.shutdownGracefully();
                    SHARE_EVENT_LOOP_GROUP = null;
                }
            }
        }
    }

    @Override
    public String toString() {
        return String.format("%s[remote:%s, isConnected=%b]", getClass().getName(), getRemoteAddress(), isConnected());
    }
}
