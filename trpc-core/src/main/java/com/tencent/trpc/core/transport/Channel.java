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

package com.tencent.trpc.core.transport;

import com.tencent.trpc.core.common.config.ProtocolConfig;
import com.tencent.trpc.core.exception.TransportException;
import java.net.InetSocketAddress;
import java.util.concurrent.CompletionStage;

/**
 * Similar to Netty channel, wraps the underlying channel, represents the abstraction of network I/O.
 */
public interface Channel {

    /**
     * Send data through the channel.
     */
    CompletionStage<Void> send(Object message) throws TransportException;

    /**
     * Close operation.
     */
    CompletionStage<Void> close();

    /**
     * Check if the close operation has been executed.
     */
    boolean isClosed();

    /**
     * Check if the connection is established.
     */
    boolean isConnected();

    /**
     * Get the remote address (returns null in UDP scenarios).
     */
    InetSocketAddress getRemoteAddress();

    /**
     * Get the local address.
     */
    InetSocketAddress getLocalAddress();

    /**
     * Get the associated configuration information for the instance.
     */
    ProtocolConfig getProtocolConfig();

}
