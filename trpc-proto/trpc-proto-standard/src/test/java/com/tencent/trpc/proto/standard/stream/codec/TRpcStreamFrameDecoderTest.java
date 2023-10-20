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

import com.tencent.trpc.core.exception.ErrorCode.Stream;
import com.tencent.trpc.core.exception.TRpcException;
import com.tencent.trpc.proto.standard.common.StandardFrame;
import com.tencent.trpc.proto.standard.common.TRPCProtocol.TrpcDataFrameType;
import com.tencent.trpc.proto.standard.common.TRpcFrameType;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Assert;
import org.junit.Test;

public class TRpcStreamFrameDecoderTest {

    @Test
    public void testNormalDecode() {
        TRpcStreamFrameDecoder frameDecoder = new TRpcStreamFrameDecoder();

        int streamId = 1000;
        for (TRpcFrameType frameType : TRpcFrameType.values()) {
            ByteBuf frame = getNormalTRpcFrame(streamId, frameType);
            int frameSize = frame.readableBytes();
            ByteBuf decoded = frameDecoder.decode(frame);
            int decodedFrameSize = decoded.readableBytes();

            Assert.assertEquals(0, frame.readableBytes());
            Assert.assertTrue(decodedFrameSize > 0);
            Assert.assertEquals(frameSize, decodedFrameSize);

            Assert.assertNull(frameDecoder.decode(frame));

            frame.writeByte(0);
            Assert.assertEquals(decodedFrameSize, decoded.readableBytes());
        }
    }

    @Test
    public void testNotEnoughData() {
        TRpcStreamFrameDecoder frameDecoder = new TRpcStreamFrameDecoder();
        Assert.assertNull(frameDecoder.decode(getNotEnoughHeaderTRpcFrame()));
        Assert.assertNull(frameDecoder.decode(getNotEnoughDataTRpcFrame(100)));
    }

    @Test
    public void testInvalidMagicFrame() {
        TRpcStreamFrameDecoder frameDecoder = new TRpcStreamFrameDecoder();
        try {
            frameDecoder.decode(getInvalidMagicTRpcFrame());
            Assert.fail("check unknown magic failed, should throw an exception");
        } catch (Exception e) {
            Assert.assertTrue(e instanceof TRpcException);
        }
    }

    @Test
    public void testMultiNormalFrames() {
        TRpcStreamFrameDecoder frameDecoder = new TRpcStreamFrameDecoder();

        int streamId = 1000;
        int count = 10;

        ByteBuf frames = getMultiNormalTRpcFrame(10, streamId, TRpcFrameType.DATA);
        int singleFrameSize = frames.readableBytes() / count;

        int got = 0;
        for (; ; ) {
            ByteBuf decoded = frameDecoder.decode(frames);
            if (decoded == null) {
                break;
            }
            Assert.assertEquals(singleFrameSize, decoded.readableBytes());
            got++;
        }
        Assert.assertEquals(count, got);
    }

    private ByteBuf getNormalTRpcFrame(int streamID, TRpcFrameType frameType) {
        ByteBuf buffer = Unpooled.buffer(TRpcStreamFrameHeaderCodec.TRPC_FIX_HEADER_LENGTH);
        fillNormalEmptyFrame(buffer, streamID, frameType, TRpcStreamFrameHeaderCodec.TRPC_FIX_HEADER_LENGTH);
        return buffer;
    }

    private ByteBuf getInvalidMagicTRpcFrame() {
        ByteBuf buffer = Unpooled.buffer(TRpcStreamFrameHeaderCodec.TRPC_FIX_HEADER_LENGTH);
        fillNormalEmptyFrame(buffer, 0, TRpcFrameType.DATA, TRpcStreamFrameHeaderCodec.TRPC_FIX_HEADER_LENGTH);

        buffer.markWriterIndex();
        buffer.writerIndex(0);
        buffer.writeShort(0);
        buffer.resetWriterIndex();
        return buffer;
    }

    private ByteBuf getNotEnoughHeaderTRpcFrame() {
        ByteBuf buffer = Unpooled.buffer(TRpcStreamFrameHeaderCodec.TRPC_FIX_HEADER_LENGTH);
        buffer.writeShort(StandardFrame.TRPC_MAGIC);
        buffer.writeByte(TrpcDataFrameType.TRPC_STREAM_FRAME_VALUE);
        buffer.writeByte(TRpcFrameType.DATA.getEncodedType());  // frameType
        buffer.writeInt(TRpcStreamFrameHeaderCodec.TRPC_FIX_HEADER_LENGTH);   // total len
        return buffer;
    }

    private ByteBuf getNotEnoughDataTRpcFrame(int dataSize) {
        ByteBuf buffer = Unpooled.buffer(TRpcStreamFrameHeaderCodec.TRPC_FIX_HEADER_LENGTH + dataSize);
        fillNormalEmptyFrame(buffer, 0, TRpcFrameType.DATA,
                TRpcStreamFrameHeaderCodec.TRPC_FIX_HEADER_LENGTH + dataSize);
        return buffer;
    }

    private ByteBuf getMultiNormalTRpcFrame(int count, int streamID, TRpcFrameType frameType) {
        ByteBuf buffer = Unpooled.buffer(count * TRpcStreamFrameHeaderCodec.TRPC_FIX_HEADER_LENGTH);
        for (int i = 0; i < count; i++) {
            fillNormalEmptyFrame(buffer, streamID, frameType, TRpcStreamFrameHeaderCodec.TRPC_FIX_HEADER_LENGTH);
        }
        return buffer;
    }

    private void fillNormalEmptyFrame(ByteBuf frame, int streamId, TRpcFrameType frameType, int totalLen) {
        frame.writeShort(StandardFrame.TRPC_MAGIC);
        frame.writeByte(TrpcDataFrameType.TRPC_STREAM_FRAME_VALUE);
        frame.writeByte(frameType.getEncodedType());
        frame.writeInt(totalLen);   // total len
        frame.writeShort(0);   // head size
        frame.writeInt(streamId);
        frame.writeShort(0);   // reserved
    }

    @Test
    public void testErrorCodeStream() {
        Assert.assertNotNull(Stream.FRAME_DECODE_MAGIC_ERR.getMessage());
        Assert.assertNotNull(Stream.FRAME_DECODE_MAGIC_ERR.getStatusCode());
    }


}