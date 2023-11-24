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
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicInteger;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

public class ForkJoinPoolMXBeanImpl implements ForkJoinPoolMXBean {

    private static final AtomicInteger forkJoinPoolImplIndex = new AtomicInteger(1);

    private final ForkJoinPool forkJoinPool;

    private final String objectName;

    public ForkJoinPoolMXBeanImpl(ForkJoinPool forkJoinPool) {
        this.forkJoinPool = Objects.requireNonNull(forkJoinPool, "forkJoinPool is null");
        objectName = WorkerPoolType.FORK_JOIN.getName() + BAR + forkJoinPoolImplIndex.getAndIncrement();
    }

    @Override
    public String getType() {
        return WorkerPoolType.FORK_JOIN.getName();
    }

    @Override
    public long getQueuedSubmissionCount() {
        return forkJoinPool.getQueuedSubmissionCount();
    }

    @Override
    public long getQueuedTaskCount() {
        return forkJoinPool.getQueuedTaskCount();
    }

    @Override
    public int getPoolSize() {
        return forkJoinPool.getPoolSize();
    }

    @Override
    public int getActiveThreadCount() {
        return forkJoinPool.getActiveThreadCount();
    }

    @Override
    public int getRunningThreadCount() {
        return forkJoinPool.getRunningThreadCount();
    }

    @Override
    public long getStealCount() {
        return forkJoinPool.getStealCount();
    }

    @Override
    public int getParallelism() {
        return forkJoinPool.getParallelism();
    }

    @Override
    public int getCommonPoolParallelism() {
        return ForkJoinPool.getCommonPoolParallelism();
    }

    @Override
    public ObjectName getObjectName() {
        try {
            return new ObjectName(WORKER_POOL_MXBEAN_DOMAIN_TYPE + ",name=" + objectName);
        } catch (MalformedObjectNameException e) {
            throw new IllegalArgumentException(e);
        }
    }

}