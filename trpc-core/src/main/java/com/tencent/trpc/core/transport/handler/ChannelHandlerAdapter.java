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

import com.tencent.trpc.core.logger.Logger;
import com.tencent.trpc.core.logger.LoggerFactory;
import com.tencent.trpc.core.transport.ChannelHandler;
import java.util.concurrent.atomic.AtomicInteger;

public class ChannelHandlerAdapter implements ChannelHandler {

    private static final Logger logger = LoggerFactory.getLogger(ChannelHandlerAdapter.class);

    private final AtomicInteger connectedCnt = new AtomicInteger();
    private final AtomicInteger disconnectedCnt = new AtomicInteger();

    @Override
    public void connected(com.tencent.trpc.core.transport.Channel channel) {
        connectedCnt.incrementAndGet();
    }

    @Override
    public void disconnected(com.tencent.trpc.core.transport.Channel channel) {
        if (logger.isDebugEnabled()) {
            logger.debug("disconnected channel|{}", channel);
        }
        disconnectedCnt.incrementAndGet();
    }

    @Override
    public void send(com.tencent.trpc.core.transport.Channel channel, Object message) {
    }

    @Override
    public void received(com.tencent.trpc.core.transport.Channel channel, Object message) {
    }

    @Override
    public void caught(com.tencent.trpc.core.transport.Channel channel, Throwable exception) {
        logger.error("channel|" + channel + "exception", exception);
    }

    @Override
    public void destroy() {
    }

    public int getConnectedCnt() {
        return connectedCnt.get();
    }

    public int getDisconnectedCnt() {
        return disconnectedCnt.get();
    }

}
