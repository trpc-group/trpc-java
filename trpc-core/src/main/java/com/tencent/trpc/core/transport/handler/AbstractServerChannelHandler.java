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

import com.tencent.trpc.core.transport.ChannelHandler;
import com.tencent.trpc.core.transport.ServerTransport;

public class AbstractServerChannelHandler extends AbstractChannelHandler {

    protected ServerTransport server;

    public AbstractServerChannelHandler(ChannelHandler wrap, ServerTransport server) {
        super(wrap);
        this.server = server;
    }

}
