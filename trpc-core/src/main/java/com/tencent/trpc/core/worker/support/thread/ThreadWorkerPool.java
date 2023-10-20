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

package com.tencent.trpc.core.worker.support.thread;

import com.tencent.trpc.core.common.NamedThreadFactory;
import com.tencent.trpc.core.common.config.PluginConfig;
import com.tencent.trpc.core.exception.LifecycleException;
import com.tencent.trpc.core.exception.TRpcExtensionException;
import com.tencent.trpc.core.extension.DisposableExtension;
import com.tencent.trpc.core.extension.Extension;
import com.tencent.trpc.core.extension.InitializingExtension;
import com.tencent.trpc.core.extension.PluginConfigAware;
import com.tencent.trpc.core.extension.RefreshableExtension;
import com.tencent.trpc.core.logger.Logger;
import com.tencent.trpc.core.logger.LoggerFactory;
import com.tencent.trpc.core.management.PoolMXBean;
import com.tencent.trpc.core.management.ThreadPoolMXBean;
import com.tencent.trpc.core.management.ThreadPoolMXBeanImpl;
import com.tencent.trpc.core.management.support.MBeanRegistryHelper;
import com.tencent.trpc.core.worker.AbstractWorkerPool;
import com.tencent.trpc.core.worker.handler.TrpcThreadExceptionHandler;
import com.tencent.trpc.core.worker.spi.WorkerPool;
import java.lang.Thread.Builder.OfVirtual;
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@Extension(ThreadWorkerPool.TYPE)
public class ThreadWorkerPool extends AbstractWorkerPool
        implements PluginConfigAware, InitializingExtension, DisposableExtension,
        RefreshableExtension {

    public static final String TYPE = "thread";

    protected static final Logger logger = LoggerFactory.getLogger(ThreadWorkerPool.class);
    private ExecutorService threadPool;
    private ThreadPoolConfig poolConfig;
    private PluginConfig config;
    private ThreadPoolMXBean threadPoolMXBean;
    private AtomicLong errorCount;
    private AtomicLong businessError;
    private AtomicLong protocolError;
    private UncaughtExceptionHandler uncaughtExceptionHandler;

    public static PluginConfig newThreadWorkerPoolConfig(String name, int corePoolSize, boolean useFiber) {
        ThreadPoolConfig poolConfig = new ThreadPoolConfig();
        poolConfig.setUseFiber(useFiber);
        poolConfig.setCorePoolSize(corePoolSize);
        poolConfig.setMaximumPoolSize(corePoolSize);
        poolConfig.setShareSchedule(Boolean.TRUE);
        return new PluginConfig(name, WorkerPool.class, ThreadWorkerPool.class, poolConfig.toMap());
    }

    @Override
    public void init() throws TRpcExtensionException {
        Objects.requireNonNull(config, "config");
        Map<String, Object> configMap = config.getProperties();
        poolConfig = ThreadPoolConfig.parse(config.getName(), configMap);
        poolConfig.validate();

        errorCount = new AtomicLong(0);
        businessError = new AtomicLong(0);
        protocolError = new AtomicLong(0);
        uncaughtExceptionHandler = new TrpcThreadExceptionHandler(errorCount, businessError, protocolError);

        ThreadFactory threadFactory;
        if (poolConfig.useFiber()) {
            OfVirtual virtual = Thread.ofVirtual().name(poolConfig.getNamePrefix(), 1);
            if (!poolConfig.isShareSchedule()) {
                virtual.scheduler(Executors.newWorkStealingPool(poolConfig.getFiberParallel()));
            }
            threadFactory = virtual.factory();
        } else {
            threadFactory = new NamedThreadFactory(poolConfig.getNamePrefix(), poolConfig.isDaemon(),
                    uncaughtExceptionHandler);
        }

        threadPool = new ThreadPoolExecutor(poolConfig.getCorePoolSize(),
                poolConfig.getMaximumPoolSize(), poolConfig.getKeepAliveTimeSeconds(),
                TimeUnit.SECONDS, poolConfig.getQueueSize() <= 0 ? new LinkedTransferQueue<>()
                : new LinkedBlockingQueue<>(poolConfig.getQueueSize()), threadFactory);
        ((ThreadPoolExecutor) threadPool).allowCoreThreadTimeOut(poolConfig.isAllowCoreThreadTimeOut());
        threadPoolMXBean = new ThreadPoolMXBeanImpl((ThreadPoolExecutor) threadPool);
        MBeanRegistryHelper.registerMBean(threadPoolMXBean, threadPoolMXBean.getObjectName());
    }


    @Override
    public void setPluginConfig(PluginConfig pluginConfig) throws TRpcExtensionException {
        this.config = pluginConfig;
    }

    @Override
    public void destroy() throws TRpcExtensionException {
        close(poolConfig.getCloseTimeout());
    }

    @Override
    public void close(long timeoutMills) {
        if (this.threadPool != null) {
            if (this.poolConfig == null || timeoutMills <= 0) {
                this.threadPool.shutdownNow();
            } else {
                shutdownGraceful(timeoutMills);
            }
        }
    }

    @Override
    public void refresh(PluginConfig pluginConfig) throws TRpcExtensionException {
        String name = config.getName();
        Map<String, Object> configMap = config.getProperties();
        ThreadPoolConfig poolConfig = ThreadPoolConfig.parse(name, configMap);
        if (poolConfig.getCorePoolSize() < 0) {
            throw new IllegalArgumentException(
                    "Refresh fail, CorePoolSize < 0, pluginConfig={" + pluginConfig + "}");
        }
        if (poolConfig.getMaximumPoolSize() < 0) {
            throw new IllegalArgumentException(
                    "Refresh fail, MaximumPoolSize < 0, pluginConfig={" + pluginConfig + "}");
        }
        if (threadPool instanceof ThreadPoolExecutor) {
            ((ThreadPoolExecutor) threadPool).setCorePoolSize(poolConfig.getCorePoolSize());
            ((ThreadPoolExecutor) threadPool).setMaximumPoolSize(poolConfig.getMaximumPoolSize());
        }
    }

    public void shutdownGraceful(long timeoutMills) {
        this.threadPool.shutdown();
        try {
            this.threadPool.awaitTermination(timeoutMills, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            throw new LifecycleException(e);
        }
    }

    @Override
    public Executor toExecutor() {
        return threadPool;
    }

    @Override
    public String getName() {
        return config.getName();
    }

    @Override
    public void execute(Task task) {
        threadPool.execute(() -> {
            try {
                task.run();
            } catch (Throwable ex) {
                logger.error("", ex);
            }
        });
    }

    @Override
    public PoolMXBean report() {
        return threadPoolMXBean;
    }

    @Override
    public UncaughtExceptionHandler getUncaughtExceptionHandler() {
        return this.uncaughtExceptionHandler;
    }

}
