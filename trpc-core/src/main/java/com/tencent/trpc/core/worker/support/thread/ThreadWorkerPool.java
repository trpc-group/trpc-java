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
import com.tencent.trpc.core.management.ThreadPerTaskExecutorMXBeanImpl;
import com.tencent.trpc.core.management.ThreadPerTaskExecutorWrapper;
import com.tencent.trpc.core.management.ThreadPoolMXBean;
import com.tencent.trpc.core.management.ThreadPoolMXBeanImpl;
import com.tencent.trpc.core.management.support.MBeanRegistryHelper;
import com.tencent.trpc.core.worker.AbstractWorkerPool;
import com.tencent.trpc.core.worker.handler.TrpcThreadExceptionHandler;
import com.tencent.trpc.core.worker.spi.WorkerPool;
import java.lang.Thread.UncaughtExceptionHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
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
import org.reflections.ReflectionUtils;

@Extension(ThreadWorkerPool.TYPE)
public class ThreadWorkerPool extends AbstractWorkerPool
        implements PluginConfigAware, InitializingExtension, DisposableExtension,
        RefreshableExtension {

    public static final String TYPE = "thread";

    protected static final Logger logger = LoggerFactory.getLogger(ThreadWorkerPool.class);

    private static final String THREAD_CLASS_NAME = "java.lang.Thread";
    private static final String OF_VIRTUAL_NAME = "ofVirtual";
    private static final String NAME = "name";
    private static final String SCHEDULER_NAME = "scheduler";
    private static final String FACTORY_NAME = "factory";
    private static final String EXECUTORS_CLASS_NAME = "java.util.concurrent.Executors";
    private static final String NEW_THREAD_PER_TASK_EXECUTOR_NAME = "newThreadPerTaskExecutor";

    private ExecutorService threadPool;
    private ThreadPoolConfig poolConfig;
    private PluginConfig config;
    private ThreadPoolMXBean threadPoolMXBean;
    private AtomicLong errorCount;
    private AtomicLong businessError;
    private AtomicLong protocolError;
    private UncaughtExceptionHandler uncaughtExceptionHandler;

    public static PluginConfig newThreadWorkerPoolConfig(String name, int corePoolSize, boolean useFiber) {
        return newThreadWorkerPoolConfig(name, corePoolSize, corePoolSize, useFiber);
    }

    public static PluginConfig newThreadWorkerPoolConfig(String name,
            int corePoolSize, int maxPoolSize, boolean useFiber) {
        ThreadPoolConfig poolConfig = new ThreadPoolConfig();
        poolConfig.setUseFiber(useFiber);
        poolConfig.setCorePoolSize(corePoolSize);
        poolConfig.setMaximumPoolSize(maxPoolSize);
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

        ThreadFactory threadFactory = getThreadFactory(poolConfig);
        if (poolConfig.useVirtualThreadPerTaskExecutor()) {
            try {
                // Use JDK 21+ method Executors.newThreadPerTaskExecutor(ThreadFactory threadFactory)
                // to create a virtual thread executor service
                Class<?> executorsClazz = ReflectionUtils.forName(EXECUTORS_CLASS_NAME);
                Method newThreadPerTaskExecutorMethod = executorsClazz
                        .getDeclaredMethod(NEW_THREAD_PER_TASK_EXECUTOR_NAME, ThreadFactory.class);
                ThreadPerTaskExecutorWrapper wrappedThreadPool = ThreadPerTaskExecutorWrapper
                        .wrap((ExecutorService) newThreadPerTaskExecutorMethod.invoke(executorsClazz, threadFactory));
                threadPool = wrappedThreadPool;
                threadPoolMXBean = new ThreadPerTaskExecutorMXBeanImpl(wrappedThreadPool);
                MBeanRegistryHelper.registerMBean(threadPoolMXBean, threadPoolMXBean.getObjectName());
                logger.info("Successfully created an executor that assigns each task to a "
                        + "new virtual thread for processing");
                return;
            } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException exception) {
                logger.warn("The current JDK version does not support virtual threads, please use OpenJDK 21+, "
                        + "or remove use_virtual_thread_per_task_executor config, error: ", exception);
            }
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

    private ThreadFactory getThreadFactory(ThreadPoolConfig poolConfig) {
        ThreadFactory threadFactory = null;
        // Whether to use virtual threads
        if (poolConfig.useFiber() || poolConfig.useVirtualThreadPerTaskExecutor()) {
            try {
                // Since versions below OpenJDK 21 and Tencent JDK non-FIBER versions do not support virtual threads,
                // introducing the "java.lang.Thread.Builder.OfVirtual" dependency will result in an error,
                // so we create virtual threads through reflection, which is compatible with JDKs that do not support
                // virtual threads. When the JDK does not support virtual threads, it downgrades to thread.
                Class<?> threadClazz = ReflectionUtils.forName(THREAD_CLASS_NAME);
                Method ofVirtualMethod = threadClazz.getDeclaredMethod(OF_VIRTUAL_NAME);
                Object virtual = ofVirtualMethod.invoke(threadClazz);
                Class<?> virtualClazz = ofVirtualMethod.getReturnType();
                Method nameMethod = virtualClazz.getMethod(NAME, String.class, long.class);
                nameMethod.invoke(virtual, poolConfig.getNamePrefix(), 1);
                // Only Tencent Kona JDK FIBER 8+ version support the scheduler method, OpenJDK 21 version does not
                // support the scheduler method.
                if (poolConfig.useFiber()
                        && !poolConfig.isShareSchedule()
                        && containsMethod(virtualClazz.getDeclaredMethods(), SCHEDULER_NAME)) {
                    Method schedulerMethod = virtualClazz.getDeclaredMethod(SCHEDULER_NAME, Executor.class);
                    schedulerMethod.setAccessible(true);
                    schedulerMethod.invoke(virtual, Executors.newWorkStealingPool(poolConfig.getFiberParallel()));
                }
                Method factoryMethod = virtualClazz.getMethod(FACTORY_NAME);
                threadFactory = (ThreadFactory) factoryMethod.invoke(virtual);
                logger.info("Successfully created virtual thread factory");
                return threadFactory;
            } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException exception) {
                logger.error("The current JDK version cannot use virtual threads, please use OpenJDK 21+ or "
                        + "Tencent Kona JDK FIBER 8+ version, error: ", exception);
            }
        }
        // If virtual threads cannot be used, downgrade to threads
        threadFactory = new NamedThreadFactory(poolConfig.getNamePrefix(), poolConfig.isDaemon(),
                uncaughtExceptionHandler);
        logger.warn("Successfully created thread factory. If the server uses a synchronous interface, "
                + "please increase the thread pool size");
        return threadFactory;
    }

    private boolean containsMethod(Method[] methods, String methodName) {
        for (Method method : methods) {
            if (method.getName().equals(methodName)) {
                return true;
            }
        }
        return false;
    }

}
