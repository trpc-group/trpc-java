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

package com.tencent.trpc.transport.http.support.jetty;

import static com.tencent.trpc.transport.http.common.Constants.HTTP2_SCHEME;

import com.tencent.trpc.core.common.config.ProtocolConfig;
import com.tencent.trpc.transport.http.HttpExecutor;
import org.eclipse.jetty.alpn.server.ALPNServerConnectionFactory;
import org.eclipse.jetty.http2.server.HTTP2ServerConnectionFactory;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.util.ssl.SslContextFactory;


/**
 * The server is based on the H2 protocol and by default uses the TLS protocol and ALPN protocol.
 * The ALPN (Application-Layer Protocol Negotiation) protocol is an extension of the TLS protocol
 * that enables application-layer protocol negotiation.
 * There may be compatibility issues with ALPN under JDK8,
 * see: https://www.eclipse.org/jetty/documentation/current/alpn-chapter.html
 * Inherited from {@link JettyHttpsServer}, the {@link JettyHttpsServer#buildServerConnector} method
 * has been overridden to support the H2 protocol, while other configurations and loading are consistent
 * with {@link JettyHttpsServer}.
 */
public class JettyHttp2Server extends JettyHttpsServer {

    public JettyHttp2Server(ProtocolConfig config, HttpExecutor executor) {
        super(config, executor);
    }

    @Override
    protected ServerConnector buildServerConnector(Server server, ProtocolConfig config,
            SslContextFactory sslContextFactory, HttpConfiguration httpConfig,
            HttpConfiguration httpsConfig) {
        // 1. Config the h2 factory
        HTTP2ServerConnectionFactory h2 = new HTTP2ServerConnectionFactory(httpsConfig);

        // 2. Config the alpn factory. There may be compatibility issues with ALPN under JDK8.
        ALPNServerConnectionFactory alpn = new ALPNServerConnectionFactory();
        alpn.setDefaultProtocol(HTTP2_SCHEME);

        // 3. Config ssl factory
        SslConnectionFactory ssl = new SslConnectionFactory(sslContextFactory, alpn.getProtocol());

        // 4. Config the connector of h2
        ServerConnector http2Connector = new ServerConnector(server, ssl, alpn, h2,
                new HttpConnectionFactory(httpConfig));
        http2Connector.setHost(config.getIp());
        http2Connector.setPort(config.getPort());
        http2Connector.setAcceptQueueSize(config.getMaxConns());

        return http2Connector;
    }
}
