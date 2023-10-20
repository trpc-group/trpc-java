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

package com.tencent.trpc.proto.standard.stream.codec;

import com.tencent.trpc.proto.standard.common.StandardFrame;
import com.tencent.trpc.proto.standard.common.TRPCProtocol.TrpcDataFrameType;
import com.tencent.trpc.proto.standard.common.TRpcFrameType;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Assert;
import org.junit.Test;

public class TRpcStreamFrameHeaderCodecTest {

    @Test
    public void tesetNormalFrame() {
        int streamId = 1000;
        for (TRpcFrameType frameType : TRpcFrameType.values()) {
            ByteBuf normalTRpcFrame = getNormalTRpcFrame(streamId, frameType);
            // check frame
            checkNormalFrame(normalTRpcFrame, streamId, frameType);
            // check twice, ensure frame read position not modified
            checkNormalFrame(normalTRpcFrame, streamId, frameType);
        }
    }

    private void checkNormalFrame(ByteBuf frame, int streamId, TRpcFrameType frameType) {
        Assert.assertEquals(TRpcStreamFrameHeaderCodec.magic(frame), StandardFrame.TRPC_MAGIC);
        Assert.assertEquals(TRpcStreamFrameHeaderCodec.frameSize(frame), frame.readableBytes());
        Assert.assertEquals(TRpcStreamFrameHeaderCodec.streamId(frame), streamId);
        Assert.assertEquals(TRpcStreamFrameHeaderCodec.frameType(frame), frameType);
    }

    @Test
    public void testUnknownFrame() {
        try {
            TRpcStreamFrameHeaderCodec.frameType(getAbnormalDataTypeTRpcFrame());
            Assert.fail("check unknown dataType failed, should throw an exception");
        } catch (Exception e) {
            Assert.assertTrue(e instanceof IllegalArgumentException);
        }

        try {
            TRpcStreamFrameHeaderCodec.frameType(getAbnormalFrameTypeTRpcFrame());
            Assert.fail("check unknown frameType failed, should throw an exception");
        } catch (Exception e) {
            Assert.assertTrue(e instanceof IllegalArgumentException);
        }
    }

    private ByteBuf getNormalTRpcFrame(int streamID, TRpcFrameType frameType) {
        ByteBuf buffer = Unpooled.buffer(TRpcStreamFrameHeaderCodec.TRPC_FIX_HEADER_LENGTH);
        buffer.writeShort(StandardFrame.TRPC_MAGIC);
        buffer.writeByte(TrpcDataFrameType.TRPC_STREAM_FRAME_VALUE);
        buffer.writeByte(frameType.getEncodedType());
        buffer.writeInt(TRpcStreamFrameHeaderCodec.TRPC_FIX_HEADER_LENGTH);
        buffer.writeShort(0);   // head size
        buffer.writeInt(streamID);
        buffer.writeShort(0);   // reserved
        return buffer;
    }

    private ByteBuf getAbnormalDataTypeTRpcFrame() {
        ByteBuf buffer = Unpooled.buffer(TRpcStreamFrameHeaderCodec.TRPC_FIX_HEADER_LENGTH);
        buffer.writeShort(0);   // invalid magic
        buffer.writeByte(TrpcDataFrameType.TRPC_STREAM_FRAME_VALUE);
        buffer.writeByte(0);    // invalid frameType
        buffer.writeInt(TRpcStreamFrameHeaderCodec.TRPC_FIX_HEADER_LENGTH);
        buffer.writeShort(0);   // head size
        buffer.writeInt(0);     // streamID
        buffer.writeShort(0);   // reserved
        return buffer;
    }

    private ByteBuf getAbnormalFrameTypeTRpcFrame() {
        ByteBuf buffer = Unpooled.buffer(TRpcStreamFrameHeaderCodec.TRPC_FIX_HEADER_LENGTH);
        buffer.writeShort(0);   // invalid magic
        buffer.writeByte(TrpcDataFrameType.TRPC_STREAM_FRAME_VALUE);
        buffer.writeByte(0);    // invalid frameType
        buffer.writeInt(TRpcStreamFrameHeaderCodec.TRPC_FIX_HEADER_LENGTH);
        buffer.writeShort(0);   // head size
        buffer.writeInt(0);     // streamID
        buffer.writeShort(0);   // reserved
        return buffer;
    }
}