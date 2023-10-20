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

import com.tencent.trpc.core.common.Constants;
import com.tencent.trpc.core.common.config.ProtocolConfig;
import com.tencent.trpc.core.extension.Extension;
import com.tencent.trpc.core.stream.transport.ClientTransport;
import com.tencent.trpc.core.stream.transport.codec.FrameDecoder;
import com.tencent.trpc.core.stream.transport.spi.ClientTransportFactory;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Constructing a factory class for creating {@link ClientTransport} implemented by Reactor Netty.
 */
@Extension("netty")
public class NettyClientTransportFactory implements ClientTransportFactory {

    @Override
    public ClientTransport create(ProtocolConfig protocolConfig, Supplier<FrameDecoder> frameDecoder) {
        String network = protocolConfig.getNetwork();
        if (Objects.equals(Constants.NETWORK_TCP, network)) {
            return new NettyTcpClientTransport(protocolConfig, frameDecoder);
        }
        // only support tcp
        throw new IllegalArgumentException("unknown network " + network);
    }

}
