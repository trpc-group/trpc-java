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

package com.tencent.trpc.core.transport.spi;

import com.tencent.trpc.core.common.config.ProtocolConfig;
import com.tencent.trpc.core.extension.Extensible;
import com.tencent.trpc.core.transport.ChannelHandler;
import com.tencent.trpc.core.transport.ClientTransport;
import com.tencent.trpc.core.transport.codec.ClientCodec;

/**
 * The factory of the {@link ClientTransport}, for creating {@link ClientTransport}. The default implementation
 * is Netty.
 */
@Extensible("netty")
public interface ClientTransportFactory {

    /**
     * The client startup timing is determined by the outer layer. Call start before using the client.
     *
     * @param config the ProtocolConfig for the client transport
     * @param handler the ChannelHandler for the client transport
     * @param codec the ClientCodec for the client transport
     * @return the created ClientTransport
     */
    ClientTransport create(ProtocolConfig config, ChannelHandler handler, ClientCodec codec);

}
