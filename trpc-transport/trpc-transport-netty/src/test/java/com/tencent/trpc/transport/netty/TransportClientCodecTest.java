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

import com.tencent.trpc.core.transport.Channel;
import com.tencent.trpc.core.transport.codec.ChannelBuffer;
import com.tencent.trpc.core.transport.codec.ClientCodec;
import com.tencent.trpc.transport.netty.NettyTest.TestRequest;
import com.tencent.trpc.transport.netty.NettyTest.TestResponse;

public class TransportClientCodecTest extends ClientCodec {

    @Override
    public void encode(Channel channel, ChannelBuffer channelBuffer, Object message) {
        channelBuffer.writeBytes(((TestRequest) message).getBody().getBytes());
    }

    @Override
    public Object decode(Channel channel, ChannelBuffer channelBuffer) {
        if (channelBuffer.readableBytes() <= 0) {
            return DecodeResult.NOT_ENOUGH_DATA;
        }
        byte[] bytes = new byte[NettyProtoTest.MESSAGE.getBytes().length];
        channelBuffer.readBytes(bytes);
        return new TestResponse(null, new String(bytes));
    }
}
