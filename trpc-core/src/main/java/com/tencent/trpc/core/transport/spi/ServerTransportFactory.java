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

package com.tencent.trpc.core.transport.spi;

import com.tencent.trpc.core.common.config.ProtocolConfig;
import com.tencent.trpc.core.extension.Extensible;
import com.tencent.trpc.core.transport.ChannelHandler;
import com.tencent.trpc.core.transport.ServerTransport;
import com.tencent.trpc.core.transport.codec.ServerCodec;

/**
 * The factory of the {@link ServerTransport}, for creating {@link ServerTransport}. The default implementation
 * is Netty.
 */
@Extensible("netty")
public interface ServerTransportFactory {

    /**
     * The server startup timing is determined by the outer layer. Call start before using the server.
     *
     * @param config the ProtocolConfig for the server transport
     * @param handler the ChannelHandler for the server transport
     * @param codec the ServerCodec for the server transport
     * @return the created ServerTransport
     */
    ServerTransport create(ProtocolConfig config, ChannelHandler handler, ServerCodec codec);

}
