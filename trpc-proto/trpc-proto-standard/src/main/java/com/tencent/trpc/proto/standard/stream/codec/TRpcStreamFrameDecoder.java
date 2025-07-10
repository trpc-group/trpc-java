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

import com.tencent.trpc.core.exception.ErrorCode.Stream;
import com.tencent.trpc.core.exception.TRpcException;
import com.tencent.trpc.core.stream.transport.codec.FrameDecoder;
import com.tencent.trpc.proto.standard.common.StandardFrame;
import io.netty.buffer.ByteBuf;

/**
 * TRPC protocol frame parser, which can extract a complete frame ByteBuf from binary data.
 */
public class TRpcStreamFrameDecoder implements FrameDecoder {


    /**
     * Decode a frame of data.
     *
     * @param inputStream aggregated input data
     * @return a complete frame ByteBuf data
     */
    @Override
    public ByteBuf decode(ByteBuf inputStream) {
        if (isFrameHeaderNotReady(inputStream)) {
            return null;
        }
        checkMagic(inputStream);
        int frameSize = TRpcStreamFrameHeaderCodec.frameSize(inputStream);
        if (isFrameReady(inputStream, frameSize)) {
            return null;
        }
        return readFrame(inputStream, frameSize);
    }

    /**
     * Verify if the frame header magic number is 0x930
     *
     * @param inputStream data frame
     */
    private void checkMagic(ByteBuf inputStream) {
        short magic = TRpcStreamFrameHeaderCodec.magic(inputStream);
        // if the magic number is incorrect, throw an exception
        if (StandardFrame.TRPC_MAGIC != magic) {
            throw TRpcException.newFrameException(Stream.FRAME_DECODE_MAGIC_ERR.getStatusCode(),
                    Stream.FRAME_DECODE_MAGIC_ERR.getMessage());
        }
    }

    /**
     * Check if the frame header data is ready.
     *
     * @param inputStream data frame
     * @return true if the frame header is ready, otherwise return false
     */
    private boolean isFrameHeaderNotReady(ByteBuf inputStream) {
        int readyFrameHeaderSize = inputStream.readableBytes();
        return readyFrameHeaderSize < StandardFrame.FRAME_SIZE;
    }

    /**
     * Check if a frame of data is ready.
     *
     * @param inputStream data frame
     * @return true if the frame is ready, otherwise return false
     */
    private boolean isFrameReady(ByteBuf inputStream, int frameSize) {
        return inputStream.readableBytes() < frameSize;
    }

    /**
     * Read a frame of data.
     *
     * @param inputStream data frame
     * @param frameSize a data frame size
     * @return a data frame
     */
    private ByteBuf readFrame(ByteBuf inputStream, int frameSize) {
        int start = inputStream.readerIndex();
        inputStream.skipBytes(frameSize);
        return inputStream.retainedSlice(start, frameSize);
    }
}
