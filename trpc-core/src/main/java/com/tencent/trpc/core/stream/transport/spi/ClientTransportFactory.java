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

package com.tencent.trpc.core.stream.transport.spi;

import com.tencent.trpc.core.common.config.ProtocolConfig;
import com.tencent.trpc.core.extension.Extensible;
import com.tencent.trpc.core.stream.transport.ClientTransport;
import com.tencent.trpc.core.stream.transport.codec.FrameDecoder;
import java.util.function.Supplier;

/**
 * Client transport creation factory, loaded through extensions.
 */
@Extensible("netty")
public interface ClientTransportFactory {

    /**
     * Creating a client transport
     *
     * @param protocolConfig protocol configuration
     * @param frameDecoder frame decoder
     * @return a client transport
     */
    ClientTransport create(ProtocolConfig protocolConfig, Supplier<FrameDecoder> frameDecoder);

}
