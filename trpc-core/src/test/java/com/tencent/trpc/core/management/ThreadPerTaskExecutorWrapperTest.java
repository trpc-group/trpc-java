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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.Assert;
import org.junit.Test;
import org.powermock.api.mockito.PowerMockito;

public class ThreadPerTaskExecutorWrapperTest {

    @Test
    public void testThreadPerTaskExecutorWrapper() throws ExecutionException, InterruptedException {
        ExecutorService executorService = Executors.newFixedThreadPool(1);
        ThreadPerTaskExecutorWrapper wrapper = ThreadPerTaskExecutorWrapper.wrap(executorService);
        Callable<String> callable = () -> "mock";
        Future<String> submit = wrapper.submit(callable);
        submit.get();
        Assert.assertEquals(1, wrapper.getSubmittedTaskCount());
        Assert.assertEquals(1, wrapper.getCompletedTaskCount());
        List<Callable<String>> callables = Arrays.asList(callable, callable, callable);
        List<Future<String>> futures = wrapper.invokeAll(callables);
        for (Future<String> future : futures) {
            future.get();
        }
        Assert.assertEquals(4, wrapper.getSubmittedTaskCount());
        Assert.assertEquals(4, wrapper.getCompletedTaskCount());
        Runnable runnable = () -> {
        };
        Future<?> future = wrapper.submit(runnable);
        future.get();
        Assert.assertEquals(5, wrapper.getSubmittedTaskCount());
        Assert.assertEquals(5, wrapper.getCompletedTaskCount());
    }

    @Test
    public void testThreadPerTaskExecutorWrapper2() throws ExecutionException, InterruptedException, TimeoutException {
        ExecutorService executorService = PowerMockito.mock(ExecutorService.class);
        ThreadPerTaskExecutorWrapper wrapper = ThreadPerTaskExecutorWrapper.wrap(executorService);
        Runnable runnable = () -> {
        };
        Callable<String> callable = () -> "mock";
        List<Callable<String>> callables = Collections.singletonList(callable);
        wrapper.execute(runnable);
        wrapper.submit(runnable);
        wrapper.submit(runnable, "mock");
        wrapper.submit(callable);
        wrapper.invokeAll(callables);
        wrapper.invokeAll(callables, 1, TimeUnit.SECONDS);
        wrapper.invokeAny(callables);
        wrapper.invokeAny(callables, 1, TimeUnit.SECONDS);
        wrapper.shutdown();
        wrapper.shutdownNow();
        wrapper.isShutdown();
        wrapper.isTerminated();
        wrapper.awaitTermination(1, TimeUnit.SECONDS);
        Assert.assertEquals(0, wrapper.getSubmittedTaskCount());
        Assert.assertEquals(0, wrapper.getCompletedTaskCount());
    }

}