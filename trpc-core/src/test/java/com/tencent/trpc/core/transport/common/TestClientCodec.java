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

package com.tencent.trpc.core.transport.common;

import com.tencent.trpc.core.transport.Channel;
import com.tencent.trpc.core.transport.codec.ChannelBuffer;
import com.tencent.trpc.core.transport.codec.ClientCodec;

public class TestClientCodec extends ClientCodec {

    @Override
    public void encode(Channel channel, ChannelBuffer channelBuffer, Object message) {

    }

    @Override
    public Object decode(Channel channel, ChannelBuffer channelBuffer) {
        return null;
    }

}