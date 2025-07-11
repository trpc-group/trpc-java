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

import com.tencent.trpc.core.stream.transport.RpcConnection;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.netty.Connection;

import java.util.Objects;

/**
 * Represents a bidirectional TCP communication connection, encapsulating Reactor Netty's Connection.
 */
public class TcpRpcConnection implements RpcConnection {

    /**
     * Network packet sender, which uses an infinite queue by default.
     * If the connection is abnormal, the sending queue will be closed
     * and any unsent packets will be cleared.
     */
    private final Sinks.Many<ByteBuf> sender = Sinks.many().unicast().onBackpressureBuffer();
    /**
     * Used to listen for connection close events.
     */
    private final Sinks.Empty<Void> onClose = Sinks.empty();
    /**
     * Underlying Reactor Netty connection.
     */
    private final Connection connection;

    public TcpRpcConnection(Connection connection) {
        this.connection = Objects.requireNonNull(connection, "connection is null");
        onClose.asMono().doFinally(s -> doOnClose()).subscribe();

        connection.channel()
                .closeFuture()
                .addListener(future -> {
                    if (!isDisposed()) {
                        dispose();
                    }
                });

        connection.outbound().send(sender.asFlux()).then().subscribe();
    }

    @Override
    public void send(ByteBuf frame) {
        sender.emitNext(frame, Sinks.EmitFailureHandler.FAIL_FAST);
    }

    @Override
    public Flux<ByteBuf> receive() {
        return connection.inbound().receive();
    }

    @Override
    public ByteBufAllocator alloc() {
        return connection.channel().alloc();
    }

    public void doOnClose() {
        connection.dispose();
    }

    @Override
    public Mono<Void> onClose() {
        return connection.onDispose();
    }

    @Override
    public void dispose() {
        onClose.emitEmpty(Sinks.EmitFailureHandler.FAIL_FAST);
    }

    @Override
    public boolean isDisposed() {
        return connection.isDisposed();
    }
}
