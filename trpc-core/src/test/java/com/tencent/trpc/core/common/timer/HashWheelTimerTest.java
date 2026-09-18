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
            Assert.assertNotNull(hashedWheelTimer);
            hashedWheelTimer.start();
            // the tick duration has been normalized, so the timer still works
            Timeout timeout = hashedWheelTimer.newTimeout(t -> {
            }, 1000, TimeUnit.MILLISECONDS);
            Assert.assertFalse(timeout.isExpired());
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
            Assert.fail("IllegalArgumentException is expected");
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(e.getMessage(), e.getMessage().contains("tickDuration must be greater than 0"));
        }
    }

    /**
     * A tick duration which overflows the wheel should be rejected.
     */
    @Test
    public void testTickDurationOverflow() {
        try {
            new HashedWheelTimer(new NamedThreadFactory(), Long.MAX_VALUE / 2, TimeUnit.DAYS);
            Assert.fail("IllegalArgumentException is expected");
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(e.getMessage(), e.getMessage().contains("tickDuration"));
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