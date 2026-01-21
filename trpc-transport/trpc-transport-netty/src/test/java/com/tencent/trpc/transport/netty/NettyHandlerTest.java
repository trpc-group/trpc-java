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

import com.tencent.trpc.core.common.config.ProtocolConfig;
import com.tencent.trpc.core.logger.Logger;
import com.tencent.trpc.core.logger.LoggerFactory;
import com.tencent.trpc.core.transport.handler.ChannelHandlerAdapter;
import org.junit.jupiter.api.Test;

public class NettyHandlerTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(NettyHandlerTest.class);

    @Test
    public void test() {
        NettyClientHandler handler =
                new NettyClientHandler(new ChannelHandlerAdapter() {
                }, new ProtocolConfig(), true);
        try {
            handler.exceptionCaught(null, null);
        } catch (Exception e) {
            LOGGER.error("error:", e);
        }
        try {
            handler.userEventTriggered(null, null);
        } catch (Exception e) {
            LOGGER.error("error:", e);
        }

        NettyServerHandler svrhandler =
                new NettyServerHandler(new ChannelHandlerAdapter() {
                }, new ProtocolConfig(), true);
        try {
            svrhandler.exceptionCaught(null, null);
        } catch (Exception e) {
            LOGGER.error("error:", e);
        }
        try {
            svrhandler.userEventTriggered(null, null);
        } catch (Exception e) {
            LOGGER.error("error:", e);
        }
    }
}
