package com.tencent.trpc.core.management;

import org.junit.Assert;
import org.junit.Test;
import org.powermock.api.mockito.PowerMockito;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;

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
    }

}