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

import static org.junit.Assert.assertNull;

import com.tencent.trpc.core.common.config.BackendConfig;
import com.tencent.trpc.core.common.config.ConsumerConfig;
import com.tencent.trpc.core.common.config.ProtocolConfig;
import com.tencent.trpc.core.exception.TRpcException;
import com.tencent.trpc.core.rpc.CloseFuture;
import com.tencent.trpc.core.rpc.ConsumerInvoker;
import com.tencent.trpc.core.rpc.RpcClient;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Covers the {@code logger.isDebugEnabled() == false} branches in
 * {@link RpcClusterClientManager}. The test classpath's {@code log4j2.xml} configures the root
 * logger at {@code DEBUG}, so the {@code true} branches are already exercised by other tests.
 *
 * <p>Strategy: temporarily raise the {@link RpcClusterClientManager} logger level to {@code INFO}
 * via the standard log4j2 API (same API as {@code Log4j2LoggerProcessUnit}); run the same code
 * paths; restore the level afterwards. No reflection / no PowerMock.</p>
 */
public class RpcClusterLoggerLevelTest {

    private static final String TARGET_LOGGER = RpcClusterClientManager.class.getName();

    private Level originalLevel;

    @Before
    public void setUp() throws Exception {
        RpcClusterClientManager.reset();
        clearClusterMap();
        // Snapshot original level and force INFO so isDebugEnabled() returns false.
        originalLevel = setLoggerLevel(TARGET_LOGGER, Level.INFO);
    }

    @After
    public void tearDown() throws Exception {
        // Restore original level (DEBUG inherited from root by default).
        setLoggerLevel(TARGET_LOGGER, originalLevel);
        clearClusterMap();
        RpcClusterClientManager.reset();
    }

    /**
     * Drives the {@code if (logger.isDebugEnabled())} blocks in the source with the flag
     * evaluating to {@code false}, covering the previously-missed branches:
     * <ul>
     *     <li>{@code shutdownBackendConfig} success path,</li>
     *     <li>{@code getOrCreateClient} closeFuture hook.</li>
     * </ul>
     * Also exercises {@link RpcClusterClientManager#scanIdleClients()} once for completeness;
     * the Netty stub's default transporter makes the idle-scan a no-op.
     */
    @Test
    public void testDebugBranchesWhenLoggerDisabled() throws Exception {
        // Sanity check: after setLoggerLevel(INFO), the manager's logger must report
        // isDebugEnabled() == false; otherwise our coverage assumption is wrong.
        com.tencent.trpc.core.logger.Logger mgrLogger =
                com.tencent.trpc.core.logger.LoggerFactory.getLogger(RpcClusterClientManager.class);
        org.junit.Assert.assertFalse("logger.isDebugEnabled() must be false to cover the missed branch",
                mgrLogger.isDebugEnabled());

        BackendConfig backendConfig = new BackendConfig();
        backendConfig.setNamingUrl("ip://127.0.0.1:9100");
        StubProtocolConfig pConfig = new StubProtocolConfig();

        // (1) Triggers getOrCreateClient → eventually closeFuture hook (via shutdown below).
        RpcClient client = RpcClusterClientManager.getOrCreateClient(backendConfig, pConfig);
        org.junit.Assert.assertNotNull(client);

        pConfig.available = false;
        invokeScanIdleClients();

        // (2) shutdownBackendConfig success path.
        RpcClusterClientManager.shutdownBackendConfig(backendConfig);
        Field f = RpcClusterClientManager.class.getDeclaredField("CLUSTER_MAP");
        f.setAccessible(true);
        assertNull(((Map<?, ?>) f.get(null)).get(backendConfig));
    }

    /* ---------------- helpers ---------------- */

    /**
     * Set the level of {@code loggerName} via log4j2 API; returns the previous level so the
     * caller can restore it. Mirrors {@code Log4j2LoggerProcessUnit#changeLoggerLevel}.
     */
    private static Level setLoggerLevel(String loggerName, Level newLevel) {
        LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        Configuration config = ctx.getConfiguration();
        LoggerConfig loggerConfig = config.getLoggerConfig(loggerName);
        Level previous = loggerConfig.getLevel();
        // If the resolved logger config is the parent (root), create a dedicated one so we don't
        // accidentally raise the level for other tests sharing the root logger.
        if (!loggerConfig.getName().equals(loggerName)) {
            LoggerConfig dedicated = LoggerConfig.createLogger(false, newLevel, loggerName,
                    "true", new org.apache.logging.log4j.core.config.AppenderRef[0], null, config,
                    null);
            config.addLogger(loggerName, dedicated);
        } else {
            loggerConfig.setLevel(newLevel);
        }
        ctx.updateLoggers();
        return previous;
    }

    private static void invokeScanIdleClients() throws Exception {
        Method m = RpcClusterClientManager.class.getDeclaredMethod("scanIdleClients");
        m.setAccessible(true);
        m.invoke(null);
    }

    private static void clearClusterMap() throws Exception {
        Field f = RpcClusterClientManager.class.getDeclaredField("CLUSTER_MAP");
        f.setAccessible(true);
        ((Map<?, ?>) f.get(null)).clear();
    }

    /* ---------------- stubs ---------------- */

    private static class StubProtocolConfig extends ProtocolConfig {

        volatile boolean available = true;
        final AtomicBoolean closed = new AtomicBoolean(false);

        @Override
        public RpcClient createClient() {
            return new StubRpcClient(this);
        }
    }

    private static class StubRpcClient implements RpcClient {

        private final StubProtocolConfig owner;
        private final CloseFuture<Void> closeFuture = new CloseFuture<>();

        StubRpcClient(StubProtocolConfig owner) {
            this.owner = owner;
        }

        @Override
        public void open() throws TRpcException {
        }

        @Override
        public boolean isClosed() {
            return owner.closed.get();
        }

        @Override
        public boolean isAvailable() {
            return owner.available && !owner.closed.get();
        }

        @Override
        public ProtocolConfig getProtocolConfig() {
            return owner;
        }

        @Override
        public void close() {
            owner.closed.set(true);
            closeFuture.complete(null);
        }

        @Override
        public <T> ConsumerInvoker<T> createInvoker(ConsumerConfig<T> consumerConfig) {
            return null;
        }

        @Override
        public CloseFuture<Void> closeFuture() {
            return closeFuture;
        }
    }
}
