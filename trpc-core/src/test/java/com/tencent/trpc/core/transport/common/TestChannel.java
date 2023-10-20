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

package com.tencent.trpc.core.transport.common;

import com.tencent.trpc.core.common.config.ProtocolConfig;
import com.tencent.trpc.core.transport.AbstractChannel;
import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class TestChannel extends AbstractChannel {

    private ProtocolConfig config;

    private InetSocketAddress remoteAddress;

    private InetSocketAddress localAddress;

    public TestChannel(ProtocolConfig config) {
        this.config = config;
        this.remoteAddress = new InetSocketAddress(config.getIp(), config.getPort());
        this.localAddress = new InetSocketAddress(config.getIp(), config.getPort());
    }

    @Override
    protected CompletionStage<Void> doSend(Object message) {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    protected CompletionStage<Void> doClose() {
        return null;
    }

    @Override
    public boolean isConnected() {
        return true;
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
    public ProtocolConfig getProtocolConfig() {
        return config;
    }
}