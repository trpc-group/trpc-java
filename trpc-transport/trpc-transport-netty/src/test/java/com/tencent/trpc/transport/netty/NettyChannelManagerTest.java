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

import io.netty.channel.Channel;
import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.ConcurrentMap;

public class NettyChannelManagerTest {

    @Test
    public void test() {
        Assert.assertTrue(NettyChannelManager.getOrAddChannel(null, null) == null);
    }


    @Test
    public void getChannelMap() {
        ConcurrentMap<Channel, NettyChannel>  channelMap = NettyChannelManager.getChannelMap();
        Assert.assertNotEquals(channelMap, null);
    }
}
