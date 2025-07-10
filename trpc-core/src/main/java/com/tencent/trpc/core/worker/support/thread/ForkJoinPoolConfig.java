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

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import org.apache.commons.collections4.MapUtils;

/**
 * Fork join pool configuration class.
 */
public class ForkJoinPoolConfig {

    /**
     * Parallel thread count.
     */
    public static final String PARALLELISM = "parallel";
    /**
     * Thread pool shutdown timeout.
     */
    public static final String TIMEOUT_MS = "timeoutMs";
    /**
     * Default parallel thread count.
     */
    private static final int DEFAULT_PARALLELISM = Runtime.getRuntime().availableProcessors();
    /**
     * Default thread pool shutdown timeout.
     */
    private static final int DEFAULT_TIMEOUT = (int) TimeUnit.SECONDS.toMillis(10);
    /**
     * Parallel count.
     */
    private int parallelism;
    /**
     * Shutdown timeout.
     */
    private int timeoutMills;

    public static ForkJoinPoolConfig parse(String id, Map<String, Object> extMap) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(extMap, "extMap");
        ForkJoinPoolConfig config = new ForkJoinPoolConfig();
        config.setParallelism(MapUtils.getIntValue(extMap, PARALLELISM, DEFAULT_PARALLELISM));
        config.setTimeoutMills(MapUtils.getIntValue(extMap, TIMEOUT_MS, DEFAULT_TIMEOUT));
        return config;
    }

    public int getParallelism() {
        return parallelism;
    }

    public void setParallelism(int parallelism) {
        this.parallelism = parallelism;
    }

    public int getTimeoutMills() {
        return timeoutMills;
    }

    public void setTimeoutMills(int timeoutMills) {
        this.timeoutMills = timeoutMills;
    }

}
