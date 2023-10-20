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
import com.tencent.trpc.core.transport.AbstractChannel;
import java.net.InetSocketAddress;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class NettyChannel extends AbstractChannel {

    private io.netty.channel.Channel ioChannel;
    private ProtocolConfig config;
    private InetSocketAddress remoteAddress;
    private InetSocketAddress localAddress;

    /**
     * For test
     */
    public NettyChannel() {
    }

    public NettyChannel(io.netty.channel.Channel channel, ProtocolConfig config) {
        this.ioChannel = channel;
        this.config = config;
        if (channel != null) {
            // can't get the remote address while using udp, so the remoteAddress is null
            this.remoteAddress = ((InetSocketAddress) channel.remoteAddress());
            this.localAddress = (InetSocketAddress) channel.localAddress();
        }
        // listen for the close event
        if (channel != null && channel.closeFuture() != null) {
            channel.closeFuture().addListener(
                    future -> NettyChannelManager.removeChannelIfDisconnected(channel));
        }
    }

    @Override
    public InetSocketAddress getRemoteAddress() {
        return remoteAddress;
    }

    @Override
    public InetSocketAddress getLocalAddress() {
        return localAddress;
    }

    @Override
    public boolean isConnected() {
        return ioChannel != null && ioChannel.isActive();
    }

    @Override
    public ProtocolConfig getProtocolConfig() {
        return config;
    }

    @Override
    public CompletableFuture<Void> doSend(final Object message) {
        return NettyFutureUtils.from(ioChannel.writeAndFlush(message));
    }

    @Override
    public CompletableFuture<Void> doClose() {
        if (ioChannel != null) {
            return NettyFutureUtils.from(ioChannel.close());
        } else {
            return CompletableFuture.completedFuture(null);
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(ioChannel);
    }

    public io.netty.channel.Channel getIoChannel() {
        return ioChannel;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        NettyChannel other = (NettyChannel) obj;
        return Objects.equals(ioChannel, other.ioChannel);
    }
}
