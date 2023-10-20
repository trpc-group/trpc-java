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

package com.tencent.trpc.core.stream.transport.codec;


import io.netty.buffer.ByteBuf;

/**
 * Used to decode and extract the entire frame of each send-receive packet,
 * returning the complete frame {@link ByteBuf} for downstream use.
 */
public interface FrameDecoder {

    /**
     * Used to decode a frame of data
     *
     * @param inputStream aggregated input data
     * @return The decoded frame of data. It is important to note that the input {@link ByteBuf}
     * should not be directly returned, and care should be taken with reference counting and
     * the {@link ByteBuf} read pointer position.
     */
    ByteBuf decode(ByteBuf inputStream);

}
