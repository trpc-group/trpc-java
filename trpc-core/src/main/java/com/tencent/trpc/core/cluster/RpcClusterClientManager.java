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

package com.tencent.trpc.core.cluster;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.tencent.trpc.core.common.Constants;
import com.tencent.trpc.core.common.config.BackendConfig;
import com.tencent.trpc.core.common.config.ConsumerConfig;
import com.tencent.trpc.core.common.config.ProtocolConfig;
import com.tencent.trpc.core.exception.TRpcException;
import com.tencent.trpc.core.logger.Logger;
import com.tencent.trpc.core.logger.LoggerFactory;
import com.tencent.trpc.core.rpc.CloseFuture;
import com.tencent.trpc.core.rpc.ConsumerInvoker;
import com.tencent.trpc.core.rpc.Request;
import com.tencent.trpc.core.rpc.Response;
import com.tencent.trpc.core.rpc.RpcClient;
import com.tencent.trpc.core.worker.WorkerPoolManager;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Used to manage the list mapping of point-to-point clients generated through BackendConfig.
 * <p>
 * Long-connection lifecycle:
 * <ul>
 *     <li><b>Netty-based clients</b> ({@code transporter=netty}, e.g. the standard tRPC
 *         protocol) are kept alive for the lifetime of the {@link BackendConfig}. Their
 *         idleness is already governed at the transport layer (Netty
 *         {@code IdleStateHandler} and TCP keepalive), so the cluster manager never closes
 *         them by idle time. Recovery on transport failure is delegated entirely to:
 *         <ol>
 *             <li>the request path (
 *                 {@link com.tencent.trpc.core.transport.AbstractClientTransport#ensureChannelActive}),
 *                 which lazily rebuilds a slot when the next call arrives, and</li>
 *             <li>Netty's {@code channelInactive} event, which surfaces TCP-level failures
 *                 (RST / FIN / kernel keepalive) into the cache via the proxy's
 *                 {@link RpcClient#closeFuture()} hook.</li>
 *         </ol>
 *     </li>
 *     <li><b>Non-Netty clients</b> (e.g. HTTP / Jetty pooled connections) are policed by a
 *         lightweight background <b>idle-scanner</b> that runs every
 *         {@value #IDLE_SCAN_PERIOD_SECONDS}s. For each cached client, if no successful RPC
 *         response has been observed for longer than the configured
 *         {@link ProtocolConfig#getIdleTimeout()} (in milliseconds), the scanner closes the
 *         client. Closing fires the {@link RpcClient#closeFuture()} hook, which removes the
 *         entry from the cluster cache; the next request rebuilds a fresh connection
 *         lazily. Setting {@code idleTimeout <= 0} disables the check.
 *         <p>To avoid racing with a request whose response is just about to come back,
 *         the scanner uses an in-flight counter, a single-shot CAS gate and a re-check of
 *         the last-response timestamp before actually closing &mdash; see
 *         {@link #closeIfIdleResponseTimedOut} for details.</p></li>
 *     <li>The idle-scanner <b>does NOT send heartbeats and does NOT trigger reconnects</b>;
 *         its only side effect is closing idle non-Netty clients.</li>
 *     <li>When the underlying {@link RpcClient} closes itself (transport error, idle-scan
 *         eviction or explicit shutdown), the {@link RpcClient#closeFuture()} callback
 *         removes the cache entry so the next request rebuilds a fresh long connection.</li>
 *     <li>{@link #shutdownBackendConfig(BackendConfig)} / {@link #close()} still release
 *         clients explicitly.</li>
 * </ul>
 */
public class RpcClusterClientManager {

    private static final Logger logger = LoggerFactory.getLogger(RpcClusterClientManager.class);

    /**
     * How often (in seconds) the background idle-scanner runs to evict non-Netty clients
     * that have not received any successful RPC response within their configured
     * {@link ProtocolConfig#getIdleTimeout()}.
     */
    private static final int IDLE_SCAN_PERIOD_SECONDS = 30;

    /**
     * Cluster map, {@code Map<BackendConfig, Map<String, RpcClientProxy>>}
     */
    private static final Map<BackendConfig, Map<String, RpcClientProxy>> CLUSTER_MAP = Maps.newConcurrentMap();
    /**
     * Is close flag
     */
    private static final AtomicBoolean CLOSED_FLAG = new AtomicBoolean(false);

    /**
     * Handle of the periodic idle-scan task, started lazily on the first
     * {@link #getOrCreateClient(BackendConfig, ProtocolConfig)}. {@code null} until the
     * scheduler accepts the task; remains {@code null} if the scheduler rejected it.
     */
    private static volatile ScheduledFuture<?> idleScanFuture;

    /**
     * Shutdown a cluster.
     *
     * @param backendConfig the configuration for the backend
     */
    public static void shutdownBackendConfig(BackendConfig backendConfig) {
        Optional.ofNullable(CLUSTER_MAP.remove(backendConfig))
                .ifPresent(proxyMap -> proxyMap.forEach((k, v) -> {
                    try {
                        v.close();
                        if (logger.isDebugEnabled()) {
                            logger.debug("Shutdown client:{} backendConfig:{} success", k,
                                    backendConfig.toSimpleString());
                        }
                    } catch (Exception ex) {
                        logger.error("Shutdown client:{} backendConfig:{},exception", k, backendConfig.toSimpleString(),
                                ex);
                    }
                }));
    }

    /**
     * Get RpcClient based on BackendConfig. If RpcClient does not exist, create a new one and cache it.
     * <p>The created client is a long-lived connection. To prevent memory leak, when the
     * underlying client is closed (by itself or via the cache-eviction hook below), its entry
     * in the cache is removed via the {@link RpcClient#closeFuture()} callback.</p>
     *
     * @param bConfig BackendConfig, configuration for the backend
     * @param pConfig ProtocolConfig, configuration for the protocol
     * @return RpcClient instance based on BackendConfig and ProtocolConfig
     */
    public static RpcClient getOrCreateClient(BackendConfig bConfig, ProtocolConfig pConfig) {
        Preconditions.checkNotNull(bConfig, "backendConfig can't not be null");
        ensureIdleScanStarted();
        Map<String, RpcClientProxy> map = CLUSTER_MAP.computeIfAbsent(bConfig, k -> new ConcurrentHashMap<>());
        String uniqId = pConfig.toUniqId();
        RpcClientProxy rpcClientProxy = map.computeIfAbsent(uniqId,
                k -> {
                    RpcClientProxy proxy = createRpcClientProxy(pConfig);
                    // When the underlying rpcClient closes (transport error or explicit
                    // shutdown), remove it from the cache to avoid memory leak. The next call
                    // will rebuild a new long connection on demand.
                    proxy.closeFuture().whenComplete((r, e) -> {
                        Map<String, RpcClientProxy> clusterMap = CLUSTER_MAP.get(bConfig);
                        if (clusterMap != null) {
                            // Only remove if the cached proxy is still the same instance.
                            clusterMap.remove(k, proxy);
                        }
                        if (logger.isDebugEnabled()) {
                            logger.debug("RpcClient closed, removed from cluster cache, backendConfig={}, client={}",
                                    bConfig.toSimpleString(), proxy.getProtocolConfig().toSimpleString());
                        }
                    });
                    return proxy;
                });
        return rpcClientProxy;
    }

    private static RpcClientProxy createRpcClientProxy(ProtocolConfig protocolConfig) {
        Preconditions.checkArgument(!CLOSED_FLAG.get(), "Closed, can't create client");
        RpcClientProxy createdClient = new RpcClientProxy(protocolConfig.createClient());
        boolean isSucceeded = false;
        try {
            createdClient.open();
            isSucceeded = true;
            return createdClient;
        } finally {
            if (!isSucceeded) {
                createdClient.close();
            }
        }
    }

    /**
     * Lazily start the periodic idle-scan task on first usage. Idempotent and thread-safe.
     * <p>If the shared scheduler rejects the task, the failure is logged and swallowed:
     * Netty long connections remain unaffected (they manage their own idleness), and
     * non-Netty pooled clients simply lose the proactive idle-timeout eviction — they
     * will still be reclaimed lazily on the next failed request.</p>
     */
    private static void ensureIdleScanStarted() {
        if (idleScanFuture != null || CLOSED_FLAG.get()) {
            return;
        }
        synchronized (RpcClusterClientManager.class) {
            if (idleScanFuture != null || CLOSED_FLAG.get()) {
                return;
            }
            try {
                idleScanFuture = WorkerPoolManager.getShareScheduler().scheduleAtFixedRate(
                        RpcClusterClientManager::scanIdleClients,
                        IDLE_SCAN_PERIOD_SECONDS,
                        IDLE_SCAN_PERIOD_SECONDS,
                        TimeUnit.SECONDS);
            } catch (Throwable ex) {
                logger.warn("Start cluster idle-scan task failed; non-Netty clients will only "
                        + "be reclaimed lazily on the request path", ex);
            }
        }
    }

    /**
     * Periodic idle-scan tick. Iterates every cached {@link RpcClientProxy} and delegates
     * to {@link #closeIfIdleResponseTimedOut} to evict non-Netty clients whose
     * idle-response timeout has elapsed. Per-proxy exceptions are swallowed so a single
     * misbehaving client cannot break the timer loop.
     * <p>Netty-based transports are deliberately left untouched here — see class javadoc.</p>
     */
    static void scanIdleClients() {
        if (CLOSED_FLAG.get()) {
            return;
        }
        CLUSTER_MAP.forEach((bConfig, clusterMap) -> clusterMap.forEach((key, proxy) -> {
            try {
                closeIfIdleResponseTimedOut(bConfig, key, proxy);
            } catch (Throwable ex) {
                logger.error("IdleScan: tick on client {} threw", key, ex);
            }
        }));
    }

    /**
     * Close a non-Netty client when it has not received any successful RPC response within
     * {@link ProtocolConfig#getIdleTimeout()} milliseconds. Closing fires the
     * {@link RpcClient#closeFuture()} hook installed in {@link #getOrCreateClient}, which
     * removes the entry from the cluster cache so the next request rebuilds a fresh client.
     *
     * <p>Netty-based transports are skipped: their idleness is already governed by Netty's
     * {@code IdleStateHandler} and TCP keepalive; closing them here would tear down the
     * shared {@code EventLoopGroup} and abort in-flight long-connection requests.</p>
     *
     * <p><b>Race-window narrowing</b>. Closing a long-lived client while a request is in
     * flight (or a response is on its way back from the wire) would surface as a spurious
     * I/O failure to the caller. To minimise that window without resorting to coarse
     * locking, this method:
     * <ol>
     *     <li>skips the proxy when {@link RpcClientProxy#inFlight} &gt; 0 (a request is on
     *         the wire — wait until the next tick);</li>
     *     <li>uses {@link RpcClientProxy#closing} as a single-shot CAS gate so only one
     *         caller (this scanner thread) actually invokes {@link RpcClient#close()};</li>
     *     <li>re-reads {@link RpcClientProxy#lastResponseTimeMs} <i>after</i> winning the
     *         CAS &mdash; if a response landed in the tiny window before we won, the
     *         eviction is aborted and the {@code closing} flag is rolled back so a future
     *         tick can try again.</li>
     * </ol>
     * The truly extreme corner case &mdash; a response arriving on the wire <i>after</i>
     * {@code close()} has begun &mdash; is handled by the underlying transport: the
     * pooled HTTP client surfaces an {@code IOException} on the affected request, which
     * the consumer invoker maps to a normal {@link TRpcException}. Subsequent requests
     * see the proxy gone from the cache and rebuild a fresh connection.</p>
     */
    private static void closeIfIdleResponseTimedOut(BackendConfig bConfig, String key, RpcClientProxy proxy) {
        ProtocolConfig pConfig = proxy.getProtocolConfig();
        if (pConfig == null) {
            return;
        }
        // Skip Netty transports — see method-level javadoc.
        String transporter = pConfig.getTransporter();
        if (transporter == null || Constants.TRANSPORTER_NETTY.equalsIgnoreCase(transporter)) {
            return;
        }
        Integer idleTimeoutBoxed = pConfig.getIdleTimeout();
        if (idleTimeoutBoxed == null) {
            return;
        }
        long idleTimeoutMs = idleTimeoutBoxed.longValue();
        if (idleTimeoutMs <= 0L) {
            return;
        }
        // Already closed (or being closed) — leave the cleanup to the closeFuture hook.
        if (proxy.isClosed() || proxy.closing.get()) {
            return;
        }
        // Skip while any RPC is on the wire — closing now would surface as a spurious
        // I/O failure on the in-flight request. The scanner will re-evaluate next tick.
        if (proxy.inFlight.get() > 0) {
            return;
        }
        long idleMs = System.currentTimeMillis() - proxy.lastResponseTimeMs.get();
        if (idleMs < idleTimeoutMs) {
            return;
        }
        // Claim the right to close exactly once.
        if (!proxy.closing.compareAndSet(false, true)) {
            return;
        }
        // Re-check after winning the CAS to plug the residual race: a successful response
        // may have updated lastResponseTimeMs (and inFlight may have ticked back up)
        // between our first read above and the CAS here. If so, abort the eviction and
        // give back the closing flag so a future tick can retry.
        long idleMsAfterCas = System.currentTimeMillis() - proxy.lastResponseTimeMs.get();
        if (idleMsAfterCas < idleTimeoutMs || proxy.inFlight.get() > 0) {
            proxy.closing.set(false);
            return;
        }
        try {
            logger.info("IdleScan: closing idle client {} (transporter={}, idleMs={}, "
                            + "idleTimeoutMs={}); cache entry will be removed via closeFuture",
                    pConfig.toSimpleString(), transporter, idleMsAfterCas, idleTimeoutMs);
            proxy.close();
        } catch (Throwable ex) {
            logger.error("IdleScan: close idle client {} (key={}) failed",
                    pConfig.toSimpleString(), key, ex);
        }
    }

    /**
     * Close client
     */
    public static void close() {
        if (CLOSED_FLAG.compareAndSet(Boolean.FALSE, Boolean.TRUE)) {
            try {
                ScheduledFuture<?> f = idleScanFuture;
                if (f != null) {
                    f.cancel(true);
                    idleScanFuture = null;
                }
            } catch (Exception ex) {
                logger.error("Cancel cluster idle-scan task failed", ex);
            }
            CLUSTER_MAP.forEach((config, clientProxyMap) -> clientProxyMap
                    .forEach((key, clientProxy) -> {
                        try {
                            clientProxy.close();
                        } catch (Exception ex) {
                            logger.error("Close clusterConfig{}, client {} exception:", config.toSimpleString(), key,
                                    ex);
                        }
                    }));
            CLUSTER_MAP.clear();
        }
    }

    public static synchronized void reset() {
        CLOSED_FLAG.set(false);
    }

    private static class ConsumerInvokerProxy<T> implements ConsumerInvoker<T> {

        private ConsumerInvoker<T> delegate;
        private RpcClientProxy rpcClient;

        ConsumerInvokerProxy(ConsumerInvoker<T> delegate, RpcClientProxy rpcClient) {
            super();
            this.delegate = delegate;
            this.rpcClient = rpcClient;
        }

        @Override
        public Class<T> getInterface() {
            return delegate.getInterface();
        }

        @Override
        public CompletionStage<Response> invoke(Request request) {
            // Bump the in-flight counter BEFORE handing the request off to the delegate,
            // so the idle-scanner cannot race in and close the proxy between the counter
            // read and the actual network operation. A matching decrement runs in
            // whenComplete below regardless of success or failure.
            rpcClient.inFlight.incrementAndGet();
            CompletionStage<Response> stage;
            try {
                stage = delegate.invoke(request);
            } catch (Throwable ex) {
                // Synchronous failure (e.g. immediate IllegalStateException) — release the
                // counter here, otherwise it would leak. Then propagate so the caller's
                // exception-handling stays unchanged.
                rpcClient.inFlight.decrementAndGet();
                throw ex;
            }
            // Mark the underlying long-lived client as "active" only when we actually
            // observe a response come back from the wire. Failures are intentionally NOT
            // counted as activity here: in a fully-broken-link scenario invocations may
            // fail-fast indefinitely, and treating those as "activity" would prevent the
            // cluster manager's idle-response timeout from ever closing the dead client.
            return stage.whenComplete((response, throwable) -> {
                try {
                    if (throwable == null && response != null) {
                        rpcClient.markResponseReceived();
                    }
                } finally {
                    rpcClient.inFlight.decrementAndGet();
                }
            });
        }

        @Override
        public ConsumerConfig<T> getConfig() {
            return delegate.getConfig();
        }

        @Override
        public ProtocolConfig getProtocolConfig() {
            return delegate.getProtocolConfig();
        }

        @Override
        public int hashCode() {
            return Objects.hash(delegate);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            ConsumerInvokerProxy other = (ConsumerInvokerProxy) obj;
            return Objects.equals(delegate, other.delegate);
        }
    }

    private static class RpcClientProxy implements RpcClient {

        private final RpcClient delegate;
        /**
         * Wall-clock timestamp (ms) of the most recent successful RPC response observed on
         * this proxy. Initialised to the proxy creation time so a freshly-built client is
         * not eligible for idle-timeout eviction until at least
         * {@link ProtocolConfig#getIdleTimeout()} has elapsed without any traffic.
         * <p>Updated by {@link ConsumerInvokerProxy#invoke(Request)} when the underlying
         * stage completes with a non-null response and no throwable.</p>
         */
        final AtomicLong lastResponseTimeMs = new AtomicLong(System.currentTimeMillis());
        /**
         * Number of RPCs currently in flight on this proxy. Incremented on
         * {@link ConsumerInvokerProxy#invoke(Request)} entry and decremented on stage
         * completion (success or failure). Read by the idle-scanner: if any RPC is in
         * flight, eviction is skipped this tick to avoid racing with a request whose
         * response is about to come back. Pure best-effort &mdash; under truly extreme
         * timing the response can still arrive after {@link #close()} runs, but the
         * counter dramatically narrows the window.
         */
        final AtomicInteger inFlight = new AtomicInteger(0);
        /**
         * Single-shot guard used by the idle-scanner to claim "the right to close this
         * proxy" exactly once. {@code compareAndSet(false, true)} succeeds for the first
         * caller; subsequent calls (a concurrent shutdown, a duplicate scan, etc.) see
         * {@code true} and back off &mdash; the proxy will be removed from the cache by
         * the {@link RpcClient#closeFuture()} hook.
         */
        final AtomicBoolean closing = new AtomicBoolean(false);

        RpcClientProxy(RpcClient delegate) {
            this.delegate = delegate;
        }

        /**
         * Refresh {@link #lastResponseTimeMs} to "now". Called from the response-completion
         * path of {@link ConsumerInvokerProxy#invoke(Request)}. Skipped once the proxy has
         * already been claimed for closing &mdash; the next request will rebuild a fresh
         * proxy whose timestamp starts from the current wall-clock.
         */
        void markResponseReceived() {
            if (closing.get()) {
                return;
            }
            lastResponseTimeMs.set(System.currentTimeMillis());
        }

        @Override
        public void open() throws TRpcException {
            delegate.open();
        }

        @Override
        public <T> ConsumerInvoker<T> createInvoker(ConsumerConfig<T> consumerConfig) {
            return new ConsumerInvokerProxy<T>(delegate.createInvoker(consumerConfig), this);
        }

        @Override
        public void close() {
            delegate.close();
        }

        @Override
        public CloseFuture<Void> closeFuture() {
            return delegate.closeFuture();
        }

        @Override
        public boolean isAvailable() {
            return delegate.isAvailable();
        }

        @Override
        public boolean isClosed() {
            return delegate.isClosed();
        }

        @Override
        public ProtocolConfig getProtocolConfig() {
            return delegate.getProtocolConfig();
        }

        @Override
        public int hashCode() {
            return Objects.hash(delegate);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            RpcClientProxy other = (RpcClientProxy) obj;
            return Objects.equals(delegate, other.delegate);
        }
    }

}
