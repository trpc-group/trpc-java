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

import com.tencent.trpc.core.common.config.BackendConfig;
import com.tencent.trpc.core.common.config.ProtocolConfig;
import com.tencent.trpc.core.compressor.CompressorSupport;
import com.tencent.trpc.core.compressor.support.SnappyCompressor;
import com.tencent.trpc.core.serialization.SerializationSupport;
import com.tencent.trpc.core.serialization.support.PBSerialization;
import com.tencent.trpc.proto.standard.common.TRPCProtocol;
import com.tencent.trpc.proto.standard.common.TRPCProtocol.TrpcStreamFrameType;
import com.tencent.trpc.proto.standard.common.TRpcFrameType;
import com.tencent.trpc.proto.standard.stream.codec.TRpcStreamFrameCodec.RpcCallInfo;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.buffer.UnpooledByteBufAllocator;
import org.junit.Assert;
import org.junit.Test;

public class TRpcStreamFrameCodecTest {

    @Test
    public void encodeStreamInitRequestFrame() {
        try {
            TRpcStreamFrameCodec.encodeStreamInitRequestFrame(UnpooledByteBufAllocator.DEFAULT, 0, 0,
                    getCallInfo(), getBackendConfig("notExist", "notExist"));
            Assert.fail("check invalid serialization failed, should throw an exception");
        } catch (Exception e) {
            Assert.assertTrue(e instanceof IllegalArgumentException);
        }

        try {
            TRpcStreamFrameCodec.encodeStreamInitRequestFrame(UnpooledByteBufAllocator.DEFAULT, 0, 0,
                    getCallInfo(), getBackendConfig(PBSerialization.NAME, "notExist"));
            Assert.fail("check invalid compressor failed, should throw an exception");
        } catch (Exception e) {
            Assert.assertTrue(e instanceof IllegalArgumentException);
        }

        ByteBuf frame = TRpcStreamFrameCodec.encodeStreamInitRequestFrame(UnpooledByteBufAllocator.DEFAULT, 0, 0,
                getCallInfo(), getBackendConfig(PBSerialization.NAME, SnappyCompressor.NAME));
        Assert.assertNotNull(frame);
        Assert.assertEquals(TrpcStreamFrameType.TRPC_STREAM_FRAME_INIT_VALUE,
                TRpcStreamFrameHeaderCodec.frameType(frame).getEncodedType());
    }

    @Test
    public void encodeStreamInitResponseFrame() {
        try {
            TRpcStreamFrameCodec.encodeStreamInitResponseFrame(UnpooledByteBufAllocator.DEFAULT, 0, 0,
                    getProtocolConfig("notExist", "notExist"), 0, "OK");
            Assert.fail("check invalid serialization failed, should throw an exception");
        } catch (Exception e) {
            Assert.assertTrue(e instanceof IllegalArgumentException);
        }

        try {
            TRpcStreamFrameCodec.encodeStreamInitResponseFrame(UnpooledByteBufAllocator.DEFAULT, 0, 0,
                    getProtocolConfig(PBSerialization.NAME, "notExist"), 0, "OK");
            Assert.fail("check invalid compressor failed, should throw an exception");
        } catch (Exception e) {
            Assert.assertTrue(e instanceof IllegalArgumentException);
        }

        ByteBuf frame = TRpcStreamFrameCodec.encodeStreamInitResponseFrame(UnpooledByteBufAllocator.DEFAULT, 0, 0,
                getProtocolConfig(PBSerialization.NAME, SnappyCompressor.NAME), 0, "OK");
        Assert.assertNotNull(frame);
        Assert.assertEquals(TrpcStreamFrameType.TRPC_STREAM_FRAME_INIT_VALUE,
                TRpcStreamFrameHeaderCodec.frameType(frame).getEncodedType());

        // 异常情况不检查serialization和compressor
        ByteBuf frame2 = TRpcStreamFrameCodec.encodeStreamInitResponseFrame(UnpooledByteBufAllocator.DEFAULT, 0, 0,
                getProtocolConfig("notExist", "notExist"), 404, "FAIL");
        Assert.assertNotNull(frame2);
        Assert.assertEquals(TrpcStreamFrameType.TRPC_STREAM_FRAME_INIT_VALUE,
                TRpcStreamFrameHeaderCodec.frameType(frame2).getEncodedType());
    }

    @Test
    public void encodeStreamResetFrame() {
        ByteBufAllocator allocator = UnpooledByteBufAllocator.DEFAULT;
        int streamId = 1;
        int retCode = 500;
        String errMsg = "Internal Server Error";

        ByteBuf frame = TRpcStreamFrameCodec.encodeStreamResetFrame(allocator, streamId, retCode, errMsg);
        Assert.assertNotNull(frame);
        Assert.assertEquals(TRpcFrameType.CLOSE.getEncodedType(),
                TRpcStreamFrameHeaderCodec.frameType(frame).getEncodedType());
    }

    @Test
    public void encodeStreamCloseFrame() {
    }

    @Test
    public void decodeStreamInitFrame() {
        ByteBuf invalidFrame = Unpooled.buffer();
        invalidFrame.writeBytes(new byte[]{0x00, 0x01, 0x02, 0x03});

        try {
            TRPCProtocol.TrpcStreamInitMeta initMeta = TRpcStreamFrameCodec.decodeStreamInitFrame(invalidFrame);
            Assert.fail("Expected an IllegalArgumentException to be thrown");
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(e instanceof IllegalArgumentException);
        }
    }

    @Test
    public void decodeStreamFeedbackFrame() {
        ByteBuf invalidFrame = Unpooled.buffer();
        invalidFrame.writeBytes(new byte[]{0x00, 0x01, 0x02, 0x03});

        try {
            TRPCProtocol.TrpcStreamFeedBackMeta feedBackMeta = TRpcStreamFrameCodec.decodeStreamFeedbackFrame(invalidFrame);
            Assert.fail("Expected an IllegalArgumentException to be thrown");
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(e instanceof IllegalArgumentException);
        }
    }

    @Test
    public void decodeStreamCloseFrame() {
        ByteBuf invalidFrame = Unpooled.buffer();
        invalidFrame.writeBytes(new byte[]{0x00, 0x01, 0x02, 0x03});

        try {
            TRPCProtocol.TrpcStreamCloseMeta closeMeta = TRpcStreamFrameCodec.decodeStreamCloseFrame(invalidFrame);
            Assert.fail("Expected an IllegalArgumentException to be thrown");
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(e instanceof IllegalArgumentException);
        }
    }

    @Test
    public void buildRpcCallInfo() {
    }

    @Test
    public void encodeStreamDataFrame() {
    }

    @Test
    public void decodeDataFrameData() {
        ProtocolConfig protocolConfig = getProtocolConfig("PBSerialization", "SnappyCompressor");
        TRpcStreamFrameCodec codec = TRpcStreamFrameCodec.newDataFrameCodec(
                protocolConfig,
                UnpooledByteBufAllocator.DEFAULT,
                0,
                0
        );

        ByteBuf invalidFrame = Unpooled.buffer();
        invalidFrame.writeBytes(new byte[]{0x00, 0x01, 0x02, 0x03});

        try {
            String data = codec.decodeDataFrameData(invalidFrame, String.class);
            Assert.fail("Expected an IllegalArgumentException to be thrown");
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(e instanceof IllegalArgumentException);
        }
    }

    private BackendConfig getBackendConfig(String serialization, String compressor) {
        BackendConfig backendConfig = new BackendConfig();
        backendConfig.setSerialization(serialization);
        backendConfig.setCompressor(compressor);
        return backendConfig;
    }

    private ProtocolConfig getProtocolConfig(String serialization, String compressor) {
        ProtocolConfig protocolConfig = new ProtocolConfig();
        protocolConfig.setSerialization(serialization);
        protocolConfig.setCompressor(compressor);
        return protocolConfig;
    }

    private RpcCallInfo getCallInfo() {
        return new RpcCallInfo("trpc.testApp.testServer.testService1",
                "trpc.testApp.testServer.testService2",
                "/trpc.testApp.testServer.testService2/func1");
    }
}