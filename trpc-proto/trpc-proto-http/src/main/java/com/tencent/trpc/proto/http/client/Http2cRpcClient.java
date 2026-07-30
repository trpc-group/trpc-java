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

import com.tencent.trpc.core.common.config.ConsumerConfig;
import com.tencent.trpc.core.common.config.ProtocolConfig;
import com.tencent.trpc.core.exception.ErrorCode;
import com.tencent.trpc.core.exception.TRpcException;
import com.tencent.trpc.core.logger.Logger;
import com.tencent.trpc.core.logger.LoggerFactory;
import com.tencent.trpc.core.rpc.AbstractRpcClient;
import com.tencent.trpc.core.rpc.ConsumerInvoker;
import java.io.IOException;
import org.apache.hc.client5.http.impl.async.CloseableHttpAsyncClient;
import org.apache.hc.client5.http.impl.async.HttpAsyncClientBuilder;
import org.apache.hc.client5.http.impl.async.HttpAsyncClients;
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManager;
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManagerBuilder;
import org.apache.hc.core5.pool.PoolReusePolicy;
import org.apache.hc.core5.reactor.IOReactorConfig;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;

/**
 * HTTP/2 cleartext (h2c) protocol client.
 *
 * <p><b>Long-connection mode</b>. Built on Apache HttpClient 5.x async + HTTP/2 multiplexing —
 * a single TCP connection carries many concurrent RPC streams. The connection manager is
 * tuned identically in spirit to {@link HttpRpcClient}:</p>
 * <ul>
 *     <li>{@code maxConnTotal} / {@code maxConnPerRoute} sized from
 *         {@code protocolConfig.getMaxConns()} so the pool never silently caps at the tiny
 *         HttpClient defaults;</li>
 *     <li>{@code validateAfterInactivity}: re-check on idle connections before reuse
 *         (see {@code HttpConstants.VALIDATE_AFTER_INACTIVITY_MS});</li>
 *     <li>{@code evictExpired} + {@code evictIdle}: daemon-thread cleanup of idle connections,
 *         the idle threshold taken from {@code protocolConfig.getIdleTimeout()} (milliseconds);
 *         a non-positive value disables idle eviction;</li>
 *     <li>{@code SO_KEEPALIVE} enabled on the IOReactor so the OS itself surfaces dead peers
 *         on platforms where it is configured (Linux ~2h default, far quicker with kernel
 *         tuning).</li>
 * </ul>
 */
public class Http2cRpcClient extends AbstractRpcClient {

    private static final Logger logger = LoggerFactory.getLogger(Http2cRpcClient.class);

    /**
     * Asynchronous HTTP client
     */
    protected CloseableHttpAsyncClient httpAsyncClient;

    public Http2cRpcClient(ProtocolConfig config) {
        setConfig(config);
    }

    /**
     * Configure and start the client. The pool is sized from {@code maxConns}; idle / expired
     * connections are reaped in the background; dead-peer detection happens via SO_KEEPALIVE.
     * Connections have no hard TTL — they live until idle-evicted, validated-out or closed by
     * the peer.
     *
     * @throws TRpcException if the underlying HttpClient fails to start; surfacing this lets
     *         {@link AbstractRpcClient#open()} mark the lifecycle FAILED instead of leaving a
     *         half-built client cached.
     */
    @Override
    protected void doOpen() throws TRpcException {
        try {
            int maxConns = protocolConfig.getMaxConns();
            PoolingAsyncClientConnectionManager cm = PoolingAsyncClientConnectionManagerBuilder
                    .create()
                    .setMaxConnTotal(maxConns)
                    .setMaxConnPerRoute(maxConns)
                    .setConnPoolPolicy(PoolReusePolicy.LIFO)
                    .setValidateAfterInactivity(TimeValue.ofMilliseconds(VALIDATE_AFTER_INACTIVITY_MS))
                    .build();

            HttpAsyncClientBuilder builder = HttpAsyncClients.custom()
                    .setConnectionManager(cm)
                    // Enable SO_KEEPALIVE on every socket so the OS eventually reaps dead peers
                    // even when no idle eviction has fired.
                    .setIOReactorConfig(IOReactorConfig.custom()
                            .setSoKeepAlive(true)
                            .setSoTimeout(Timeout.ofSeconds(0))
                            .build())
                    .evictExpiredConnections()
                    .setVersionPolicy(org.apache.hc.core5.http2.HttpVersionPolicy.FORCE_HTTP_2);
            applyIdleEviction(builder);
            httpAsyncClient = builder.build();
            httpAsyncClient.start();
        } catch (Exception e) {
            // Surface the failure so the lifecycle moves to FAILED and the cached cluster slot
            // is not populated with a half-built client.
            String desc = protocolConfig != null ? protocolConfig.toSimpleString() : "<null>";
            throw TRpcException.newFrameException(ErrorCode.TRPC_CLIENT_CONNECT_ERR,
                    "open http2c client (" + desc + ") failed", e);
        }
    }

    /**
     * Apply background idle-connection eviction to the given builder using
     * {@code protocolConfig.getIdleTimeout()} (milliseconds) as the idle threshold. A
     * {@code null} or non-positive idle timeout disables idle eviction, matching the framework
     * convention used by the cluster-level idle scanner.
     *
     * @param builder the async client builder to configure (shared with the H2/HTTPS subclass)
     */
    protected void applyIdleEviction(HttpAsyncClientBuilder builder) {
        Integer idleTimeoutMs = protocolConfig.getIdleTimeout();
        if (idleTimeoutMs != null && idleTimeoutMs > 0) {
            builder.evictIdleConnections(TimeValue.ofMilliseconds(idleTimeoutMs));
        }
    }

    /**
     * Close the client.
     */
    @Override
    protected void doClose() {
        if (httpAsyncClient != null) {
            try {
                httpAsyncClient.close();
            } catch (IOException e) {
                logger.error("close httpClient of " + protocolConfig.getIp() + ":"
                        + protocolConfig.getPort() + " failed", e);
            }
        }
    }

    /**
     * Generate an invoker and hand it over to the proxy to generate a proxy object.
     * The chain processing of the invoker is wrapped outside.
     *
     * @param consumerConfig the configuration related to the interface set by the method invoker,
     * such as timeout duration, filter configuration, etc.
     */
    @Override
    public <T> ConsumerInvoker<T> createInvoker(ConsumerConfig<T> consumerConfig) {
        return new Http2ConsumerInvoker<>(this, consumerConfig, protocolConfig);
    }

    public CloseableHttpAsyncClient getHttpAsyncClient() {
        return httpAsyncClient;
    }
}
