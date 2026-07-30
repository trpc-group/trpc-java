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
import com.tencent.trpc.core.logger.Logger;
import com.tencent.trpc.core.logger.LoggerFactory;
import com.tencent.trpc.core.rpc.AbstractRpcClient;
import com.tencent.trpc.core.rpc.ConsumerInvoker;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import org.apache.http.config.SocketConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;

/**
 * HTTP/1.1 protocol client.
 *
 * <p><b>Long-connection mode</b>. Connections are pooled by Apache
 * {@link PoolingHttpClientConnectionManager} and reused across requests via HTTP/1.1
 * {@code Connection: keep-alive}. The following safeguards are wired by default to keep the
 * pool healthy in long-running processes — especially when the server, an intermediary load
 * balancer or a NAT silently terminates idle keep-alive sockets:</p>
 * <ul>
 *     <li>{@code maxTotal} / {@code maxPerRoute} sized from {@code protocolConfig.getMaxConns()}
 *         so the pool never silently caps at HttpClient's tiny default (25/5);</li>
 *     <li>{@code validateAfterInactivity}: re-checks a pooled connection's liveness before reuse
 *         when it has been idle for at least {@code HttpConstants.VALIDATE_AFTER_INACTIVITY_MS}ms
 *         (avoids the classic "stale connection / {@code NoHttpResponseException}" against a
 *         server-side half-closed keep-alive socket);</li>
 *     <li>{@code evictExpiredConnections} + {@code evictIdleConnections}: a daemon thread evicts
 *         connections idle longer than {@code protocolConfig.getIdleTimeout()} (milliseconds),
 *         freeing OS file descriptors; a non-positive idle timeout disables idle eviction;</li>
 *     <li>{@code keepAliveStrategy} with a {@value #FALLBACK_KEEPALIVE_MINUTES}min ceiling: when
 *         the server omits {@code Keep-Alive: timeout=N} we still cap connection age client-side,
 *         which beats most NAT idle timers (typical 5–15min);</li>
 *     <li>{@code SO_KEEPALIVE=true} on every pooled socket: a last-resort path so the OS
 *         eventually surfaces dead peers in the worst-case "host pulled the plug / kernel
 *         panic" black-hole scenario where no FIN/RST is ever sent. Linux defaults are
 *         conservative (~2h11min) — the application-layer paths above will normally fire
 *         long before this kicks in.</li>
 * </ul>
 */
public class HttpRpcClient extends AbstractRpcClient {

    private static final Logger logger = LoggerFactory.getLogger(HttpRpcClient.class);

    /**
     * Fallback {@code Keep-Alive} duration applied client-side when the server response omits
     * a {@code Keep-Alive: timeout=N} hint. Picked to be shorter than typical NAT / LB idle
     * timers (5–15 minutes) so we never hold a connection past the point where some hop on
     * the path has silently dropped it.
     */
    private static final int FALLBACK_KEEPALIVE_MINUTES = 5;

    private CloseableHttpClient httpClient;

    public HttpRpcClient(ProtocolConfig config) {
        setConfig(config);
    }

    @Override
    protected void doOpen() {
        PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
        int maxConns = protocolConfig.getMaxConns();
        // Set the maximum number of connections.
        cm.setMaxTotal(maxConns);
        // If there is only one route, the maximum number of connections for a single route is the same
        // as the maximum number of connections for the entire connection pool.
        cm.setDefaultMaxPerRoute(maxConns);
        // Re-validate idle pooled connections before reuse so we do not send a request through a
        // socket the server has already half-closed.
        cm.setValidateAfterInactivity(VALIDATE_AFTER_INACTIVITY_MS);
        // Enable TCP-level keep-alive (SO_KEEPALIVE) on every pooled socket. This gives the OS a
        // last-resort path for surfacing dead peers in the worst case where the application-layer
        // Keep-Alive timer (5min) does not fire — typically the "host pulled the plug / kernel
        // panic" black-hole scenario where no FIN or RST is ever sent. Linux defaults are
        // conservative (~2h11min total: 7200s idle + 9 × 75s probes) so this isn't a fast path,
        // but it's a pure win — the alternative is socket lingering until {@code tcp_retries2}
        // (~15min) catches up. Operators who care about faster recovery should tune sysctl
        // {@code net.ipv4.tcp_keepalive_time/intvl/probes} host-wide; Apache HttpClient 4.x's
        // blocking IO does not expose per-socket configuration of those values via SocketOptions,
        // hence the OS default applies.
        cm.setDefaultSocketConfig(SocketConfig.custom()
                .setSoKeepAlive(true)
                .build());
        HttpClientBuilder builder = HttpClients.custom()
                .setConnectionManager(cm)
                // Background eviction of stale & long-idle connections; keeps the pool tidy in
                // long-running processes without affecting hot connections.
                .evictExpiredConnections()
                // Cap server-suggested keep-alive at FALLBACK_KEEPALIVE_MINUTES. When the server
                // omits a Keep-Alive header HttpClient defaults to "infinite" — that loses to any
                // intermediary NAT / LB silently dropping idle sockets, manifesting as the
                // dreaded NoHttpResponseException on the next request.
                .setKeepAliveStrategy(HttpRpcClient::resolveKeepAliveDuration);
        // Idle-connection eviction threshold driven by protocolConfig.getIdleTimeout() (ms);
        // a non-positive value disables idle eviction, matching the cluster-level idle scanner.
        Integer idleTimeoutMs = protocolConfig.getIdleTimeout();
        if (idleTimeoutMs != null && idleTimeoutMs > 0) {
            builder.evictIdleConnections((long) idleTimeoutMs, TimeUnit.MILLISECONDS);
        }
        httpClient = builder.build();
    }

    /**
     * Capped keep-alive duration: prefer the {@code Keep-Alive: timeout=N} hint from the server
     * (clamped at {@value #FALLBACK_KEEPALIVE_MINUTES}min) and fall back to that ceiling when
     * the server omits the hint or the value is malformed. Package-private so unit tests can
     * exercise the parsing branches without spinning up a real HTTP server.
     *
     * @param response the inbound HTTP response (only the header is read)
     * @param context unused, present to satisfy {@code ConnectionKeepAliveStrategy}
     * @return the keep-alive duration in milliseconds
     */
    public static long resolveKeepAliveDuration(org.apache.http.HttpResponse response,
            org.apache.http.protocol.HttpContext context) {
        long fallbackMs = TimeUnit.MINUTES.toMillis(FALLBACK_KEEPALIVE_MINUTES);
        org.apache.http.Header h = response.getFirstHeader("Keep-Alive");
        if (h != null) {
            for (org.apache.http.HeaderElement el : h.getElements()) {
                if ("timeout".equalsIgnoreCase(el.getName()) && el.getValue() != null) {
                    try {
                        long server = Long.parseLong(el.getValue()) * 1000L;
                        return Math.min(server, fallbackMs);
                    } catch (NumberFormatException ignore) {
                        // fall through to the fallback
                    }
                }
            }
        }
        return fallbackMs;
    }

    @Override
    protected void doClose() {
        if (httpClient != null) {
            try {
                httpClient.close();
            } catch (IOException e) {
                logger.error("close httpClient of " + protocolConfig.getIp() + ":"
                        + protocolConfig.getPort() + " failed", e);
            }
        }
    }

    @Override
    public <T> ConsumerInvoker<T> createInvoker(ConsumerConfig<T> consumerConfig) {
        return new HttpConsumerInvoker<>(this, consumerConfig, protocolConfig);
    }

    public CloseableHttpClient getHttpClient() {
        return httpClient;
    }
}
