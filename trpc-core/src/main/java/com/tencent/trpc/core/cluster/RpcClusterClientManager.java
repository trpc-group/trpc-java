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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Used to manage the list mapping of point-to-point clients generated through BackendConfig,
 * and because the entire framework is based on a pull model to maintain the service IP of the server.
 * Therefore, a scanner is added to remove long-unused clients.
 */
public class RpcClusterClientManager {

    private static final Logger logger = LoggerFactory.getLogger(RpcClusterClientManager.class);
    /**
     * Cluster map, {@code Map<BackendConfig, Map<String, RpcClientProxy>>}
     */
    private static final Map<BackendConfig, Map<String, RpcClientProxy>> CLUSTER_MAP = Maps.newConcurrentMap();
    /**
     * Is close flag
     */
    private static final AtomicBoolean CLOSED_FLAG = new AtomicBoolean(false);
    /**
     * Prevent too many clients and perform periodic cleaning.
     */
    private static ScheduledFuture<?> cleanerFuture;

    static {
        cleanerFuture = startRpcClientCleaner();
    }

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
     * Used to periodically scan unused clients and release them.
     * <p>Add judgment to determine whether to close the shared thread pool.</p>
     *
     * @return ScheduledFuture, a delayed result-bearing action that can be cancelled
     */
    private static ScheduledFuture<?> startRpcClientCleaner() {
        return Optional.ofNullable(WorkerPoolManager.getShareScheduler())
                .map(ss -> {
                    if (ss.isShutdown()) {
                        return null;
                    }
                    return ss.scheduleAtFixedRate(() -> {
                        try {
                            scanUnusedClient();
                        } catch (Throwable ex) {
                            logger.error("RpcClientCleaner exception", ex);
                        }
                    }, 0, 15, TimeUnit.MINUTES);
                }).orElse(null);
    }

    /**
     * Scanning for unused clients.
     */
    public static void scanUnusedClient() {
        Map<BackendConfig, List<RpcClient>> unusedClientMap = Maps.newHashMap();
        CLUSTER_MAP.forEach((bConfig, clusterMap) -> {
            if (logger.isDebugEnabled()) {
                logger.debug("RpcClusterClient scheduler report clusterName={}, naming={}, num of client is {}",
                        bConfig.getName(), bConfig.getNamingOptions().getServiceNaming(), clusterMap.keySet().size());
            }
            clusterMap.forEach((clientKey, clientValue) -> {
                try {
                    if (isIdleTimeout(bConfig, clientValue)) {
                        Optional.ofNullable(clusterMap.remove(clientKey))
                                .ifPresent(rpcCli -> unusedClientMap.computeIfAbsent(bConfig, k -> new ArrayList<>())
                                        .add(rpcCli));
                    }
                } catch (Throwable ex) {
                    logger.error("RpcClientCleaner exception", ex);
                }
            });
        });
        unusedClientMap.forEach((bConfig, value) -> value.forEach(e -> {
            try {
                e.close();
            } finally {
                logger.warn("RpcClient in clusterName={}, naming={}, remove rpc client{}, due to unused time > {} ms",
                        bConfig.getName(), bConfig.getNamingOptions().getServiceNaming(),
                        e.getProtocolConfig().toSimpleString(), bConfig.getIdleTimeout());
            }
        }));
    }

    private static boolean isIdleTimeout(BackendConfig bConfig, RpcClientProxy clientProxy) {
        long unusedNanosLimit = TimeUnit.MILLISECONDS.toNanos(bConfig.getIdleTimeout());
        long lastUsedNanos = clientProxy.getLastUsedNanos();
        return lastUsedNanos > 0 && unusedNanosLimit > 0 && (System.nanoTime() - lastUsedNanos) > unusedNanosLimit;
    }

    /**
     * Get RpcClient based on BackendConfig. If RpcClient does not exist, create a new one and cache it.
     *
     * @param bConfig BackendConfig, configuration for the backend
     * @param pConfig ProtocolConfig, configuration for the protocol
     * @return RpcClient instance based on BackendConfig and ProtocolConfig
     */
    public static RpcClient getOrCreateClient(BackendConfig bConfig, ProtocolConfig pConfig) {
        Preconditions.checkNotNull(bConfig, "backendConfig can't not be null");
        Map<String, RpcClientProxy> map = CLUSTER_MAP.computeIfAbsent(bConfig, k -> new ConcurrentHashMap<>());
        RpcClientProxy rpcClientProxy = map.computeIfAbsent(pConfig.toUniqId(),
                uniqId -> createRpcClientProxy(pConfig));
        rpcClientProxy.updateLastUsedNanos();
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
     * Close client
     */
    public static void close() {
        if (CLOSED_FLAG.compareAndSet(Boolean.FALSE, Boolean.TRUE)) {
            try {
                Optional.ofNullable(cleanerFuture).ifPresent(cf -> cf.cancel(Boolean.TRUE));
            } catch (Exception ex) {
                logger.error("clientCleanerFuture ", ex);
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
            rpcClient.updateLastUsedNanos();
            return delegate.invoke(request);
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

        private RpcClient delegate;

        private volatile long lastUsedNanos = System.nanoTime();

        RpcClientProxy(RpcClient delegate) {
            this.delegate = delegate;
        }

        public void updateLastUsedNanos() {
            lastUsedNanos = System.nanoTime();
        }

        public long getLastUsedNanos() {
            return lastUsedNanos;
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