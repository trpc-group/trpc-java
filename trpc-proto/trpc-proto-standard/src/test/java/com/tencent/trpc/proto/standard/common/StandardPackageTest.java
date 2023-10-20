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

package com.tencent.trpc.proto.standard.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.google.protobuf.ByteString;
import com.tencent.trpc.core.common.config.ProtocolConfig;
import com.tencent.trpc.proto.standard.common.TRPCProtocol.TrpcProtoVersion;
import com.tencent.trpc.transport.netty.NettyChannel;
import com.tencent.trpc.transport.netty.NettyChannelBuffer;
import io.netty.buffer.UnpooledByteBufAllocator;
import org.apache.commons.lang3.ArrayUtils;
import org.junit.Test;

public class StandardPackageTest {

    @Test
    public void test() {
        StandardPackage pkg = new StandardPackage();
        pkg.setBodyBytes(new byte[]{1, 2, 3, 4});
        TRPCProtocol.RequestProtocol.Builder requestHeader = TRPCProtocol.RequestProtocol.newBuilder()
                .setContentType(0)
                .setVersion(TrpcProtoVersion.TRPC_PROTO_V1_VALUE)
                .setRequestId(1)
                .setAttachmentSize(0)
                .setFunc(ByteString.copyFromUtf8(""));
        byte[] head = requestHeader.build().toByteArray();
        pkg.setHeadBytes(head);
        pkg.getFrame().setHeadSize(pkg.getHeadBytes().length);
        pkg.getFrame()
                .setSize(pkg.getBodyBytes().length + pkg.getHeadBytes().length
                        + StandardFrame.FRAME_SIZE);
        pkg.getFrame().setMagic((short) 0x930);
        pkg.getFrame().setState((byte) 1);
        pkg.getFrame().setStreamId(100);
        pkg.getFrame().setType((byte) 2);
        ProtocolConfig config = new ProtocolConfig();
        config.setIp("127.0.0.1");
        config.setPort(54321);
        config.setDefault();
        NettyChannel channel = new NettyChannel(null, config);
        NettyChannelBuffer nettyChannelBuffer =
                new NettyChannelBuffer(UnpooledByteBufAllocator.DEFAULT.buffer(65535));
        pkg.write(nettyChannelBuffer);

        StandardPackage newPkg = (StandardPackage) StandardPackage
                .decode(channel, nettyChannelBuffer, true);

        assertEquals(newPkg.getFrame().getMagic(), (short) 0x930);
        assertEquals(newPkg.getFrame().getState(), 1);
        assertEquals(newPkg.getFrame().getStreamId(), 100);
        assertEquals(newPkg.getFrame().getType(), 2);
        assertEquals(newPkg.getFrame().getHeadSize(), head.length);
        assertTrue(ArrayUtils.isEquals(newPkg.getBodyBytes(), new byte[]{1, 2, 3, 4}));
        assertTrue(ArrayUtils.isEquals(newPkg.getHeadBytes(), head));

    }
}
