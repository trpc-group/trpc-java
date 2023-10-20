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
import com.tencent.trpc.core.management.ForkJoinPoolMXBean;
import com.tencent.trpc.core.management.ForkJoinPoolMXBeanImpl;
import com.tencent.trpc.core.management.PoolMXBean;
import com.tencent.trpc.core.management.support.MBeanRegistryHelper;
import com.tencent.trpc.core.worker.AbstractWorkerPool;
import com.tencent.trpc.core.worker.handler.TrpcThreadExceptionHandler;
import java.lang.Thread.UncaughtExceptionHandler;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Fork join thread pool.
 */
@Extension(ForkJoinWorkerPool.NAME)
public class ForkJoinWorkerPool extends AbstractWorkerPool implements PluginConfigAware, InitializingExtension,
        DisposableExtension, RefreshableExtension {

    /**
     * Plugin name.
     */
    public static final String NAME = "forkJoinPool";
    protected static final Logger logger = LoggerFactory.getLogger(ForkJoinWorkerPool.class);
    /**
     * Actual forkJoinPool.
     */
    private volatile ForkJoinPool forkJoinPool;
    /**
     * Fork join pool configuration class.
     */
    private ForkJoinPoolConfig poolConfig;
    /**
     * Plugin configuration.
     */
    private PluginConfig pluginConfig;
    /**
     * Thread pool MXBean implementation.
     */
    private ForkJoinPoolMXBean forkJoinPoolMXBean;

    private AtomicLong errorCount;

    private AtomicLong businessError;

    private AtomicLong protocolError;

    private UncaughtExceptionHandler uncaughtExceptionHandler;

    private BigDecimal latencyP1;

    private BigDecimal latencyP2;

    private BigDecimal latencyP3;

    private BigDecimal latency999;

    private BigDecimal latency9999;

    private BigInteger totalCost;

    private BigDecimal latencyMin;

    private BigDecimal latencyMax;

    @Override
    public String getName() {
        return pluginConfig.getName();
    }

    @Override
    public Executor toExecutor() {
        return this.forkJoinPool;
    }

    @Override
    public void execute(Task task) throws RejectedExecutionException {
        forkJoinPool.execute(() -> {
            try {
                task.run();
            } catch (Throwable ex) {
                logger.error("execute task failure:", ex);
            }
        });

    }

    @Override
    public PoolMXBean report() {
        return forkJoinPoolMXBean;
    }

    @Override
    public void close(long timeoutMills) {
        if (this.forkJoinPool != null) {
            if (timeoutMills <= 0) {
                this.forkJoinPool.shutdownNow();
            } else {
                shutdownGraceful(timeoutMills);
            }
        }
    }

    private void shutdownGraceful(long timeoutMills) {
        this.forkJoinPool.shutdown();
        try {
            this.forkJoinPool.awaitTermination(timeoutMills, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            throw new LifecycleException(e);
        }
    }

    @Override
    public void destroy() throws TRpcExtensionException {
        close(poolConfig.getTimeoutMills());
    }

    @Override
    public void init() throws TRpcExtensionException {
        Map<String, Object> configMap = pluginConfig.getProperties();
        this.poolConfig = ForkJoinPoolConfig.parse(pluginConfig.getName(), configMap);
        errorCount = new AtomicLong(0);
        businessError = new AtomicLong(0);
        protocolError = new AtomicLong(0);
        this.uncaughtExceptionHandler = new TrpcThreadExceptionHandler(errorCount, businessError, protocolError);
        this.forkJoinPool = new ForkJoinPool(poolConfig.getParallelism(),
                ForkJoinPool.defaultForkJoinWorkerThreadFactory,
                uncaughtExceptionHandler, false);
        this.forkJoinPoolMXBean = new ForkJoinPoolMXBeanImpl(forkJoinPool);
        MBeanRegistryHelper.registerMBean(forkJoinPoolMXBean, forkJoinPoolMXBean.getObjectName());
    }

    @Override
    public void setPluginConfig(PluginConfig pluginConfig) throws TRpcExtensionException {
        this.pluginConfig = pluginConfig;
    }

    @Override
    public void refresh(PluginConfig pluginConfig) throws TRpcExtensionException {
        throw new UnsupportedOperationException(" not support refresh");
    }

    @Override
    public UncaughtExceptionHandler getUncaughtExceptionHandler() {
        return this.uncaughtExceptionHandler;
    }

}
