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

import static com.tencent.trpc.transport.http.common.Constants.HTTP1_SCHEME;
import static com.tencent.trpc.transport.http.common.Constants.HTTPS_SCHEME;
import static com.tencent.trpc.transport.http.common.Constants.KEYSTORE_PASS;
import static com.tencent.trpc.transport.http.common.Constants.KEYSTORE_PATH;

import com.tencent.trpc.core.common.config.ProtocolConfig;
import com.tencent.trpc.transport.http.HttpExecutor;
import org.eclipse.jetty.http2.HTTP2Cipher;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.util.ssl.SslContextFactory;


/**
 * A HTTPS server that uses the TLS protocol by default.
 * Inherited from {@link JettyHttpServer}, the {@link JettyHttpServer#getServerConnector} method has been
 * overridden to support the HTTPS protocol, while other configurations and loading are consistent
 * with {@link JettyHttpServer}.
 * The TLS protocol configuration is consistent, and subclasses can override the
 * {@link JettyHttpsServer#buildServerConnector} method for configuration. For more details,
 * please refer to {@link JettyHttp2Server}.
 */
public class JettyHttpsServer extends JettyHttpServer {

    public JettyHttpsServer(ProtocolConfig config, HttpExecutor executor) {
        super(config, executor);
    }

    @Override
    public ServerConnector getServerConnector(Server server) {

        ProtocolConfig config = getConfig();

        // 1. Configure SSL certificate, which uses TLS by default and is suitable for HTTPS and h2.
        SslContextFactory.Server sslContextFactory = new SslContextFactory.Server();
        sslContextFactory.setKeyStorePath(String.valueOf(config.getExtMap().get(KEYSTORE_PATH)));
        sslContextFactory
                .setKeyStorePassword(String.valueOf(config.getExtMap().get(KEYSTORE_PASS)));
        sslContextFactory.setCipherComparator(HTTP2Cipher.COMPARATOR);
        sslContextFactory.setProvider("Conscrypt");

        // 2. Configure http protocol
        HttpConfiguration httpConfig = new HttpConfiguration();
        httpConfig.setSecureScheme(HTTPS_SCHEME);
        httpConfig.setSecurePort(config.getPort());

        // 3. Configure https protocol
        HttpConfiguration httpsConfig = new HttpConfiguration(httpConfig);
        httpsConfig.addCustomizer(new SecureRequestCustomizer());

        // 4. Configure server connector
        return buildServerConnector(server, config, sslContextFactory,
                httpConfig, httpsConfig);
    }

    /**
     * Build a Jetty's {@link ServerConnector}.
     *
     * @param server Jetty HTTP Servlet Server.
     * @param config trpc protocol config
     * @param sslContextFactory ssl protocol factory
     * @param httpConfig http protocol config
     * @param httpsConfig https protocol config
     * @return the Jetty's {@link ServerConnector}
     */
    protected ServerConnector buildServerConnector(Server server, ProtocolConfig config,
            SslContextFactory.Server sslContextFactory, HttpConfiguration httpConfig,
            HttpConfiguration httpsConfig) {
        // 1. Configure ssl factory
        SslConnectionFactory ssl = new SslConnectionFactory(sslContextFactory, HTTP1_SCHEME);

        // 2. Configure the connector of https
        ServerConnector http2Connector = new ServerConnector(server, ssl,
                new HttpConnectionFactory(httpsConfig));
        http2Connector.setHost(config.getIp());
        http2Connector.setPort(config.getPort());
        http2Connector.setAcceptQueueSize(config.getMaxConns());
        return http2Connector;
    }

}
