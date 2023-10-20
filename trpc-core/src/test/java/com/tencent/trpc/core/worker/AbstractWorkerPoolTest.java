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

package com.tencent.trpc.core.worker;

import com.tencent.trpc.core.management.ForkJoinPoolMXBeanImpl;
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.concurrent.RejectedExecutionException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class AbstractWorkerPoolTest {

    @Rule
    public ExpectedException expectedEx = ExpectedException.none();

    @Test
    public void testToExecutor() {
        expectedEx.expect(UnsupportedOperationException.class);
        expectedEx.expectMessage("not support toThreadPoolExecutor");

        AbstractWorkerPool pool = new AbstractWorkerPool() {

            @Override
            public UncaughtExceptionHandler getUncaughtExceptionHandler() {
                return null;
            }

            @Override
            public String getName() {
                return null;
            }

            @Override
            public void execute(Task task) throws RejectedExecutionException {

            }

            @Override
            public ForkJoinPoolMXBeanImpl report() {
                return null;
            }

            @Override
            public void close(long timeoutMills) {

            }
        };

        pool.toExecutor();

    }
}
