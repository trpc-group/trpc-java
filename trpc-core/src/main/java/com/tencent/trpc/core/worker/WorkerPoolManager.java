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

package com.tencent.trpc.core.worker;

import com.tencent.trpc.core.common.Constants;
import com.tencent.trpc.core.common.NamedThreadFactory;
import com.tencent.trpc.core.common.config.PluginConfig;
import com.tencent.trpc.core.extension.ExtensionLoader;
import com.tencent.trpc.core.extension.ExtensionManager;
import com.tencent.trpc.core.logger.Logger;
import com.tencent.trpc.core.logger.LoggerFactory;
import com.tencent.trpc.core.worker.spi.WorkerPool;
import com.tencent.trpc.core.worker.support.thread.ThreadWorkerPool;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Worker pool manager.
 */
public class WorkerPoolManager {

    /**
     * Provider worker pool name
     */
    public static final String DEF_PROVIDER_WORKER_POOL_NAME = "trpc_provider_biz_def";
    public static final PluginConfig DEF_PROVIDER_WORKER_POOL_CONFIG;
    /**
     * Consumer worker pool name
     */
    public static final String DEF_CONSUMER_WORKER_POOL_NAME = "trpc_consumer_biz_def";
    public static final PluginConfig DEF_CONSUMER_WORKER_POOL_CONFIG;
    /**
     * Naming worker pool name
     */
    public static final String DEF_NAMING_WORKER_POOL_NAME = "trpc_naming_def";
    public static final PluginConfig DEF_NAMING_WORKER_POOL_CONFIG;
    private static final Logger LOG = LoggerFactory.getLogger(WorkerPoolManager.class);
    private static final AtomicBoolean CLOSED_FLAG = new AtomicBoolean(false);
    /**
     * Used for graceful shutdown.
     **/
    private static final Executor SHUTDOWN_EXECUTOR =
            Executors.newFixedThreadPool(Math.min(Constants.CPUS, 4));
    private static ExtensionManager<WorkerPool> manager = new ExtensionManager<>(WorkerPool.class);
    /**
     * Shared scheduling worker pool manager. Parameters can be considered for startup parameter configuration.
     */
    private static ScheduledThreadPoolExecutor shareScheduler;

    static {
        DEF_PROVIDER_WORKER_POOL_CONFIG = newThreadWorkerPoolConfig(DEF_PROVIDER_WORKER_POOL_NAME,
                Constants.DEFAULT_CORE_THREADS, Constants.DEFAULT_MAX_THREADS, Boolean.FALSE);
        DEF_CONSUMER_WORKER_POOL_CONFIG = newThreadWorkerPoolConfig(DEF_CONSUMER_WORKER_POOL_NAME,
                Constants.DEFAULT_CORE_THREADS, Constants.DEFAULT_MAX_THREADS, Boolean.FALSE);
        DEF_NAMING_WORKER_POOL_CONFIG = newThreadWorkerPoolConfig(DEF_NAMING_WORKER_POOL_NAME,
                Constants.DEFAULT_CORE_THREADS, Constants.DEFAULT_MAX_THREADS, Boolean.FALSE);
        shareScheduler = new ScheduledThreadPoolExecutor(Math.min(Constants.CPUS, 4),
                // Parameters can be considered for startup parameter configuration
                new NamedThreadFactory("trpc_share_scheduler"));
    }

    /**
     * Get the shared global scheduler.
     */
    public static ScheduledExecutorService getShareScheduler() {
        return shareScheduler;
    }

    public static Executor getShutdownExecutor() {
        return SHUTDOWN_EXECUTOR;
    }

    /**
     * If the user overrides the corresponding registration configuration, it will no longer be registered
     */
    public static void registDefaultPluginConfig() {
        if (ExtensionLoader.getPluginConfig(WorkerPool.class,
                DEF_CONSUMER_WORKER_POOL_CONFIG.getName()) == null) {
            ExtensionLoader.registerPlugin(DEF_CONSUMER_WORKER_POOL_CONFIG);
        }
        if (ExtensionLoader.getPluginConfig(WorkerPool.class,
                DEF_PROVIDER_WORKER_POOL_CONFIG.getName()) == null) {
            ExtensionLoader.registerPlugin(DEF_PROVIDER_WORKER_POOL_CONFIG);
        }
        if (ExtensionLoader.getPluginConfig(WorkerPool.class,
                DEF_NAMING_WORKER_POOL_CONFIG.getName()) == null) {
            ExtensionLoader.registerPlugin(DEF_NAMING_WORKER_POOL_CONFIG);
        }
    }

    public static void validate(String name) {
        manager.validate(name);
    }

    public static WorkerPool get(String name) {
        return manager.get(name);
    }

    public static List<WorkerPool> getAllInitializedExtension() {
        return manager.getAllInitializedExtension();
    }

    public static void refresh(String name, PluginConfig newConfig) {
        manager.refresh(name, newConfig);
    }

    public static PluginConfig newThreadWorkerPoolConfig(String name, int thread, boolean useFiber) {
        return ThreadWorkerPool.newThreadWorkerPoolConfig(name, thread, useFiber);
    }

    public static PluginConfig newThreadWorkerPoolConfig(String name, int corePoolSize,
            int maxPoolSize, boolean useFiber) {
        return ThreadWorkerPool.newThreadWorkerPoolConfig(name, corePoolSize, maxPoolSize, useFiber);
    }

    /**
     * Close shared thread pool, graceful shutdown.
     */
    public static synchronized void shutdown(final long timeout, final TimeUnit unit) {
        if (CLOSED_FLAG.compareAndSet(Boolean.FALSE, Boolean.TRUE)) {
            try {
                if (!shareScheduler.isShutdown()) {
                    if (timeout <= 0) {
                        shareScheduler.shutdownNow();
                    } else {
                        shareScheduler.shutdown();
                        shareScheduler.awaitTermination(timeout, unit);
                    }
                }
            } catch (Exception e) {
                LOG.error("Shut down WorkerPoolManager exception", e);
            }
            manager.getAllInitializedExtension().forEach((v) -> v.close(unit.toMillis(timeout)));

        }
    }

    /**
     * For Test purpose
     */
    public static synchronized void reset() {
        CLOSED_FLAG.set(false);
        shareScheduler = new ScheduledThreadPoolExecutor(Math.min(Constants.CPUS, 4),
                // Parameters can be considered for startup parameter configuration
                new NamedThreadFactory("trpc_share_scheduler"));
    }
    
}