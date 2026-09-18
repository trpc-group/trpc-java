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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import com.tencent.trpc.core.common.config.BackendConfig;
import com.tencent.trpc.core.common.config.ConsumerConfig;
import com.tencent.trpc.core.common.config.ProtocolConfig;
import com.tencent.trpc.core.exception.TRpcException;
import com.tencent.trpc.core.rpc.CloseFuture;
import com.tencent.trpc.core.rpc.ConsumerInvoker;
import com.tencent.trpc.core.rpc.RpcClient;
import com.tencent.trpc.core.worker.WorkerPoolManager;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Drives the {@code catch (Throwable)} branch in
 * {@link RpcClusterClientManager#ensureIdleScanStarted()}: when the shared scheduler
 * rejects the periodic task, the manager must swallow the exception and leave
 * {@code idleScanFuture} as {@code null}.
 *
 * <p>Implemented without PowerMock — instead the {@code WorkerPoolManager.shareScheduler}
 * static field is reflectively replaced with a {@link ScheduledThreadPoolExecutor} subclass
 * whose {@code scheduleAtFixedRate} unconditionally throws
 * {@link RejectedExecutionException}. Original scheduler is restored in {@code @After}.</p>
 */
public class RpcClusterSchedulerRejectTest {

    private ScheduledThreadPoolExecutor originalScheduler;

    @Before
    public void setUp() throws Exception {
        RpcClusterClientManager.reset();
        clearIdleScanFuture();
        clearClusterMap();
        // Snapshot the live scheduler so we can put it back when the test ends.
        Field f = WorkerPoolManager.class.getDeclaredField("shareScheduler");
        f.setAccessible(true);
        this.originalScheduler = (ScheduledThreadPoolExecutor) f.get(null);
        f.set(null, new RejectingScheduler());
    }

    @After
    public void tearDown() throws Exception {
        // Restore the original scheduler before any other test in the same JVM observes
        // a rejected one.
        Field f = WorkerPoolManager.class.getDeclaredField("shareScheduler");
        f.setAccessible(true);
        f.set(null, originalScheduler);
        clearIdleScanFuture();
        clearClusterMap();
        RpcClusterClientManager.reset();
    }

    @Test
    public void testSchedulerExceptionCatchBranch() throws Exception {
        BackendConfig backendConfig = new BackendConfig();
        backendConfig.setNamingUrl("ip://127.0.0.1:9101");
        // Must NOT propagate the RejectedExecutionException — the manager has to swallow it
        // and keep functioning, falling back to lazy reconnect on the request path only.
        RpcClient client = RpcClusterClientManager.getOrCreateClient(backendConfig,
                new StubProtocolConfig());
        assertNotNull(client);

        // Catch branch leaves idleScanFuture as null.
        Field f = RpcClusterClientManager.class.getDeclaredField("idleScanFuture");
        f.setAccessible(true);
        assertNull("scheduler rejected → future must remain null", f.get(null));

        RpcClusterClientManager.shutdownBackendConfig(backendConfig);
    }

    /* ---------------- helpers ---------------- */

    private static void clearClusterMap() throws Exception {
        Field f = RpcClusterClientManager.class.getDeclaredField("CLUSTER_MAP");
        f.setAccessible(true);
        ((Map<?, ?>) f.get(null)).clear();
    }

    private static void clearIdleScanFuture() throws Exception {
        Field f = RpcClusterClientManager.class.getDeclaredField("idleScanFuture");
        f.setAccessible(true);
        Object cur = f.get(null);
        if (cur != null) {
            try {
                ((ScheduledFuture<?>) cur).cancel(true);
            } catch (Throwable ignore) {
                // ignore
            }
            f.set(null, null);
        }
    }

    /* ---------------- stubs ---------------- */

    /**
     * A {@link ScheduledThreadPoolExecutor} subclass that rejects every
     * {@code scheduleAtFixedRate} call. {@code core=1} keeps the parent constructor happy
     * without consuming real worker threads — the test never actually submits anything.
     */
    private static final class RejectingScheduler extends ScheduledThreadPoolExecutor {

        RejectingScheduler() {
            super(1);
        }

        @Override
        public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay,
                long period, TimeUnit unit) {
            throw new RejectedExecutionException("rejected by test stub");
        }

        @Override
        public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay,
                long delay, TimeUnit unit) {
            throw new RejectedExecutionException("rejected by test stub");
        }

        @Override
        public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
            throw new RejectedExecutionException("rejected by test stub");
        }

        @Override
        public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
            throw new RejectedExecutionException("rejected by test stub");
        }
    }

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
