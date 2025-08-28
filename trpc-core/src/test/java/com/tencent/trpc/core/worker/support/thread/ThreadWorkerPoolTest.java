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
import com.tencent.trpc.core.exception.TRpcException;
import com.tencent.trpc.core.exception.TRpcExtensionException;
import com.tencent.trpc.core.management.PoolMXBean;
import com.tencent.trpc.core.management.PoolMXBean.WorkerPoolType;
import com.tencent.trpc.core.management.ThreadPerTaskExecutorMXBeanImpl;
import com.tencent.trpc.core.management.ThreadPoolMXBean;
import com.tencent.trpc.core.management.support.MBeanRegistryHelper;
import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.Map;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import org.junit.Assert;
import org.junit.Test;

public class ThreadWorkerPoolTest {

    public static final int DEFALUT_POOL_SIZE = 2;

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
        properties.put(ThreadPoolConfig.CORE_POOL_SIZE, DEFALUT_POOL_SIZE);
        properties.put(ThreadPoolConfig.MAXIMUM_POOL_SIZE, DEFALUT_POOL_SIZE);
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
    public void testVirtualThreads() {
        Map<String, Object> properties = getProperties();
        properties.put(ThreadPoolConfig.USE_VIRTUAL_THREAD_PER_TASK_EXECUTOR, Boolean.TRUE);
        PluginConfig poolPluginConfig = new PluginConfig("work_pool", ThreadWorkerPool.class,
                properties);
        ThreadWorkerPool threadWorkerPool = new ThreadWorkerPool();
        threadWorkerPool.setPluginConfig(poolPluginConfig);
        try {
            threadWorkerPool.init();
        } catch (TRpcExtensionException e) {
            // not in java21+
            return;
        }
        PoolMXBean report = threadWorkerPool.report();
        ThreadPoolMXBean threadPoolMXBean = (ThreadPoolMXBean) report;
        Assert.assertEquals(0, threadPoolMXBean.getPoolSize());
        Assert.assertEquals(0, threadPoolMXBean.getTaskCount());
        Assert.assertEquals(0, threadPoolMXBean.getCompletedTaskCount());
        Assert.assertEquals(0, threadPoolMXBean.getActiveThreadCount());
        Assert.assertEquals(WorkerPoolType.THREAD.getName(), threadPoolMXBean.getType());
        if (threadPoolMXBean instanceof ThreadPerTaskExecutorMXBeanImpl) {
            Assert.assertEquals(0, threadPoolMXBean.getCorePoolSize());
            Assert.assertEquals(Integer.MAX_VALUE, threadPoolMXBean.getMaximumPoolSize());
        } else {
            Assert.assertEquals(DEFALUT_POOL_SIZE, threadPoolMXBean.getCorePoolSize());
            Assert.assertEquals(DEFALUT_POOL_SIZE, threadPoolMXBean.getMaximumPoolSize());
        }
        Assert.assertNotNull(report.toString());
    }

    /**
     * Test MBean unregistration when closing ThreadWorkerPool
     */
    @Test
    public void testMBeanUnregistrationOnClose() throws Exception {
        Map<String, Object> properties = getProperties();
        PluginConfig poolPluginConfig = new PluginConfig("work_pool", ThreadWorkerPool.class,
                properties);
        ThreadWorkerPool threadWorkerPool = new ThreadWorkerPool();
        threadWorkerPool.setPluginConfig(poolPluginConfig);
        threadWorkerPool.init();
        
        // Get the MXBean and its object name
        PoolMXBean report = threadWorkerPool.report();
        ThreadPoolMXBean threadPoolMXBean = (ThreadPoolMXBean) report;
        ObjectName objectName = threadPoolMXBean.getObjectName();
        
        // Verify MBean is registered
        MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
        Assert.assertTrue("MBean should be registered after init", 
                mBeanServer.isRegistered(objectName));
        
        // Close the worker pool
        threadWorkerPool.close(1000);
        
        // Verify MBean is unregistered after close
        Assert.assertFalse("MBean should be unregistered after close", 
                mBeanServer.isRegistered(objectName));
    }

}