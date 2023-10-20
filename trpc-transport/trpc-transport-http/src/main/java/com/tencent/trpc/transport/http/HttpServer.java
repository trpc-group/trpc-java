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

package com.tencent.trpc.transport.http;

import com.tencent.trpc.core.common.config.ProtocolConfig;
import com.tencent.trpc.core.exception.TransportException;
import java.net.InetSocketAddress;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;

public interface HttpServer {

    /**
     * Start server
     */
    void open() throws TransportException;

    /**
     * Get the http executor
     */
    HttpExecutor getExecutor();

    /**
     * Get protocol config
     */
    ProtocolConfig getConfig();

    /**
     * Get locol ip address
     */
    InetSocketAddress getLocalAddress();

    /**
     * Whether has bound
     */
    boolean isBound();

    /**
     * Whether has closed
     */
    boolean isClosed();

    /**
     * Close server
     */
    void close();

    /**
     * Get the server connector
     */
    ServerConnector getServerConnector(Server server);

}
