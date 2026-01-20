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

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tencent.trpc.core.common.NamedThreadFactory;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class HashWheelTimerTest {

    HashedWheelTimer timer;

    @BeforeEach
    public void before() {
        NamedThreadFactory threadFactory = new NamedThreadFactory("Test-Scheduler", true);
        timer = new HashedWheelTimer(threadFactory, 10, TimeUnit.MILLISECONDS);
        timer.start();
    }

    @AfterEach
    public void after() {
        if (timer != null) {
            timer.stop();
        }
    }

    @Test
    public void testInit() {
        HashedWheelTimer hashedWheelTimer = new HashedWheelTimer();
        Assertions.assertNotNull(hashedWheelTimer);
        hashedWheelTimer.stop();
        hashedWheelTimer = new HashedWheelTimer(1L, TimeUnit.MINUTES);
        Assertions.assertNotNull(hashedWheelTimer);
        hashedWheelTimer.stop();
        hashedWheelTimer = new HashedWheelTimer(1L, TimeUnit.MINUTES, 1);
        Assertions.assertNotNull(hashedWheelTimer);
        hashedWheelTimer.stop();
        hashedWheelTimer = new HashedWheelTimer(new NamedThreadFactory());
        Assertions.assertNotNull(hashedWheelTimer);
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
