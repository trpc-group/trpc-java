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

package com.tencent.trpc.core.common.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.google.common.collect.ImmutableMap;
import com.tencent.trpc.core.worker.spi.WorkerPool;
import com.tencent.trpc.core.worker.support.thread.ThreadWorkerPool;
import org.junit.Test;

public class PluginConfigTest {

    @Test
    public void test() {
        PluginConfig clientConfig = new PluginConfig("id", ThreadWorkerPool.class);
        assertEquals("id", clientConfig.getName());
        assertEquals(ThreadWorkerPool.class, clientConfig.getPluginClass());
        assertEquals(WorkerPool.class, clientConfig.getPluginInterface());
        PluginConfig clientConfig2 =
                new PluginConfig("id2", ThreadWorkerPool.class, ImmutableMap.of("a", 1));
        assertEquals("id2", clientConfig2.getName());
        assertEquals(ThreadWorkerPool.class, clientConfig2.getPluginClass());
        assertEquals(1, clientConfig2.getProperties().get("a"));
        assertTrue(clientConfig.toSimpleString().contains("PluginConfig"));
    }
}
