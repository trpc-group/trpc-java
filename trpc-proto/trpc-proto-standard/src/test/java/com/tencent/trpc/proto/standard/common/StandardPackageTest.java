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
import com.tencent.trpc.core.exception.TRpcException;
import com.tencent.trpc.proto.standard.common.TRPCProtocol.TrpcProtoVersion;
import com.tencent.trpc.transport.netty.NettyChannel;
import com.tencent.trpc.transport.netty.NettyChannelBuffer;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.buffer.UnpooledByteBufAllocator;
import org.apache.commons.lang3.ArrayUtils;
import org.junit.Assert;
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

        StandardPackage pkg2 = new StandardPackage();
        pkg2.setBodyBytes(new byte[]{1, 2, 3, 4});
        pkg2.setHeadBytes(head);
        pkg2.getFrame().setHeadSize(pkg.getHeadBytes().length);
        pkg2.getFrame()
                .setSize(pkg.getBodyBytes().length + pkg.getHeadBytes().length
                        + StandardFrame.FRAME_SIZE);
        pkg2.getFrame().setMagic((short) 0x0000); // not equals FRAME_MAGIC
        pkg2.getFrame().setState((byte) 1);
        pkg2.getFrame().setStreamId(100);
        pkg2.getFrame().setType((byte) 2);
        pkg2.write(nettyChannelBuffer);
        try {
            StandardPackage newPkg2 = (StandardPackage) StandardPackage
                    .decode(channel, nettyChannelBuffer, true);
            Assert.fail("invalid frame magaic");
        } catch (Exception e) {
            e.printStackTrace();
            Assert.assertTrue(e instanceof TRpcException);
        }

        StandardPackage pkg3 = new StandardPackage();
        pkg3.setBodyBytes(new byte[]{1, 2, 3, 4});
        pkg3.setHeadBytes(head);
        pkg3.getFrame().setHeadSize(pkg.getHeadBytes().length);
        pkg3.getFrame()
                .setSize(pkg.getBodyBytes().length + pkg.getHeadBytes().length
                        + StandardFrame.FRAME_SIZE);
        pkg3.getFrame().setMagic((short) 0x930);
        pkg3.getFrame().setState((byte) 1);
        pkg3.getFrame().setStreamId(100);
        pkg3.getFrame().setType((byte) 2);
        ProtocolConfig config3 = new ProtocolConfig();
        config3.setIp("127.0.0.1");
        config3.setPort(54321);
        config3.setDefault();
        config3.setPayload(pkg.getBodyBytes().length + pkg.getHeadBytes().length);
        NettyChannel channel3 = new NettyChannel(null, config3);
        NettyChannelBuffer nettyChannelBuffer3 =
                new NettyChannelBuffer(UnpooledByteBufAllocator.DEFAULT.buffer(65535));
        pkg3.write(nettyChannelBuffer3);
        try {
            StandardPackage newPkg3 = (StandardPackage) StandardPackage
                    .decode(channel3, nettyChannelBuffer3, true);
            Assert.fail("pkg length > payload");
        } catch (Exception e) {
            e.printStackTrace();
            Assert.assertTrue(e instanceof TRpcException);
        }
    }
}
