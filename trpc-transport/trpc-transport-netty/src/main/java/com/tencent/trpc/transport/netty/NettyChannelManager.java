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
import com.tencent.trpc.core.logger.Logger;
import com.tencent.trpc.core.logger.LoggerFactory;
import io.netty.channel.Channel;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Using space to save time, avoiding unnecessary creation of NettyChannel objects.
 *
 * <p>Note: In a UDP scenario, the channel obtained is the server-side channel. In a TCP scenario,
 * the channel obtained is the channel established with the client.</p>
 */
public class NettyChannelManager {

    private static final Logger logger = LoggerFactory.getLogger(NettyChannelManager.class);

    private static final ConcurrentMap<Channel, NettyChannel> CHANNEL_MAP =
            new ConcurrentHashMap<>();

    /**
     * Get a cashed NettyChannel by the {@link ProtocolConfig}, create if not exists.
     *
     * @param ioChannel the Netty's {@link Channel}
     * @param config the protocol config
     * @return a NettyChannel
     */
    public static NettyChannel getOrAddChannel(Channel ioChannel, ProtocolConfig config) {
        if (ioChannel == null) {
            return null;
        }
        NettyChannel channel = CHANNEL_MAP.get(ioChannel);
        if (channel == null) {
            NettyChannel nettyChannel = new NettyChannel(ioChannel, config);
            channel = CHANNEL_MAP.putIfAbsent(ioChannel, nettyChannel);
            if (channel == null) {
                return nettyChannel;
            }
        }
        return channel;
    }

    /**
     * Get all cached {@link NettyChannel}s
     *
     * @return the map contains all cashed NettyChannels
     */
    public static ConcurrentMap<Channel, NettyChannel> getChannelMap() {
        return CHANNEL_MAP;
    }

    /**
     * Remove cashed NettyChannel if it has disconnected.
     *
     * @param ch the Netty's {@link Channel}
     */
    public static void removeChannelIfDisconnected(Channel ch) {
        if (ch != null && !ch.isActive()) {
            Optional.ofNullable(CHANNEL_MAP.remove(ch))
                    .ifPresent(channel -> logger.info("Removed channel [{}] from NettyChannelManager", channel));
        }
    }
}
