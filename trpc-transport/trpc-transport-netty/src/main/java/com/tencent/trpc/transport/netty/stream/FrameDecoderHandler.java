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

import com.tencent.trpc.core.stream.transport.codec.FrameDecoder;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import java.util.List;
import java.util.Objects;

/**
 * A netty decoder used to decode frames of data of the stream protocol.
 */
public class FrameDecoderHandler extends ByteToMessageDecoder {

    /**
     * Frame decoder implemented by each protocol.
     */
    private final FrameDecoder frameDecoder;

    public FrameDecoderHandler(FrameDecoder frameDecoder) {
        this.frameDecoder = Objects.requireNonNull(frameDecoder, "frameDecoder is null");
    }

    @Override
    protected void decode(ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf,
            List<Object> out) throws Exception {
        // break after parsing all frame data in the byteBuf
        for (; ; ) {
            ByteBuf frame = this.frameDecoder.decode(byteBuf);
            // a null value indicates that a frame of data has not been parsed,
            // and an exception will be thrown to terminate the connection if the data is parsed incorrectly
            if (frame == null) {
                break;
            }
            out.add(frame);
        }
    }
}
