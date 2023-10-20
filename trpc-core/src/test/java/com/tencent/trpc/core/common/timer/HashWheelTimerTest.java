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

package com.tencent.trpc.core.common.timer;

import static org.junit.Assert.assertTrue;

import com.tencent.trpc.core.common.NamedThreadFactory;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class HashWheelTimerTest {

    HashedWheelTimer timer;

    @Before
    public void before() {
        NamedThreadFactory threadFactory = new NamedThreadFactory("Test-Scheduler", true);
        timer = new HashedWheelTimer(threadFactory, 10, TimeUnit.MILLISECONDS);
        timer.start();
    }

    @After
    public void after() {
        if (timer != null) {
            timer.stop();
        }
    }

    @Test
    public void testInit() {
        HashedWheelTimer hashedWheelTimer = new HashedWheelTimer();
        Assert.assertNotNull(hashedWheelTimer);
        hashedWheelTimer.stop();
        hashedWheelTimer = new HashedWheelTimer(1L, TimeUnit.MINUTES);
        Assert.assertNotNull(hashedWheelTimer);
        hashedWheelTimer.stop();
        hashedWheelTimer = new HashedWheelTimer(1L, TimeUnit.MINUTES, 1);
        Assert.assertNotNull(hashedWheelTimer);
        hashedWheelTimer.stop();
        hashedWheelTimer = new HashedWheelTimer(new NamedThreadFactory());
        Assert.assertNotNull(hashedWheelTimer);
        hashedWheelTimer.stop();
    }

    @Test
    public void test() {
        timer.toString();
        Timeout newTimeout = timer.newTimeout(timeout -> {

        }, 1000, TimeUnit.MILLISECONDS);
        assertTrue(1 == timer.pendingTimeouts());
        assertTrue(!newTimeout.isExpired());
        newTimeout.toString();
        newTimeout.timer();
        newTimeout.task();
        newTimeout.cancel();
        assertTrue(newTimeout.isCancelled());
    }
}