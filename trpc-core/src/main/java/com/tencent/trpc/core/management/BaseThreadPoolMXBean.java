package com.tencent.trpc.core.management;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class BaseThreadPoolMXBean implements ThreadPoolMXBean {

    private static final AtomicInteger threadPoolIndex = new AtomicInteger(1);

    private final String objectName;

    public BaseThreadPoolMXBean() {
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
