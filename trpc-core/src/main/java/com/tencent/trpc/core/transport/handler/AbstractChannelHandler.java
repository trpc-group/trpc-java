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
import com.tencent.trpc.core.transport.AbstractServerTransport;
import com.tencent.trpc.core.transport.Channel;
import com.tencent.trpc.core.transport.ChannelHandler;

public abstract class AbstractChannelHandler implements ChannelHandler {

    protected static final Logger logger = LoggerFactory.getLogger(AbstractServerTransport.class);

    ChannelHandler wrap;

    public AbstractChannelHandler(ChannelHandler wrap) {
        super();
        this.wrap = wrap;
    }

    @Override
    public void connected(Channel ch) {
        wrap.connected(ch);
    }

    @Override
    public void disconnected(Channel ch) {
        wrap.disconnected(ch);
    }

    @Override
    public void send(Channel ch, Object msg) {
        wrap.send(ch, msg);
    }

    @Override
    public void received(Channel ch, Object msg) {
        wrap.received(ch, msg);
    }

    @Override
    public void caught(Channel ch, Throwable ex) {
        wrap.caught(ch, ex);
    }

    @Override
    public void destroy() {
        wrap.destroy();
    }

}
