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

import com.tencent.trpc.core.logger.Logger;
import com.tencent.trpc.core.logger.LoggerFactory;

/**
 * Implementation of ThreadPoolMXBean for ThreadPerTaskExecutor using ThreadPerTaskExecutorWrapper.
 * JEP 444 recommends using JFR to monitor virtual threads.
 */
public class ThreadPerTaskExecutorMXBeanImpl extends AbstractThreadPoolMXBean {

    protected static final Logger logger = LoggerFactory.getLogger(ThreadPerTaskExecutorMXBeanImpl.class);

    private final ThreadPerTaskExecutorWrapper wrapper;

    public ThreadPerTaskExecutorMXBeanImpl(ThreadPerTaskExecutorWrapper wrapper) {
        this.wrapper = wrapper;
    }

    private long totalTaskCount() {
        return wrapper.getSubmittedTaskCount();
    }

    private long completedTaskCount() {
        return wrapper.getCompletedTaskCount();
    }

    private long executingTaskCount() {
        return totalTaskCount() - completedTaskCount();
    }

    @Override
    public long getTaskCount() {
        return executingTaskCount();
    }

    @Override
    public long getCompletedTaskCount() {
        return completedTaskCount();
    }

    @Override
    public int getCorePoolSize() {
        return 0;
    }

    @Override
    public int getMaximumPoolSize() {
        return Integer.MAX_VALUE;
    }

    @Override
    public int getPoolSize() {
        return (int) executingTaskCount();
    }

    @Override
    public int getActiveThreadCount() {
        return (int) executingTaskCount();
    }

}
