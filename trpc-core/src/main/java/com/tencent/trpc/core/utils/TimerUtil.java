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

package com.tencent.trpc.core.utils;

/**
 * Timer utility.
 */
public class TimerUtil {

    /**
     * Start time
     */
    private long start = 0;
    /**
     * End time
     */
    private long end = 0;
    /**
     * Start time
     */
    private long nstart = 0;
    /**
     * End time
     */
    private long nend = 0;
    /**
     * Minimum elapsed time
     */
    private long min = Integer.MAX_VALUE;
    /**
     * Maximum elapsed time
     */
    private long max = Integer.MIN_VALUE;
    /**
     * Elapsed time count
     */
    private long times = 1;
    /**
     * Total elapsed time
     */
    private long total = 0;

    /**
     * Create an instance.
     */
    public static TimerUtil newInstance() {
        return new TimerUtil();
    }

    /**
     * Start the timer.
     */
    public void start() {
        start = System.currentTimeMillis();
    }

    /**
     * Start the timer.
     */
    public void nstart() {
        nstart = System.nanoTime();
    }

    /**
     * Stop the timer.
     */
    public void end() {
        end = System.currentTimeMillis();
        long answer = getCost();
        min = Math.min(answer, min);
        max = Math.max(answer, max);
        total += answer;
        times++;
    }

    public void nend() {
        nend = System.nanoTime();
        long answer = ngetCost();
        min = Math.min(answer, min);
        max = Math.max(answer, max);
        total += answer;
        times++;
    }

    /**
     * Get the minimum elapsed time.
     */
    public long getMinCost() {
        return min;
    }

    /**
     * Get the elapsed time.
     */
    public long getCost() {
        return (end - start);
    }

    /**
     * Get the elapsed time.
     */
    public long ngetCost() {
        return (nend - nstart);
    }

    /**
     * Get the elapsed time.
     */
    public long ugetCost() {
        return (nend - nstart) / 1000;
    }

    /**
     * Get the total elapsed time.
     */
    public long getTotalCost() {
        return total;
    }

    /**
     * Get the maximum elapsed time.
     */
    public long getMaxCost() {
        return max;
    }

    /**
     * Get the average elapsed time.
     */
    public long getAverage() {
        return total / times;
    }

    public void reset() {
        start = 0;
        end = 0;
        min = Integer.MAX_VALUE;
        max = Integer.MIN_VALUE;
        total = 0;
    }

}
