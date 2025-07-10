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

package com.tencent.trpc.transport.http.support.jetty;

import com.tencent.trpc.core.common.config.ProtocolConfig;
import com.tencent.trpc.core.extension.Extension;
import com.tencent.trpc.transport.http.HttpExecutor;
import org.eclipse.jetty.http2.server.HTTP2CServerConnectionFactory;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;

/**
 * A server based on the HTTP2C (clear text H2) protocol, compatible with both HTTP1.1 and HTTP2C protocols.
 * Inherited from {@link JettyHttpServer}, {@link JettyHttpServer#getServerConnector} is overridden to support
 * the H2 protocol, while other configurations and loading remain consistent with {@link JettyHttpServer}.
 */
@Extension(JettyHttpServer.NAME)
public class JettyHttp2cServer extends JettyHttpServer {

    public JettyHttp2cServer(ProtocolConfig config, HttpExecutor executor) {
        super(config, executor);
    }

    @Override
    public ServerConnector getServerConnector(Server server) {
        ProtocolConfig config = getConfig();
        HttpConfiguration httpConfig = new HttpConfiguration();

        HttpConnectionFactory h1 = new HttpConnectionFactory(httpConfig);
        HTTP2CServerConnectionFactory h2c = new HTTP2CServerConnectionFactory(httpConfig);

        // It is compatible with both HTTP1.1 and HTTP2C protocols. The browser accesses using HTTP1.1,
        // while internal RPC calls use HTTP2C.
        ServerConnector http2Connector = new ServerConnector(server, h1, h2c);

        http2Connector.setHost(config.getIp());
        http2Connector.setPort(config.getPort());
        http2Connector.setAcceptQueueSize(config.getMaxConns());
        return http2Connector;
    }

}
