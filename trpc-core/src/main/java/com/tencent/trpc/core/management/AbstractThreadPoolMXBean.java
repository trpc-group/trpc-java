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

import java.util.concurrent.atomic.AtomicInteger;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

public abstract class AbstractThreadPoolMXBean implements ThreadPoolMXBean {

    private static final AtomicInteger threadPoolIndex = new AtomicInteger(1);

    private final String objectName;

    public AbstractThreadPoolMXBean() {
        this.objectName = WorkerPoolType.THREAD.getName() + BAR + threadPoolIndex.getAndIncrement();
    }

    @Override
    public String getType() {
        return WorkerPoolType.THREAD.getName();
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
