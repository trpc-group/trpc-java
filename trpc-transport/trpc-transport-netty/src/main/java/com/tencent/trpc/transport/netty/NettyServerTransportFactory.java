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

package com.tencent.trpc.transport.netty;

import com.google.common.base.Preconditions;
import com.tencent.trpc.core.common.Constants;
import com.tencent.trpc.core.common.config.ProtocolConfig;
import com.tencent.trpc.core.extension.Extension;
import com.tencent.trpc.core.transport.ChannelHandler;
import com.tencent.trpc.core.transport.ServerTransport;
import com.tencent.trpc.core.transport.codec.ServerCodec;
import com.tencent.trpc.core.transport.spi.ServerTransportFactory;
import java.util.Objects;
import org.apache.commons.lang3.StringUtils;

@Extension(value = "netty")
public class NettyServerTransportFactory implements ServerTransportFactory {

    @Override
    public ServerTransport create(ProtocolConfig config, ChannelHandler handler,
            ServerCodec serverCodec) {
        Objects.requireNonNull(config, "config is null");
        Objects.requireNonNull(handler, "handler is null");
        config.init();
        String network = config.getNetwork();
        Preconditions.checkArgument(
                StringUtils.isBlank(network) || StringUtils.equals(network, Constants.NETWORK_TCP)
                        || StringUtils.equals(network, Constants.NETWORK_UDP),
                "network[%s] notsupport , only support network tcp or udp", network);

        boolean isTcp = StringUtils.isBlank(network) || network.equals(Constants.NETWORK_TCP);
        if (isTcp) {
            return new NettyTcpServerTransport(config, handler, serverCodec);
        } else {
            return new NettyUdpServerTransport(config, handler, serverCodec);
        }
    }
}
