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

    /**
     * A tick duration smaller than one millisecond is normalized to one millisecond and a warning is logged. The
     * warning uses the {@code {}} placeholder of slf4j, a printf style placeholder would leave the raw text in the
     * log and lose the real values.
     */
    @Test
    public void testTickDurationSmallerThanOneMillisecond() {
        HashedWheelTimer hashedWheelTimer = new HashedWheelTimer(new NamedThreadFactory(), 1L,
                TimeUnit.NANOSECONDS);
        try {
            Assertions.assertNotNull(hashedWheelTimer);
            hashedWheelTimer.start();
            // the tick duration has been normalized, so the timer still works
            Timeout timeout = hashedWheelTimer.newTimeout(t -> {
            }, 1000, TimeUnit.MILLISECONDS);
            Assertions.assertFalse(timeout.isExpired());
            timeout.cancel();
        } finally {
            hashedWheelTimer.stop();
        }
    }

    /**
     * An illegal tick duration should be rejected.
     */
    @Test
    public void testIllegalTickDuration() {
        try {
            new HashedWheelTimer(new NamedThreadFactory(), 0L, TimeUnit.MILLISECONDS);
            Assertions.fail("IllegalArgumentException is expected");
        } catch (IllegalArgumentException e) {
            Assertions.assertTrue(e.getMessage().contains("tickDuration must be greater than 0"), e.getMessage());
        }
    }

    /**
     * A tick duration which overflows the wheel should be rejected.
     */
    @Test
    public void testTickDurationOverflow() {
        try {
            new HashedWheelTimer(new NamedThreadFactory(), Long.MAX_VALUE / 2, TimeUnit.DAYS);
            Assertions.fail("IllegalArgumentException is expected");
        } catch (IllegalArgumentException e) {
            Assertions.assertTrue(e.getMessage().contains("tickDuration"), e.getMessage());
        }
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
