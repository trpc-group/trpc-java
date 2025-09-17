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

package com.tencent.trpc.core.worker.support.thread;

import com.tencent.trpc.core.common.config.PluginConfig;
import com.tencent.trpc.core.management.ForkJoinPoolMXBean;
import com.tencent.trpc.core.management.PoolMXBean;
import com.tencent.trpc.core.management.PoolMXBean.WorkerPoolType;
import com.tencent.trpc.core.management.support.MBeanRegistryHelper;
import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import org.junit.Assert;
import org.junit.Test;

public class ForkJoinWorkerPoolTest {

    /**
     * Number of parallel threads.
     */
    private static final int PARALLELISM = 2;
    /**
     * Thread pool shutdown timeout ms.
     */
    private static final int TIMEOUT_MILLS = 20000;

    /**
     * Test ForkJoinWorkerPool all method
     */
    @Test
    public void testForkJoinWorkerPool() {
        Map<String, Object> properties = new HashMap<>();
        properties.put(ForkJoinPoolConfig.PARALLELISM, PARALLELISM);
        properties.put(ForkJoinPoolConfig.TIMEOUT_MS, TIMEOUT_MILLS);
        PluginConfig poolPluginConfig = new PluginConfig(ForkJoinWorkerPool.NAME, ThreadWorkerPool.class,
                properties);
        ForkJoinWorkerPool forkJoinWorkerPool = new ForkJoinWorkerPool();
        forkJoinWorkerPool.setPluginConfig(poolPluginConfig);
        Assert.assertEquals(ForkJoinWorkerPool.NAME, forkJoinWorkerPool.getName());
        forkJoinWorkerPool.init();
        try {
            forkJoinWorkerPool.refresh(poolPluginConfig);
        } catch (Exception e) {
            Assert.assertTrue(e instanceof UnsupportedOperationException);
        }
        PoolMXBean report = forkJoinWorkerPool.report();
        ForkJoinPoolMXBean forkJoinPoolMXBean = (ForkJoinPoolMXBean) report;
        Assert.assertEquals(0, forkJoinPoolMXBean.getPoolSize());
        Assert.assertEquals(0, forkJoinPoolMXBean.getQueuedTaskCount());
        Assert.assertEquals(0, forkJoinPoolMXBean.getQueuedSubmissionCount());
        Assert.assertTrue(forkJoinPoolMXBean.getCommonPoolParallelism() >= 0);
        Assert.assertTrue(forkJoinPoolMXBean.getParallelism() >= 0);
        Assert.assertEquals(WorkerPoolType.FORK_JOIN.getName(), forkJoinPoolMXBean.getType());
        Assert.assertNotNull(forkJoinPoolMXBean.getObjectName());
        Assert.assertEquals(0, forkJoinPoolMXBean.getRunningThreadCount());
        Assert.assertEquals(0, forkJoinPoolMXBean.getActiveThreadCount());
        Assert.assertEquals(0, forkJoinPoolMXBean.getStealCount());
        forkJoinWorkerPool.execute(() -> System.out.println("hello"));
        Executor executor = forkJoinWorkerPool.toExecutor();
        Assert.assertTrue(executor instanceof ForkJoinPool);
        forkJoinWorkerPool.destroy();
        executor = forkJoinWorkerPool.toExecutor();
        ForkJoinPool forkJoinPool = (ForkJoinPool) executor;
        Assert.assertTrue(forkJoinPool.isShutdown());
        forkJoinWorkerPool.close(0);
    }

    /**
     * Test MBean unregistration when closing ForkJoinWorkerPool
     */
    @Test
    public void testMBeanUnregistrationOnClose() throws Exception {
        Map<String, Object> properties = new HashMap<>();
        properties.put(ForkJoinPoolConfig.PARALLELISM, PARALLELISM);
        properties.put(ForkJoinPoolConfig.TIMEOUT_MS, TIMEOUT_MILLS);
        PluginConfig poolPluginConfig = new PluginConfig(ForkJoinWorkerPool.NAME, ThreadWorkerPool.class,
                properties);
        ForkJoinWorkerPool forkJoinWorkerPool = new ForkJoinWorkerPool();
        forkJoinWorkerPool.setPluginConfig(poolPluginConfig);
        forkJoinWorkerPool.init();
        
        // Get the MXBean and its object name
        PoolMXBean report = forkJoinWorkerPool.report();
        ForkJoinPoolMXBean forkJoinPoolMXBean = (ForkJoinPoolMXBean) report;
        ObjectName objectName = forkJoinPoolMXBean.getObjectName();
        
        // Verify MBean is registered
        MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
        Assert.assertTrue("MBean should be registered after init", 
                mBeanServer.isRegistered(objectName));
        
        // Close the worker pool
        forkJoinWorkerPool.close(1000);
        
        // Verify MBean is unregistered after close
        Assert.assertFalse("MBean should be unregistered after close", 
                mBeanServer.isRegistered(objectName));
    }

    /**
     * Test close method when forkJoinPoolMXBean is null (covers the null check branch)
     */
    @Test
    public void testCloseWithNullMXBean() throws Exception {
        Map<String, Object> properties = new HashMap<>();
        properties.put(ForkJoinPoolConfig.PARALLELISM, PARALLELISM);
        properties.put(ForkJoinPoolConfig.TIMEOUT_MS, TIMEOUT_MILLS);
        PluginConfig poolPluginConfig = new PluginConfig(ForkJoinWorkerPool.NAME, ThreadWorkerPool.class,
                properties);
        ForkJoinWorkerPool forkJoinWorkerPool = new ForkJoinWorkerPool();
        forkJoinWorkerPool.setPluginConfig(poolPluginConfig);
        
        // Don't call init() so forkJoinPoolMXBean remains null
        // This should not throw any exception when closing
        forkJoinWorkerPool.close(1000);
        
        // Verify no exception is thrown and method completes successfully
        Assert.assertTrue("Close method should complete successfully even with null MXBean", true);
    }
}