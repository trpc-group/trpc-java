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

package com.tencent.trpc.transport.netty.stream;

import com.tencent.trpc.core.stream.Closeable;
import java.util.Objects;
import reactor.core.publisher.Mono;
import reactor.netty.DisposableChannel;

/**
 * Closable Reactor Netty server
 */
public class CloseableChannel implements Closeable {

    /**
     * Reactor Netty Server Channel
     */
    private final DisposableChannel channel;

    public CloseableChannel(DisposableChannel channel) {
        this.channel = Objects.requireNonNull(channel, "channel is null");
    }

    @Override
    public Mono<Void> onClose() {
        return channel.onDispose();
    }

    @Override
    public void dispose() {
        channel.dispose();
    }

    @Override
    public boolean isDisposed() {
        return channel.isDisposed();
    }
}
