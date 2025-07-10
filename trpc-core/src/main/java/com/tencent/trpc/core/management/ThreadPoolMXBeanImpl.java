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

package com.tencent.trpc.core.management;

import java.util.Objects;
import java.util.concurrent.ThreadPoolExecutor;

public class ThreadPoolMXBeanImpl extends AbstractThreadPoolMXBean {


    private final ThreadPoolExecutor threadPool;

    public ThreadPoolMXBeanImpl(ThreadPoolExecutor threadPool) {
        this.threadPool = Objects.requireNonNull(threadPool, "threadPool is null");
    }

    @Override
    public int getPoolSize() {
        return threadPool.getPoolSize();
    }

    @Override
    public int getActiveThreadCount() {
        return threadPool.getActiveCount();
    }

    @Override
    public long getTaskCount() {
        return threadPool.getTaskCount();
    }

    @Override
    public long getCompletedTaskCount() {
        return threadPool.getCompletedTaskCount();
    }

    @Override
    public int getCorePoolSize() {
        return threadPool.getCorePoolSize();
    }

    @Override
    public int getMaximumPoolSize() {
        return threadPool.getMaximumPoolSize();
    }


}