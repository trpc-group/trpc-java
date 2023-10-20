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

package com.tencent.trpc.core.common;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class NamedThreadFactory implements ThreadFactory {

    protected static final AtomicInteger COUNTER = new AtomicInteger(1);
    protected final AtomicInteger threadNum = new AtomicInteger(1);
    protected final String prefix;
    protected final boolean daemon;
    protected final ThreadGroup group;
    private UncaughtExceptionHandler uncaughtExceptionHandler;

    public NamedThreadFactory() {
        this("pool-" + COUNTER.getAndIncrement(), false, null);
    }

    public NamedThreadFactory(String prefix) {
        this(prefix, false, null);
    }

    public NamedThreadFactory(String prefix, boolean daemon) {
        this(prefix, daemon, null);
    }

    public NamedThreadFactory(String prefix, boolean daemon, UncaughtExceptionHandler uncaughtExceptionHandler) {
        this.prefix = prefix + "-thread-";
        this.daemon = daemon;
        SecurityManager s = System.getSecurityManager();
        this.group = (s == null) ? Thread.currentThread().getThreadGroup() : s.getThreadGroup();
        this.uncaughtExceptionHandler = uncaughtExceptionHandler;
    }

    @Override
    public Thread newThread(Runnable runnable) {
        String name = prefix + threadNum.getAndIncrement();
        Thread ret = new Thread(group, runnable, name, 0);
        ret.setUncaughtExceptionHandler(uncaughtExceptionHandler);
        ret.setDaemon(daemon);
        return ret;
    }

    public ThreadGroup getThreadGroup() {
        return group;
    }

}
