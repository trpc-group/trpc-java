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

import com.tencent.trpc.core.common.config.PluginConfig;
import com.tencent.trpc.core.worker.spi.WorkerPool;
import com.tencent.trpc.core.worker.support.thread.ThreadPoolConfig;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class WorkerPoolManagerTest {

    /**
     * Test {@link WorkerPoolManager#getShareScheduler()}} method
     */
    @Test
    public void testGetShareScheduler() {
        ScheduledExecutorService shareScheduler = WorkerPoolManager.getShareScheduler();
        Assertions.assertNotNull(shareScheduler);
    }

    /**
     * Test {@link WorkerPoolManager#getShutdownExecutor()}} method
     */
    @Test
    public void testGetShutdownExecutor() {
        Executor shutdownExecutor = WorkerPoolManager.getShutdownExecutor();
        Assertions.assertNotNull(shutdownExecutor);
    }

    /**
     * Test {@link WorkerPoolManager#registDefaultPluginConfig()}} method
     */
    @Test
    public void testRegistDefaultPluginConfig() {
        WorkerPoolManager.registDefaultPluginConfig();
        WorkerPool consumer = WorkerPoolManager.get(WorkerPoolManager.DEF_CONSUMER_WORKER_POOL_NAME);
        Assertions.assertNotNull(consumer);
        WorkerPool provider = WorkerPoolManager.get(WorkerPoolManager.DEF_PROVIDER_WORKER_POOL_NAME);
        Assertions.assertNotNull(provider);
        WorkerPool naming = WorkerPoolManager.get(WorkerPoolManager.DEF_NAMING_WORKER_POOL_NAME);
        Assertions.assertNotNull(naming);
    }

    /**
     * Test {@link WorkerPoolManager#validate(String)} method
     */
    @Test
    public void testValidate() {
        WorkerPoolManager.registDefaultPluginConfig();
        WorkerPoolManager.validate(WorkerPoolManager.DEF_CONSUMER_WORKER_POOL_NAME);
        WorkerPoolManager.validate(WorkerPoolManager.DEF_PROVIDER_WORKER_POOL_NAME);
        WorkerPoolManager.validate(WorkerPoolManager.DEF_NAMING_WORKER_POOL_NAME);
    }

    /**
     * Test {@link WorkerPoolManager#get(String)} method
     */
    @Test
    public void testGet() {
        this.testRegistDefaultPluginConfig();
    }

    /**
     * Test {@link WorkerPoolManager#getAllInitializedExtension()} method
     */
    @Test
    public void testGetAllInitializedExtension() {
        this.testRegistDefaultPluginConfig();
        List<WorkerPool> allInitializedExtension = WorkerPoolManager.getAllInitializedExtension();
        Assertions.assertTrue(allInitializedExtension.contains(
                WorkerPoolManager.get(WorkerPoolManager.DEF_CONSUMER_WORKER_POOL_NAME)));
        Assertions.assertTrue(allInitializedExtension.contains(
                WorkerPoolManager.get(WorkerPoolManager.DEF_PROVIDER_WORKER_POOL_NAME)));
        Assertions.assertTrue(allInitializedExtension.contains(
                WorkerPoolManager.get(WorkerPoolManager.DEF_NAMING_WORKER_POOL_NAME)));
    }

    /**
     * Test {@link WorkerPoolManager#refresh(String, PluginConfig)} method
     */
    @Test
    public void testRefresh() {
        this.testRegistDefaultPluginConfig();
        PluginConfig pluginConfig = WorkerPoolManager
                .newThreadWorkerPoolConfig(WorkerPoolManager.DEF_CONSUMER_WORKER_POOL_NAME, 20, Boolean.FALSE);
        WorkerPoolManager.refresh(WorkerPoolManager.DEF_NAMING_WORKER_POOL_NAME, pluginConfig);
        WorkerPool workerPool = WorkerPoolManager.get(WorkerPoolManager.DEF_CONSUMER_WORKER_POOL_NAME);
        Assertions.assertNotNull(workerPool);
    }

    @Test
    public void testWorkPool() {
        this.testRegistDefaultPluginConfig();
        PluginConfig pluginConfig = WorkerPoolManager
                .newThreadWorkerPoolConfig(WorkerPoolManager.DEF_CONSUMER_WORKER_POOL_NAME,
                        20,20, Boolean.FALSE);
        WorkerPoolManager.refresh(WorkerPoolManager.DEF_NAMING_WORKER_POOL_NAME, pluginConfig);
        WorkerPool workerPool = WorkerPoolManager.get(WorkerPoolManager.DEF_CONSUMER_WORKER_POOL_NAME);
        Assertions.assertNotNull(workerPool);
    }

    /**
     * Test {@link WorkerPoolManager#newThreadWorkerPoolConfig(String, int, boolean)} method
     */
    @Test
    public void testNewThreadWorkerPoolConfig() {
        PluginConfig pluginConfig = WorkerPoolManager
                .newThreadWorkerPoolConfig(WorkerPoolManager.DEF_CONSUMER_WORKER_POOL_NAME, 20, Boolean.FALSE);
        Assertions.assertNotNull(pluginConfig);
        Assertions.assertEquals(WorkerPoolManager.DEF_CONSUMER_WORKER_POOL_NAME, pluginConfig.getName());
        Assertions.assertEquals(WorkerPool.class, pluginConfig.getPluginInterface());
        Assertions.assertEquals(20, pluginConfig.getProperties().get("core_pool_size"));
        Assertions.assertFalse((Boolean) pluginConfig.getProperties().get("use_fiber"));
        Assertions.assertTrue((Boolean) pluginConfig.getProperties().get("share_schedule"));

        pluginConfig = WorkerPoolManager.newThreadWorkerPoolConfig(WorkerPoolManager.DEF_CONSUMER_WORKER_POOL_NAME,
                2000, Boolean.TRUE);
        Assertions.assertNotNull(pluginConfig);
        ThreadPoolConfig.validate(pluginConfig);
        Assertions.assertEquals(WorkerPoolManager.DEF_CONSUMER_WORKER_POOL_NAME, pluginConfig.getName());
        Assertions.assertEquals(WorkerPool.class, pluginConfig.getPluginInterface());
        Assertions.assertEquals(2000, pluginConfig.getProperties().get("core_pool_size"));
        Assertions.assertTrue((Boolean) pluginConfig.getProperties().get("use_fiber"));
        Assertions.assertTrue((Boolean) pluginConfig.getProperties().get("share_schedule"));
    }

    /**
     * Test {@link WorkerPoolManager#shutdown(long, TimeUnit)} method
     */
    @Test
    public void testShutdown() {
        WorkerPoolManager.shutdown(20, TimeUnit.SECONDS);
    }

    /**
     * Test {@link WorkerPoolManager#reset()} method
     */
    @Test
    public void testReset() {
        WorkerPoolManager.reset();
        Assertions.assertNotNull(WorkerPoolManager.getShareScheduler());
    }

}
