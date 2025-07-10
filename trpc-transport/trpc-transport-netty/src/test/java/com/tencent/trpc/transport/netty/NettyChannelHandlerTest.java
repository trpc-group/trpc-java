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

package com.tencent.trpc.transport.netty;

import com.tencent.trpc.core.common.config.ProtocolConfig;
import com.tencent.trpc.core.transport.handler.ChannelHandlerAdapter;
import io.netty.handler.timeout.IdleStateEvent;
import org.junit.Assert;
import org.junit.Test;

public class NettyChannelHandlerTest {

    @Test
    public void test() throws Exception {
        ChannelTest channelTest1 = new ChannelTest();
        channelTest1.setActive(true);
        new NettyClientHandler(new ChannelHandlerAdapter(), new ProtocolConfig(), true)
                .exceptionCaught(new ChannelHandlerContextTest(channelTest1),
                        new RuntimeException(""));
        Assert.assertTrue(channelTest1.getIsClose() != null && channelTest1.isClose);

        ChannelTest channelTest2 = new ChannelTest();
        channelTest2.setActive(true);
        new NettyClientHandler(new ChannelHandlerAdapter(), new ProtocolConfig(), true)
                .userEventTriggered(new ChannelHandlerContextTest(channelTest2),
                        IdleStateEvent.WRITER_IDLE_STATE_EVENT);
        Assert.assertTrue(channelTest2.getIsClose() != null && channelTest2.isClose);

        ChannelTest channelTest3 = new ChannelTest();
        channelTest3.setActive(true);
        new NettyServerHandler(new ChannelHandlerAdapter(), new ProtocolConfig(), true)
                .exceptionCaught(new ChannelHandlerContextTest(channelTest3),
                        new RuntimeException(""));
        Assert.assertTrue(channelTest3.getIsClose() != null && channelTest3.isClose);

        ChannelTest channelTest4 = new ChannelTest();
        channelTest4.setActive(true);
        new NettyServerHandler(new ChannelHandlerAdapter(), new ProtocolConfig(), true)
                .userEventTriggered(new ChannelHandlerContextTest(channelTest4),
                        IdleStateEvent.WRITER_IDLE_STATE_EVENT);
        Assert.assertTrue(channelTest4.getIsClose() != null && channelTest4.isClose);
    }
}
