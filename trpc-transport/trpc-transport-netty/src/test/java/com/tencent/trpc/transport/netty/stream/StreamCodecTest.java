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

package com.tencent.trpc.transport.netty.stream;

import com.google.common.collect.Lists;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;

public class StreamCodecTest {

    @Test
    public void testCodec() throws Exception {
        FrameDecoderHandler frameDecoderHandler = new FrameDecoderHandler(
                in -> {
                    if (in.readableBytes() <= 0) {
                        return null;
                    }

                    in.skipBytes(in.readableBytes());
                    return in.retainedSlice(0, in.readableBytes());
                });

        List<Object> out = Lists.newArrayList();
        ByteBuf buffer = Unpooled.buffer(0);

        frameDecoderHandler.decode(null, buffer, out);
        Assert.assertEquals(0, out.size());

        buffer.writeByte(1);
        frameDecoderHandler.decode(null, buffer, out);
        Assert.assertEquals(1, out.size());
    }

}
