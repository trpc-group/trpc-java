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

package com.tencent.trpc.transport.netty.stream;

import com.tencent.trpc.core.common.config.ProtocolConfig;
import com.tencent.trpc.core.stream.transport.ClientTransport;
import com.tencent.trpc.core.stream.transport.RpcConnection;
import com.tencent.trpc.core.stream.transport.codec.FrameDecoder;
import java.util.Objects;
import java.util.function.Supplier;
import reactor.core.publisher.Mono;
import reactor.netty.tcp.TcpClient;

/**
 * Client transport implemented based on Reactor Netty.
 */
public class NettyTcpClientTransport implements ClientTransport {

    /**
     * Protocol config
     */
    private final ProtocolConfig protocolConfig;
    /**
     * Protocol frame decoder creator, used to create a separate frame decoder for each connection.
     */
    private final Supplier<FrameDecoder> frameDecoder;

    public NettyTcpClientTransport(ProtocolConfig protocolConfig,
            Supplier<FrameDecoder> frameDecoder) {
        this.protocolConfig = Objects.requireNonNull(protocolConfig, "protocolConfig is null");
        this.frameDecoder = Objects.requireNonNull(frameDecoder, "frameDecoder is null");
    }

    @Override
    public Mono<RpcConnection> connect() {
        // create a reactor netty connection and translate to RpcConnection
        return TcpClient.create()
                .host(protocolConfig.getIp())
                .port(protocolConfig.getPort())
                .doOnConnected(c ->
                        // add frame decoder
                        c.addHandlerLast(new FrameDecoderHandler(frameDecoder.get()))
                )
                .connect()
                .map(TcpRpcConnection::new);
    }

}
