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

package com.tencent.trpc.core.stream.transport;

import com.tencent.trpc.core.stream.Closeable;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import reactor.core.publisher.Flux;

/**
 * A duplex rpc connection
 */
public interface RpcConnection extends Closeable {

    /**
     * Send a frame of data
     *
     * @param frame a entire frame of data
     */
    void send(ByteBuf frame);

    /**
     * Receive frames of data from remote
     *
     * @return a stream of received frames of data
     */
    Flux<ByteBuf> receive();

    /**
     * Allocate a {@link ByteBufAllocator}, can be used to create {@link ByteBuf}
     *
     * @return a {@link ByteBufAllocator}
     */
    ByteBufAllocator alloc();

}
