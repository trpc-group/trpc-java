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

import com.tencent.trpc.core.common.config.ConsumerConfig;
import com.tencent.trpc.core.common.config.ProtocolConfig;
import com.tencent.trpc.core.logger.Logger;
import com.tencent.trpc.core.logger.LoggerFactory;
import com.tencent.trpc.core.rpc.AbstractRpcClient;
import com.tencent.trpc.core.rpc.ConsumerInvoker;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.http.config.SocketConfig;
import org.apache.http.impl.client.CloseableHttpClient;
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
 *         when it has been idle for at least
 *         {@value #VALIDATE_AFTER_INACTIVITY_MS}ms (avoids the classic "stale connection /
 *         {@code NoHttpResponseException}" against a server-side half-closed keep-alive socket);</li>
 *     <li>{@code evictExpiredConnections} + {@code evictIdleConnections}: a daemon thread evicts
 *         connections idle longer than {@value #EVICT_IDLE_CONNECTIONS_SECONDS}s, freeing OS file
 *         descriptors;</li>
 *     <li>{@code keepAliveStrategy} with a {@value #FALLBACK_KEEPALIVE_MINUTES}min ceiling: when
 *         the server omits {@code Keep-Alive: timeout=N} we still cap connection age client-side,
 *         which beats most NAT idle timers (typical 5–15min);</li>
 *     <li>{@code connectionTimeToLive}: hard ceiling — every connection is forcibly recycled
 *         after {@value #CONNECTION_TTL_MINUTES}min regardless of activity, so backend IP rotation
 *         (K8s pod drift, blue/green) is recovered from in bounded time;</li>
 *     <li>{@code SO_KEEPALIVE=true} on every pooled socket: a last-resort path so the OS
 *         eventually surfaces dead peers in the worst-case "host pulled the plug / kernel
 *         panic" black-hole scenario where no FIN/RST is ever sent. Linux defaults are
 *         conservative (~2h11min) — the application-layer paths above will normally fire
 *         long before this kicks in.</li>
 * </ul>
 *
 * <p><b>Health signalling to the cluster manager</b>. The cluster manager's periodic health
 * observer in {@code RpcClusterClientManager} polls {@link #isAvailable()} every 30s. A
 * cached HTTP client is reported unavailable (and eventually evicted) when either:</p>
 * <ol>
 *     <li>it has not served any RPC for longer than {@value #IDLE_UNAVAILABLE_THRESHOLD_MINUTES}
 *         minutes (orphaned by backend IP rotation), or</li>
 *     <li>it has accumulated {@value #FAILURE_UNAVAILABLE_THRESHOLD} consecutive RPC failures
 *         (backend persistently 5xx / unreachable). The counter is reset by every successful
 *         RPC so transient failures never cross the threshold.</li>
 * </ol>
 *
 * <p>All long-connection state ({@link #lastUsedNanos}, {@link #consecutiveFailures}) uses
 * {@code volatile} / {@link AtomicInteger} primitives — safe to read/write concurrently from
 * any number of business threads with no lock.</p>
 */
public class HttpRpcClient extends AbstractRpcClient {

    private static final Logger logger = LoggerFactory.getLogger(HttpRpcClient.class);

    /**
     * Validate a pooled connection before reuse if it has been idle for at least this many
     * milliseconds. Cheap heuristic that catches most server-side half-closed keep-alive sockets.
     */
    private static final int VALIDATE_AFTER_INACTIVITY_MS = 2000;
    /**
     * Evict pooled connections that have been idle for longer than this duration.
     */
    private static final long EVICT_IDLE_CONNECTIONS_SECONDS = 60L;
    /**
     * Fallback {@code Keep-Alive} duration applied client-side when the server response omits
     * a {@code Keep-Alive: timeout=N} hint. Picked to be shorter than typical NAT / LB idle
     * timers (5–15 minutes) so we never hold a connection past the point where some hop on
     * the path has silently dropped it.
     */
    private static final int FALLBACK_KEEPALIVE_MINUTES = 5;
    /**
     * Hard upper bound on a single connection's lifetime. Any pooled connection older than this
     * is discarded on next checkout, regardless of activity. This is the recovery mechanism for
     * backend IP rotation (K8s pod drift) — without it a hot connection routed to an old pod
     * could survive indefinitely.
     */
    private static final int CONNECTION_TTL_MINUTES = 10;
    /**
     * If this client has not been used by any RPC for longer than this window, the periodic
     * health observer in {@code RpcClusterClientManager} will treat it as unavailable. After
     * a few consecutive unavailable observations the client gets closed and evicted from the
     * cluster cache, which is how we reclaim {@link HttpRpcClient} instances orphaned by backend
     * IP rotation. The window is intentionally large so that any actively-used client is never
     * affected.
     */
    static final int IDLE_UNAVAILABLE_THRESHOLD_MINUTES = 10;
    private static final long IDLE_UNAVAILABLE_THRESHOLD_NANOS =
            TimeUnit.MINUTES.toNanos(IDLE_UNAVAILABLE_THRESHOLD_MINUTES);
    /**
     * Number of consecutive failed RPCs that flips this client to unavailable. The counter is
     * reset to 0 on the next successful RPC, so transient blips never cross the threshold —
     * only sustained failure (e.g. backend 5xx storm, unreachable IP) does.
     */
    static final int FAILURE_UNAVAILABLE_THRESHOLD = 50;

    private CloseableHttpClient httpClient;
    /**
     * Timestamp ({@link System#nanoTime()}) of the most recent RPC sent through this client.
     * Updated by {@link HttpConsumerInvoker} on each send. {@code volatile} for safe lock-free
     * publication to the health-observer thread.
     */
    private volatile long lastUsedNanos = System.nanoTime();
    /**
     * Number of consecutive failed RPCs since the last success. Bumped by
     * {@link #markFailure()} and zeroed by {@link #markSuccess()}. Lock-free — concurrent
     * RPC threads never serialize on this counter.
     */
    private final AtomicInteger consecutiveFailures = new AtomicInteger(0);

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
        // last-resort path for surfacing dead peers in the worst case where neither the
        // application-layer Keep-Alive timer (5min) nor connectionTimeToLive (10min) fires —
        // typically the "host pulled the plug / kernel panic" black-hole scenario where no FIN
        // or RST is ever sent. Linux defaults are conservative (~2h11min total: 7200s idle +
        // 9 × 75s probes) so this isn't a fast path, but it's a pure win — the alternative is
        // socket lingering until {@code tcp_retries2} (~15min) catches up. Operators who care
        // about faster recovery should tune sysctl {@code net.ipv4.tcp_keepalive_time/intvl/probes}
        // host-wide; Apache HttpClient 4.x's blocking IO does not expose per-socket
        // configuration of those values via SocketOptions, hence the OS default applies.
        cm.setDefaultSocketConfig(SocketConfig.custom()
                .setSoKeepAlive(true)
                .build());
        httpClient = HttpClients.custom()
                .setConnectionManager(cm)
                // Background eviction of stale & long-idle connections; keeps the pool tidy in
                // long-running processes without affecting hot connections.
                .evictExpiredConnections()
                .evictIdleConnections(EVICT_IDLE_CONNECTIONS_SECONDS, TimeUnit.SECONDS)
                // Cap server-suggested keep-alive at FALLBACK_KEEPALIVE_MINUTES. When the server
                // omits a Keep-Alive header HttpClient defaults to "infinite" — that loses to any
                // intermediary NAT / LB silently dropping idle sockets, manifesting as the
                // dreaded NoHttpResponseException on the next request.
                .setKeepAliveStrategy(HttpRpcClient::resolveKeepAliveDuration)
                // Hard ceiling: every connection forcibly recycled after CONNECTION_TTL_MINUTES.
                // Recovers from backend IP rotation in bounded time even if the connection stays
                // hot.
                .setConnectionTimeToLive(CONNECTION_TTL_MINUTES, TimeUnit.MINUTES)
                .build();
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

    /**
     * Record that this client just served (or is about to serve) an RPC. Called by
     * {@link HttpConsumerInvoker} on every request entry.
     */
    public void markUsed() {
        lastUsedNanos = System.nanoTime();
    }

    /**
     * Record a successful RPC. Resets {@link #consecutiveFailures} to zero so that an isolated
     * earlier failure does not contribute toward eviction.
     */
    public void markSuccess() {
        consecutiveFailures.set(0);
    }

    /**
     * Record a failed RPC (either an exception during {@code execute} or a non-2xx response).
     * After {@value #FAILURE_UNAVAILABLE_THRESHOLD} consecutive failures the client reports
     * unavailable so the cluster manager can evict it; one success in between resets the
     * counter and keeps the client cached.
     */
    public void markFailure() {
        consecutiveFailures.incrementAndGet();
    }

    /**
     * Reports the client as unavailable if any of the following holds:
     * <ul>
     *     <li>the underlying lifecycle is no longer started (closed / failed);</li>
     *     <li>no RPC has been sent through this client for longer than
     *         {@value #IDLE_UNAVAILABLE_THRESHOLD_MINUTES} minutes (orphaned client);</li>
     *     <li>at least {@value #FAILURE_UNAVAILABLE_THRESHOLD} consecutive failures have
     *         accumulated since the last success (sustained backend outage).</li>
     * </ul>
     * <p>For an actively used, healthy client this method always returns {@code true}.</p>
     */
    @Override
    public boolean isAvailable() {
        if (!super.isAvailable()) {
            return false;
        }
        if (consecutiveFailures.get() >= FAILURE_UNAVAILABLE_THRESHOLD) {
            return false;
        }
        return (System.nanoTime() - lastUsedNanos) <= IDLE_UNAVAILABLE_THRESHOLD_NANOS;
    }

    public CloseableHttpClient getHttpClient() {
        return httpClient;
    }

    /**
     * Visible for tests / observability: current consecutive-failure counter snapshot.
     */
    public int getConsecutiveFailures() {
        return consecutiveFailures.get();
    }
}
