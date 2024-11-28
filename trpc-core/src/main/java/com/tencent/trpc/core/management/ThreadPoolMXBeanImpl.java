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

package com.tencent.trpc.core.management;

import java.util.Objects;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

public class ThreadPoolMXBeanImpl extends BaseThreadPoolMXBean {


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