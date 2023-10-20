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

package com.tencent.trpc.core.rpc.def;

import com.tencent.trpc.core.common.NamedThreadFactory;
import com.tencent.trpc.core.common.timer.HashedWheelTimer;
import com.tencent.trpc.core.common.timer.Timeout;
import com.tencent.trpc.core.common.timer.TimerTask;
import com.tencent.trpc.core.rpc.TimeoutManager;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class DefTimeoutManager implements TimeoutManager {

    final HashedWheelTimer timer;

    public DefTimeoutManager(int tickms) {
        NamedThreadFactory threadFactory = new NamedThreadFactory("Trpc-Timeout-Scheduler", true);
        timer = new HashedWheelTimer(threadFactory, tickms, TimeUnit.MILLISECONDS);
        timer.start();
    }

    @SuppressWarnings("rawtypes")
    @Override
    public Future<?> watch(final Runnable task, long timeout) {
        FutureAdapter<?> adapter = new FutureAdapter(task);
        adapter.wrap = timer.newTimeout(adapter, timeout, TimeUnit.MILLISECONDS);
        return adapter;
    }

    @Override
    public void close() {
        timer.stop();
    }

    static class FutureAdapter<T> implements Future<T>, TimerTask {

        final Runnable task;
        Timeout wrap;

        FutureAdapter(Runnable task) {
            this.task = task;
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            return wrap.cancel();
        }

        @Override
        public boolean isCancelled() {
            return wrap.isCancelled();
        }

        @Override
        public boolean isDone() {
            return wrap.isCancelled();
        }

        @Override
        public T get() {
            throw new UnsupportedOperationException();
        }

        @Override
        public T get(long timeout, TimeUnit unit) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void run(Timeout timeout) throws Exception {
            task.run();
        }
    }

}
