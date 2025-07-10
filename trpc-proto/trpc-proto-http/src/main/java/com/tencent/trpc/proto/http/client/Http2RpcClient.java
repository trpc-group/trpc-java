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

package com.tencent.trpc.proto.http.client;

import static com.tencent.trpc.transport.http.common.Constants.KEYSTORE_PASS;
import static com.tencent.trpc.transport.http.common.Constants.KEYSTORE_PATH;

import com.tencent.trpc.core.common.config.ProtocolConfig;
import com.tencent.trpc.core.logger.Logger;
import com.tencent.trpc.core.logger.LoggerFactory;
import java.io.File;
import javax.net.ssl.SSLContext;
import org.apache.hc.client5.http.impl.async.HttpAsyncClients;
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManager;
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManagerBuilder;
import org.apache.hc.core5.http2.HttpVersionPolicy;
import org.apache.hc.core5.http2.ssl.ConscryptClientTlsStrategy;
import org.apache.hc.core5.ssl.SSLContexts;

/**
 * HTTP 2 protocol client.
 */
public class Http2RpcClient extends Http2cRpcClient {

    private static final Logger logger = LoggerFactory.getLogger(HttpRpcClient.class);

    /**
     * The protocol type used for interaction with the server, such as HTTP1, H2, or protocol negotiation.
     * In trpc, the interaction is forced to use H2 or HTTP1 protocol based on the configuration.
     */
    protected HttpVersionPolicy clientVersionPolicy;

    public Http2RpcClient(ProtocolConfig config) {
        super(config);
        this.clientVersionPolicy = HttpVersionPolicy.FORCE_HTTP_2;
    }

    @Override
    protected void doOpen() {
        try {
            String keyStorePath = String
                    .valueOf(getProtocolConfig().getExtMap().get(KEYSTORE_PATH));
            String keyStorePass = String
                    .valueOf(getProtocolConfig().getExtMap().get(KEYSTORE_PASS));

            // Refer to the sample code of Apache HttpClient 5.0：HTTP/2 ALPN support
            // https://hc.apache.org/httpcomponents-client-5.0.x/examples-async.html

            // 1. Configure TLS certificate.
            final SSLContext sslContext = SSLContexts.custom()
                    .loadTrustMaterial(new File(keyStorePath), keyStorePass.toCharArray())
                    .build();

            // 2. Configure connection pool.
            final PoolingAsyncClientConnectionManager cm = PoolingAsyncClientConnectionManagerBuilder
                    .create().useSystemProperties()
                    .setTlsStrategy(new ConscryptClientTlsStrategy(sslContext))
                    .build();

            // 3. Configure the client to force HTTPS protocol to use HTTP1 communication and H2 protocol
            // to use H2 communication.
            httpAsyncClient = HttpAsyncClients.custom()
                    .setVersionPolicy(this.clientVersionPolicy).setConnectionManager(cm)
                    .build();
            // 4. Start the client.
            httpAsyncClient.start();
        } catch (Exception e) {
            logger.error("httpAsyncClient error: ", e);
        }
    }
}
