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
import com.tencent.trpc.core.compressor.support.SnappyCompressor;
import com.tencent.trpc.core.serialization.support.PBSerialization;
import com.tencent.trpc.proto.standard.common.TRPCProtocol.TrpcStreamFrameType;
import com.tencent.trpc.proto.standard.stream.codec.TRpcStreamFrameCodec.RpcCallInfo;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.UnpooledByteBufAllocator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TRpcStreamFrameCodecTest {

    @Test
    public void encodeStreamInitRequestFrame() {
        try {
            TRpcStreamFrameCodec.encodeStreamInitRequestFrame(UnpooledByteBufAllocator.DEFAULT, 0, 0,
                    getCallInfo(), getBackendConfig("notExist", "notExist"));
            Assertions.fail("check invalid serialization failed, should throw an exception");
        } catch (Exception e) {
            Assertions.assertTrue(e instanceof IllegalArgumentException);
        }

        try {
            TRpcStreamFrameCodec.encodeStreamInitRequestFrame(UnpooledByteBufAllocator.DEFAULT, 0, 0,
                    getCallInfo(), getBackendConfig(PBSerialization.NAME, "notExist"));
            Assertions.fail("check invalid compressor failed, should throw an exception");
        } catch (Exception e) {
            Assertions.assertTrue(e instanceof IllegalArgumentException);
        }

        ByteBuf frame = TRpcStreamFrameCodec.encodeStreamInitRequestFrame(UnpooledByteBufAllocator.DEFAULT, 0, 0,
                getCallInfo(), getBackendConfig(PBSerialization.NAME, SnappyCompressor.NAME));
        Assertions.assertNotNull(frame);
        Assertions.assertEquals(TrpcStreamFrameType.TRPC_STREAM_FRAME_INIT_VALUE,
                TRpcStreamFrameHeaderCodec.frameType(frame).getEncodedType());
    }

    @Test
    public void encodeStreamInitResponseFrame() {
        try {
            TRpcStreamFrameCodec.encodeStreamInitResponseFrame(UnpooledByteBufAllocator.DEFAULT, 0, 0,
                    getProtocolConfig("notExist", "notExist"), 0, "OK");
            Assertions.fail("check invalid serialization failed, should throw an exception");
        } catch (Exception e) {
            Assertions.assertTrue(e instanceof IllegalArgumentException);
        }

        try {
            TRpcStreamFrameCodec.encodeStreamInitResponseFrame(UnpooledByteBufAllocator.DEFAULT, 0, 0,
                    getProtocolConfig(PBSerialization.NAME, "notExist"), 0, "OK");
            Assertions.fail("check invalid compressor failed, should throw an exception");
        } catch (Exception e) {
            Assertions.assertTrue(e instanceof IllegalArgumentException);
        }

        ByteBuf frame = TRpcStreamFrameCodec.encodeStreamInitResponseFrame(UnpooledByteBufAllocator.DEFAULT, 0, 0,
                getProtocolConfig(PBSerialization.NAME, SnappyCompressor.NAME), 0, "OK");
        Assertions.assertNotNull(frame);
        Assertions.assertEquals(TrpcStreamFrameType.TRPC_STREAM_FRAME_INIT_VALUE,
                TRpcStreamFrameHeaderCodec.frameType(frame).getEncodedType());

        // 异常情况不检查serialization和compressor
        ByteBuf frame2 = TRpcStreamFrameCodec.encodeStreamInitResponseFrame(UnpooledByteBufAllocator.DEFAULT, 0, 0,
                getProtocolConfig("notExist", "notExist"), 404, "FAIL");
        Assertions.assertNotNull(frame2);
        Assertions.assertEquals(TrpcStreamFrameType.TRPC_STREAM_FRAME_INIT_VALUE,
                TRpcStreamFrameHeaderCodec.frameType(frame2).getEncodedType());
    }

    @Test
    public void encodeStreamCloseFrame() {
    }

    @Test
    public void decodeStreamInitFrame() {
    }

    @Test
    public void decodeStreamFeedbackFrame() {
    }

    @Test
    public void decodeStreamCloseFrame() {
    }

    @Test
    public void buildRpcCallInfo() {
    }

    @Test
    public void encodeStreamDataFrame() {
    }

    @Test
    public void decodeDataFrameData() {
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
