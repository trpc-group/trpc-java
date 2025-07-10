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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.tencent.trpc.core.worker.WorkerPoolManager;
import java.util.HashMap;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;

public class ThreadPoolConfigTest {

    @Test
    public void test() {
        ThreadPoolConfig config = new ThreadPoolConfig();
        config.setAllowCoreThreadTimeOut(false);
        config.setCloseTimeout(50);
        config.setCorePoolSize(40);
        config.setDaemon(true);
        config.setId("id");
        config.setKeepAliveTimeSeconds(30);
        config.setMaximumPoolSize(20);
        config.setNamePrefix("namePrefix");
        config.setUseFiber(Boolean.TRUE);
        config.setShareSchedule(Boolean.TRUE);
        config.setUseVirtualThreadPerTaskExecutor(Boolean.FALSE);
        assertFalse(config.isAllowCoreThreadTimeOut());
        assertEquals(50, config.getCloseTimeout());
        assertEquals(40, config.getCorePoolSize());
        assertTrue(config.isDaemon());
        assertEquals("id", config.getId());
        assertEquals(30, config.getKeepAliveTimeSeconds());
        assertEquals(20, config.getMaximumPoolSize());
        assertEquals("namePrefix", config.getNamePrefix());
        assertEquals(5000, config.getQueueSize());
        config.setQueueSize(10);
        assertEquals(10, config.getQueueSize());
        assertTrue(config.useFiber());
        assertTrue(config.isShareSchedule());
        assertTrue(config.toString().contains("ThreadPoolConfig{id="));
        ThreadPoolConfig.validate(WorkerPoolManager.DEF_CONSUMER_WORKER_POOL_CONFIG);
    }

    @Test
    public void testParse() {
        Map<String, Object> properties = new HashMap<>();
        properties.put(ThreadPoolConfig.CORE_POOL_SIZE, 2000);
        properties.put(ThreadPoolConfig.MAXIMUM_POOL_SIZE, 2000);
        properties.put(ThreadPoolConfig.KEEP_ALIVE_TIME_SECONDS, 300000);
        properties.put(ThreadPoolConfig.QUEUE_SIZE, 0);
        properties.put(ThreadPoolConfig.NAME_PREFIX, "test");
        properties.put(ThreadPoolConfig.DAEMON, Boolean.FALSE);
        properties.put(ThreadPoolConfig.CLOSE_TIMEOUT, 10 * 1000);
        properties.put(ThreadPoolConfig.ALLOW_CORE_THREAD_TIMEOUT, Boolean.TRUE);
        properties.put(ThreadPoolConfig.USE_VIRTUAL_THREAD_PER_TASK_EXECUTOR, Boolean.FALSE);
        properties.put(ThreadPoolConfig.USE_FIBER, Boolean.TRUE);
        properties.put(ThreadPoolConfig.SHARE_SCHEDULE, Boolean.TRUE);
        ThreadPoolConfig config = ThreadPoolConfig.parse("1", properties);
        assertNotNull(config);
        assertTrue(config.isAllowCoreThreadTimeOut());
        assertEquals(10000, config.getCloseTimeout());
        assertEquals(2000, config.getCorePoolSize());
        assertFalse(config.isDaemon());
        assertEquals("1", config.getId());
        assertEquals(300000, config.getKeepAliveTimeSeconds());
        assertEquals(2000, config.getMaximumPoolSize());
        assertEquals("test", config.getNamePrefix());
        assertEquals(0, config.getQueueSize());
        assertFalse(config.useVirtualThreadPerTaskExecutor());
        assertTrue(config.useFiber());
        assertTrue(config.isShareSchedule());
    }

    @Test
    public void testValidate() {
        ThreadPoolConfig config = new ThreadPoolConfig();
        config.setCorePoolSize(-1);
        Assert.assertThrows(IllegalArgumentException.class, config::validate);
        config.setCorePoolSize(0);
        config.setQueueSize(-1);
        Assert.assertThrows(IllegalArgumentException.class, config::validate);
        config.setQueueSize(0);
        config.setCloseTimeout(-1);
        Assert.assertThrows(IllegalArgumentException.class, config::validate);
        config.setCloseTimeout(0);
        config.validate();
    }
}
