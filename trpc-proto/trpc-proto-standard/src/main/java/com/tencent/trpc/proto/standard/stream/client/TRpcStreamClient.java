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

package com.tencent.trpc.proto.standard.stream.client;

import com.tencent.trpc.core.common.config.ConsumerConfig;
import com.tencent.trpc.core.common.config.ProtocolConfig;
import com.tencent.trpc.core.extension.ExtensionLoader;
import com.tencent.trpc.core.rpc.AbstractRpcClient;
import com.tencent.trpc.core.rpc.ConsumerInvoker;
import com.tencent.trpc.core.stream.transport.ClientTransport;
import com.tencent.trpc.core.stream.transport.codec.FrameDecoder;
import com.tencent.trpc.core.stream.transport.spi.ClientTransportFactory;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * TRPC stream client
 */
public class TRpcStreamClient extends AbstractRpcClient {

    /**
     * TRPC protocol frame decoder for frame decoders created individually for each link
     */
    private final Supplier<FrameDecoder> frameDecoder;
    /**
     * Stream client transport
     */
    private ClientTransport clientTransport;

    public TRpcStreamClient(ProtocolConfig protocolConfig, Supplier<FrameDecoder> frameDecoder) {
        this.protocolConfig = Objects.requireNonNull(protocolConfig, "protocolConfig is null");
        this.frameDecoder = Objects.requireNonNull(frameDecoder, "frameDecoder is null");
    }

    @Override
    protected void doOpen() throws Exception {
        ClientTransportFactory transportFactory = ExtensionLoader.getExtensionLoader(ClientTransportFactory.class)
                .getExtension(protocolConfig.getTransporter());
        this.clientTransport = transportFactory.create(protocolConfig, frameDecoder);
    }

    /**
     * Currently the connection is managed by {@link StreamConsumerInvoker} in each stream call
     * and the connection is closed after each stream call.
     */
    @Override
    protected void doClose() {
    }

    @Override
    public <T> ConsumerInvoker<T> createInvoker(ConsumerConfig<T> consumerConfig) {
        // encapsulate the transport into the client invoker
        return new StreamConsumerInvoker<>(consumerConfig, this.protocolConfig, this.clientTransport);
    }

}
