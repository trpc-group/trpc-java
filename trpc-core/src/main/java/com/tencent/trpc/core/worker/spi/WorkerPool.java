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

package com.tencent.trpc.core.worker.spi;

import com.tencent.trpc.core.extension.Extensible;
import com.tencent.trpc.core.management.PoolMXBean;
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;

/**
 * Abstract of task worker pool.
 */
@Extensible
public interface WorkerPool {

    String getName();

    void execute(Task task) throws RejectedExecutionException;

    PoolMXBean report();

    /**
     * Only supports thread scenarios.
     */
    Executor toExecutor();

    void close(long timeoutMills);

    interface Task {

        void run();
    }

    UncaughtExceptionHandler getUncaughtExceptionHandler();

}
