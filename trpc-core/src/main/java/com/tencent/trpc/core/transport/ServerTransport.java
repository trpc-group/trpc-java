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

package com.tencent.trpc.core.transport;

import com.tencent.trpc.core.common.config.ProtocolConfig;
import com.tencent.trpc.core.exception.TransportException;
import java.net.InetSocketAddress;
import java.util.Set;

/**
 * Server network client interface.
 */
public interface ServerTransport {

    /**
     * Open the transport.
     */
    void open() throws TransportException;

    /**
     * Get all connections established with the server.
     */
    Set<Channel> getChannels();

    /**
     * Close the transport.
     */
    void close();

    /**
     * Check if the transport is bound to a port.
     */
    boolean isBound();

    /**
     * Check if the transport is closed.
     */
    boolean isClosed();

    /**
     * Get the channel handler.
     */
    ChannelHandler getChannelHandler();

    /**
     * Get the local address.
     */
    InetSocketAddress getLocalAddress();

    /**
     * Get the associated protocol configuration.
     */
    ProtocolConfig getProtocolConfig();

}