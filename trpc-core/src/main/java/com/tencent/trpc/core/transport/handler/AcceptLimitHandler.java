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

package com.tencent.trpc.core.transport.handler;

import com.tencent.trpc.core.transport.Channel;
import com.tencent.trpc.core.transport.ChannelHandler;
import com.tencent.trpc.core.transport.ServerTransport;
import java.util.concurrent.atomic.AtomicInteger;

public class AcceptLimitHandler extends AbstractServerChannelHandler {

    private int maxConnections;
    private AtomicInteger connections = new AtomicInteger();

    public AcceptLimitHandler(ChannelHandler wrap, ServerTransport server) {
        super(wrap, server);
        this.maxConnections = server.getProtocolConfig().getMaxConns();
    }

    @Override
    public void connected(Channel channel) {
        if (connections.incrementAndGet() > maxConnections) {
            logger.error("reject channel " + channel + ",cause: The server " + channel.getLocalAddress()
                    + " connections[" + connections.get() + "] > " + maxConnections);
            channel.close();
            return;
        }
        super.connected(channel);
    }

    @Override
    public void disconnected(Channel ch) {
        connections.decrementAndGet();
        super.disconnected(ch);
    }

}
