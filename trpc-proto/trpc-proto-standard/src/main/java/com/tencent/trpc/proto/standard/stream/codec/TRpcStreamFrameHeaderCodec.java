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

package com.tencent.trpc.proto.standard.stream.codec;

import com.tencent.trpc.proto.standard.common.StandardFrame;
import com.tencent.trpc.proto.standard.common.TRPCProtocol.TrpcDataFrameType;
import com.tencent.trpc.proto.standard.common.TRpcFrameType;
import io.netty.buffer.ByteBuf;

/**
 * TRPC protocol header decoding utility.
 */
public class TRpcStreamFrameHeaderCodec {

    public static final int TRPC_FIX_HEADER_LENGTH = StandardFrame.FRAME_SIZE;

    /**
     * Parse the protocol magic number.
     *
     * @param frameHeader data frame
     * @return protocol magic number
     */
    public static short magic(ByteBuf frameHeader) {
        frameHeader.markReaderIndex();
        short magic = frameHeader.readShort();
        frameHeader.resetReaderIndex();
        return magic;
    }

    /**
     * Frame size.
     *
     * @param frameHeader data frame
     * @return frame size
     */
    public static int frameSize(ByteBuf frameHeader) {
        frameHeader.markReaderIndex();
        frameHeader.skipBytes(Short.BYTES + Byte.BYTES + Byte.BYTES);
        int frameSize = frameHeader.readInt();
        frameHeader.resetReaderIndex();
        return frameSize;
    }

    /**
     * Decode stream id.
     *
     * @param frameHeader data frame
     * @return stream id
     */
    public static int streamId(ByteBuf frameHeader) {
        frameHeader.markReaderIndex();
        frameHeader.skipBytes(Short.BYTES + Byte.BYTES + Byte.BYTES + Integer.BYTES + Short.BYTES);
        int streamId = frameHeader.readInt();
        frameHeader.resetReaderIndex();
        return streamId;
    }

    /**
     * Decode frame type.
     *
     * @param frameHeader data frame
     * @return frame type
     */
    public static TRpcFrameType frameType(ByteBuf frameHeader) {
        frameHeader.markReaderIndex();
        frameHeader.skipBytes(Short.BYTES);
        int dataType = frameHeader.readByte();
        byte encodedType = frameHeader.readByte();
        frameHeader.resetReaderIndex();
        if (dataType == TrpcDataFrameType.TRPC_STREAM_FRAME_VALUE) {
            return TRpcFrameType.fromEncodedType(encodedType);
        }
        throw new IllegalArgumentException("Unknown data type");
    }
}
