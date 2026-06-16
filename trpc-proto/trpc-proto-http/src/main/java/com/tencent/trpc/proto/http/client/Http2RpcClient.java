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

import static com.tencent.trpc.proto.http.common.HttpConstants.VALIDATE_AFTER_INACTIVITY_MS;
import static com.tencent.trpc.transport.http.common.Constants.KEYSTORE_PASS;
import static com.tencent.trpc.transport.http.common.Constants.KEYSTORE_PATH;

import com.tencent.trpc.core.common.config.ProtocolConfig;
import com.tencent.trpc.core.exception.ErrorCode;
import com.tencent.trpc.core.exception.TRpcException;
import com.tencent.trpc.core.logger.Logger;
import com.tencent.trpc.core.logger.LoggerFactory;
import java.io.File;
import javax.net.ssl.SSLContext;
import org.apache.hc.client5.http.impl.async.HttpAsyncClientBuilder;
import org.apache.hc.client5.http.impl.async.HttpAsyncClients;
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManager;
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManagerBuilder;
import org.apache.hc.core5.http2.HttpVersionPolicy;
import org.apache.hc.core5.http2.ssl.ConscryptClientTlsStrategy;
import org.apache.hc.core5.pool.PoolReusePolicy;
import org.apache.hc.core5.reactor.IOReactorConfig;
import org.apache.hc.core5.ssl.SSLContexts;
import org.apache.hc.core5.util.TimeValue;

/**
 * HTTP/2 (TLS) protocol client. Inherits the long-connection connection-pool setup from
 * {@link Http2cRpcClient}; the differences are the TLS handshake and the explicit
 * {@link HttpVersionPolicy} negotiation.
 *
 * <p>The connection manager is sized and tuned identically to {@link Http2cRpcClient}: pool
 * limits derived from {@code maxConns}, idle / expired eviction and SO_KEEPALIVE.</p>
 */
public class Http2RpcClient extends Http2cRpcClient {

    private static final Logger logger = LoggerFactory.getLogger(Http2RpcClient.class);

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
    protected void doOpen() throws TRpcException {
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
            int maxConns = protocolConfig.getMaxConns();
            final PoolingAsyncClientConnectionManager cm = PoolingAsyncClientConnectionManagerBuilder
                    .create().useSystemProperties()
                    .setTlsStrategy(new ConscryptClientTlsStrategy(sslContext))
                    .setMaxConnTotal(maxConns)
                    .setMaxConnPerRoute(maxConns)
                    .setConnPoolPolicy(PoolReusePolicy.LIFO)
                    .setValidateAfterInactivity(TimeValue.ofMilliseconds(VALIDATE_AFTER_INACTIVITY_MS))
                    .build();

            // 3. Configure the client to force HTTPS protocol to use HTTP1 communication and H2 protocol
            // to use H2 communication.
            HttpAsyncClientBuilder builder = HttpAsyncClients.custom()
                    .setVersionPolicy(this.clientVersionPolicy)
                    .setConnectionManager(cm)
                    .setIOReactorConfig(IOReactorConfig.custom()
                            .setSoKeepAlive(true)
                            .build())
                    .evictExpiredConnections();
            applyIdleEviction(builder);
            httpAsyncClient = builder.build();
            // 4. Start the client.
            httpAsyncClient.start();
        } catch (Exception e) {
            logger.error("open https/h2 client ({}) failed",
                    getProtocolConfig().toSimpleString(), e);
            throw TRpcException.newFrameException(ErrorCode.TRPC_CLIENT_CONNECT_ERR,
                    "open https/h2 client (" + getProtocolConfig().toSimpleString() + ") failed",
                    e);
        }
    }

    /**
     * Defensive close ensuring the inherited handle is released. We let the parent's
     * {@code doClose} do the actual cleanup; this method exists in case future TLS-only
     * resources need explicit release.
     */
    @Override
    protected void doClose() {
        super.doClose();
    }
}
