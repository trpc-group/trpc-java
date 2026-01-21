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

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ForkJoinPoolConfigTest {

    private static final String FORK_JOIN_POOL_PLUGIN_NAME = "forkJoinPool";
    private static final Map<String, Object> PLUGIN_CONFIG = new HashMap<>();

    static {
        PLUGIN_CONFIG.put(ForkJoinPoolConfig.PARALLELISM, 2);
    }

    /**
     * Test {@link ForkJoinPoolConfig#parse(String, Map)}} method
     */
    @Test
    public void testParse() {
        ForkJoinPoolConfig forkJoinPoolConfig = ForkJoinPoolConfig.parse(FORK_JOIN_POOL_PLUGIN_NAME, PLUGIN_CONFIG);
        Assertions.assertNotNull(forkJoinPoolConfig);
        Assertions.assertEquals(2, forkJoinPoolConfig.getParallelism());
        Assertions.assertEquals(10000, forkJoinPoolConfig.getTimeoutMills());
        PLUGIN_CONFIG.put(ForkJoinPoolConfig.TIMEOUT_MS, 20);
        forkJoinPoolConfig = ForkJoinPoolConfig.parse(FORK_JOIN_POOL_PLUGIN_NAME, PLUGIN_CONFIG);
        Assertions.assertEquals(2, forkJoinPoolConfig.getParallelism());
        Assertions.assertEquals(20, forkJoinPoolConfig.getTimeoutMills());
    }

}
