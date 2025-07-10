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

import com.tencent.trpc.core.common.config.ProtocolConfig;
import com.tencent.trpc.core.stream.transport.ServerTransport;
import com.tencent.trpc.core.stream.transport.codec.FrameDecoder;
import java.util.Objects;
import java.util.function.Supplier;
import reactor.core.publisher.Mono;
import reactor.netty.tcp.TcpServer;

/**
 * Server transport implemented based on Reactor Netty.
 */
public class NettyTcpServerTransport implements ServerTransport<CloseableChannel> {

    /**
     * Protocol config
     */
    private final ProtocolConfig protocolConfig;
    /**
     * Protocol frame decoder creator, used to create a separate frame decoder for each connection.
     */
    private final Supplier<FrameDecoder> frameDecoder;

    public NettyTcpServerTransport(ProtocolConfig protocolConfig, Supplier<FrameDecoder> frameDecoder) {
        this.protocolConfig = Objects.requireNonNull(protocolConfig, "protocolConfig is null");
        this.frameDecoder = Objects.requireNonNull(frameDecoder, "frameDecoder is null");
    }

    @Override
    public Mono<CloseableChannel> start(ConnectionAcceptor acceptor) {
        // create a reactor netty server
        return TcpServer.create()
                .host(protocolConfig.getIp())
                .port(protocolConfig.getPort())
                .doOnConnection(c -> {
                    c.addHandlerLast(new FrameDecoderHandler(frameDecoder.get()));
                    acceptor.apply(new TcpRpcConnection(c))
                            .then(Mono.<Void>never())
                            .subscribe(c.disposeSubscriber());
                })
                .bind()
                .map(CloseableChannel::new);
    }

}
