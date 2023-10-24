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
import com.tencent.trpc.core.exception.TRpcException;
import com.tencent.trpc.core.management.PoolMXBean;
import com.tencent.trpc.core.management.PoolMXBean.WorkerPoolType;
import com.tencent.trpc.core.management.ThreadPoolMXBean;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.junit.Assert;
import org.junit.Test;
import org.reflections.ReflectionUtils;

public class ThreadWorkerPoolTest {

    @Test
    public void testInit() {
        Map<String, Object> properties = getProperties();
        PluginConfig poolPluginConfig = new PluginConfig("work_pool", ThreadWorkerPool.class,
                properties);
        ThreadWorkerPool threadWorkerPool = new ThreadWorkerPool();
        threadWorkerPool.setPluginConfig(poolPluginConfig);
        threadWorkerPool.init();
        Assert.assertEquals("work_pool", threadWorkerPool.getName());
        threadWorkerPool.execute(() -> {
            throw new TRpcException();
        });
        threadWorkerPool.destroy();
        properties.put(ThreadPoolConfig.SHARE_SCHEDULE, Boolean.TRUE);
        properties.put(ThreadPoolConfig.QUEUE_SIZE, 100);
        threadWorkerPool.setPluginConfig(poolPluginConfig);
        threadWorkerPool.init();
        PoolMXBean report = threadWorkerPool.report();
        ThreadPoolMXBean threadPoolMXBean = (ThreadPoolMXBean) report;
        Assert.assertEquals(0, threadPoolMXBean.getPoolSize());
        Assert.assertEquals(0, threadPoolMXBean.getTaskCount());
        Assert.assertEquals(0, threadPoolMXBean.getCompletedTaskCount());
        Assert.assertEquals(0, threadPoolMXBean.getActiveThreadCount());
        Assert.assertEquals(WorkerPoolType.THREAD.getName(), threadPoolMXBean.getType());
        Assert.assertEquals(2, threadPoolMXBean.getCorePoolSize());
        Assert.assertEquals(2, threadPoolMXBean.getMaximumPoolSize());
        Assert.assertNotNull(report.toString());
    }

    private Map<String, Object> getProperties() {
        Map<String, Object> properties = new HashMap<>();
        properties.put(ThreadPoolConfig.CORE_POOL_SIZE, 2);
        properties.put(ThreadPoolConfig.MAXIMUM_POOL_SIZE, 2);
        properties.put(ThreadPoolConfig.KEEP_ALIVE_TIME_SECONDS, 300000);
        properties.put(ThreadPoolConfig.QUEUE_SIZE, 0);
        properties.put(ThreadPoolConfig.NAME_PREFIX, "test");
        properties.put(ThreadPoolConfig.DAEMON, Boolean.FALSE);
        properties.put(ThreadPoolConfig.CLOSE_TIMEOUT, 10 * 1000);
        properties.put(ThreadPoolConfig.ALLOW_CORE_THREAD_TIMEOUT, Boolean.TRUE);
        return properties;
    }

    @Test
    public void testCoroutines() {
        Map<String, Object> properties = getProperties();
        properties.put(ThreadPoolConfig.USE_FIBER, Boolean.TRUE);
        properties.put(ThreadPoolConfig.SHARE_SCHEDULE, Boolean.FALSE);
        properties.put(ThreadPoolConfig.FIBER_PARALLEL, 2);
        PluginConfig poolPluginConfig = new PluginConfig("work_pool", ThreadWorkerPool.class,
                properties);
        ThreadWorkerPool threadWorkerPool = new ThreadWorkerPool();
        threadWorkerPool.setPluginConfig(poolPluginConfig);
        threadWorkerPool.init();
    }

    @Test
    public void testCreateThreadFactory()
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, InterruptedException {
        Class<?> thread = ReflectionUtils.forName("java.lang.Thread");
        Method ofVirtualMethod = thread.getDeclaredMethod("ofVirtual");
        Object virtual = ofVirtualMethod.invoke(thread);
        Class<?> virtualClazz = virtual.getClass();
        Method nameMethod = virtualClazz.getMethod("name", String.class, long.class);
        nameMethod.setAccessible(true);
        nameMethod.invoke(virtual, "test", 1);
        Method schedulerMethod = virtualClazz.getDeclaredMethod("scheduler", Executor.class);
        schedulerMethod.setAccessible(true);
        schedulerMethod.invoke(virtual, Executors.newWorkStealingPool(2));
        Method factoryMethod = virtualClazz.getDeclaredMethod("factory");
        factoryMethod.setAccessible(true);
        ThreadFactory threadFactory = (ThreadFactory) factoryMethod.invoke(virtual);
        ExecutorService threadPool = new ThreadPoolExecutor(2,
                2, 10,
                TimeUnit.SECONDS, new LinkedBlockingQueue<>(20), threadFactory);
        threadPool.submit(() -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("testCreateThreadFactory");
        });
        threadPool.awaitTermination(2, TimeUnit.SECONDS);
    }
}