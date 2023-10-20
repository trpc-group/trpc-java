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

package com.tencent.trpc.core.stream.transport.spi;

import com.tencent.trpc.core.common.config.ProtocolConfig;
import com.tencent.trpc.core.extension.Extensible;
import com.tencent.trpc.core.stream.Closeable;
import com.tencent.trpc.core.stream.transport.ServerTransport;
import com.tencent.trpc.core.stream.transport.codec.FrameDecoder;
import java.util.function.Supplier;

/**
 * Server transport creation factory.
 */
@Extensible("netty")
public interface ServerTransportFactory {

    /**
     * Creating a server transport.
     *
     * @param protocolConfig protocol configuration
     * @param frameDecoder frame decoder
     * @return a server transport
     */
    ServerTransport<? extends Closeable> create(ProtocolConfig protocolConfig,
            Supplier<FrameDecoder> frameDecoder);

}
