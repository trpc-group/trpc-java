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
import java.util.concurrent.CompletionStage;

/**
 * Client network client interface.
 */
public interface ClientTransport {

    /**
     * Open the transport.
     */
    void open() throws TransportException;

    /**
     * Asynchronously send data.
     */
    CompletionStage<Void> send(Object message) throws TransportException;

    /**
     * Get the channel.
     */
    CompletionStage<Channel> getChannel() throws TransportException;

    /**
     * All channels established by the client transport.
     */
    Set<Channel> getChannels();

    /**
     * Close the connection.
     */
    void close();

    /**
     * Indicates whether the client has established a connection.
     */
    boolean isConnected();

    /**
     * Check if the transport is closed.
     */
    boolean isClosed();

    /**
     * Get the channel handler.
     */
    ChannelHandler getChannelHandler();

    /**
     * Get the remote address.
     */
    InetSocketAddress getRemoteAddress();

    /**
     * Get the associated protocol configuration.
     */
    ProtocolConfig getProtocolConfig();

}
