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

package com.tencent.trpc.core.transport.impl;

import com.tencent.trpc.core.common.config.ProtocolConfig;
import com.tencent.trpc.core.exception.TransportException;
import com.tencent.trpc.core.transport.AbstractServerTransport;
import com.tencent.trpc.core.transport.Channel;
import com.tencent.trpc.core.transport.ChannelHandler;
import com.tencent.trpc.core.transport.codec.ServerCodec;
import java.util.Set;

public class TestServerTransport extends AbstractServerTransport {

    public TestServerTransport(ProtocolConfig config, ChannelHandler channelHandler,
            ServerCodec serverCodec) throws TransportException {
        super(config, channelHandler, serverCodec);
    }

    @Override
    protected void doOpen() throws TransportException {

    }

    @Override
    protected void doClose() {

    }

    @Override
    public boolean isBound() {
        return false;
    }

    @Override
    public Set<Channel> getChannels() {
        return null;
    }
}