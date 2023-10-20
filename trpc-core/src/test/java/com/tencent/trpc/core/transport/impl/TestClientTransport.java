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

package com.tencent.trpc.core.transport.impl;

import com.tencent.trpc.core.common.config.ProtocolConfig;
import com.tencent.trpc.core.transport.AbstractClientTransport;
import com.tencent.trpc.core.transport.Channel;
import com.tencent.trpc.core.transport.ChannelHandler;
import com.tencent.trpc.core.transport.codec.ClientCodec;
import com.tencent.trpc.core.transport.common.TestChannel;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class TestClientTransport extends AbstractClientTransport {

    private Set<Channel> channels = new HashSet<>();

    public TestClientTransport(ProtocolConfig config, ChannelHandler handler,
            ClientCodec clientCodec) {
        super(config, handler, clientCodec);
    }

    @Override
    protected void doOpen() {
        Channel channel = new TestChannel(this.getProtocolConfig());
        channels.add(channel);
    }

    @Override
    protected CompletableFuture<Channel> make() {
        Channel channel = new TestChannel(this.getProtocolConfig());
        channels.add(channel);
        CompletableFuture<Channel> completableFuture = CompletableFuture.completedFuture(channel);
        return completableFuture;
    }

    @Override
    protected void doClose() {
        channels.forEach(Channel::close);
    }

    @Override
    public Set<Channel> getChannels() {
        return channels;
    }

    @Override
    protected boolean useChannelPool() {
        return config.isKeepAlive();
    }
}