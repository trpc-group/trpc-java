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
import com.tencent.trpc.core.management.ForkJoinPoolMXBean;
import com.tencent.trpc.core.management.PoolMXBean;
import com.tencent.trpc.core.management.PoolMXBean.WorkerPoolType;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

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
        Assertions.assertEquals(ForkJoinWorkerPool.NAME, forkJoinWorkerPool.getName());
        forkJoinWorkerPool.init();
        try {
            forkJoinWorkerPool.refresh(poolPluginConfig);
        } catch (Exception e) {
            Assertions.assertTrue(e instanceof UnsupportedOperationException);
        }
        PoolMXBean report = forkJoinWorkerPool.report();
        ForkJoinPoolMXBean forkJoinPoolMXBean = (ForkJoinPoolMXBean) report;
        Assertions.assertEquals(0, forkJoinPoolMXBean.getPoolSize());
        Assertions.assertEquals(0, forkJoinPoolMXBean.getQueuedTaskCount());
        Assertions.assertEquals(0, forkJoinPoolMXBean.getQueuedSubmissionCount());
        Assertions.assertTrue(forkJoinPoolMXBean.getCommonPoolParallelism() >= 0);
        Assertions.assertTrue(forkJoinPoolMXBean.getParallelism() >= 0);
        Assertions.assertEquals(WorkerPoolType.FORK_JOIN.getName(), forkJoinPoolMXBean.getType());
        Assertions.assertNotNull(forkJoinPoolMXBean.getObjectName());
        Assertions.assertEquals(0, forkJoinPoolMXBean.getRunningThreadCount());
        Assertions.assertEquals(0, forkJoinPoolMXBean.getActiveThreadCount());
        Assertions.assertEquals(0, forkJoinPoolMXBean.getStealCount());
        forkJoinWorkerPool.execute(() -> System.out.println("hello"));
        Executor executor = forkJoinWorkerPool.toExecutor();
        Assertions.assertTrue(executor instanceof ForkJoinPool);
        forkJoinWorkerPool.destroy();
        executor = forkJoinWorkerPool.toExecutor();
        ForkJoinPool forkJoinPool = (ForkJoinPool) executor;
        Assertions.assertTrue(forkJoinPool.isShutdown());
        forkJoinWorkerPool.close(0);
    }
}
