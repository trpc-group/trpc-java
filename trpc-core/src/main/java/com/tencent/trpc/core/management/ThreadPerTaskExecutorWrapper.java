package com.tencent.trpc.core.management;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

public class ThreadPerTaskExecutorWrapper implements ExecutorService {
    private final ExecutorService executorService;
    private final AtomicLong submittedTaskCount = new AtomicLong();
    private final AtomicLong completedTaskCount = new AtomicLong();

    private ThreadPerTaskExecutorWrapper(ExecutorService executorService) {
        this.executorService = executorService;
    }

    public static ThreadPerTaskExecutorWrapper wrap(ExecutorService executorService) {
        return new ThreadPerTaskExecutorWrapper(executorService);
    }

    public long getSubmittedTaskCount() {
        return submittedTaskCount.get();
    }

    public long getCompletedTaskCount() {
        return completedTaskCount.get();
    }

    private TaskCountingRunnable wrap(Runnable task) {
        return new TaskCountingRunnable(task);
    }

    private <T> TaskCountingCallable<T> wrap(Callable<T> task) {
        return new TaskCountingCallable<>(task);
    }

    private class TaskCountingRunnable implements Runnable {
        private final Runnable task;

        public TaskCountingRunnable(Runnable task) {
            this.task = task;
        }

        @Override
        public void run() {
            submittedTaskCount.incrementAndGet();
            try {
                task.run();
            } finally {
                completedTaskCount.incrementAndGet();
            }
        }
    }

    private class TaskCountingCallable<T> implements Callable<T> {
        private final Callable<T> task;

        public TaskCountingCallable(Callable<T> task) {
            this.task = task;
        }

        @Override
        public T call() throws Exception {
            submittedTaskCount.incrementAndGet();
            try {
                return task.call();
            } finally {
                completedTaskCount.incrementAndGet();
            }
        }
    }

    @Override
    public void shutdown() {
        executorService.shutdown();
    }

    @Override
    public List<Runnable> shutdownNow() {
        return executorService.shutdownNow();
    }

    @Override
    public boolean isShutdown() {
        return executorService.isShutdown();
    }

    @Override
    public boolean isTerminated() {
        return executorService.isTerminated();
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return executorService.awaitTermination(timeout, unit);
    }

    @Override
    public <T> Future<T> submit(Callable<T> task) {
        return executorService.submit(wrap(task));
    }

    @Override
    public <T> Future<T> submit(Runnable task, T result) {
        return executorService.submit(wrap(task), result);
    }

    @Override
    public Future<?> submit(Runnable task) {
        return executorService.submit(wrap(task));
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
        List<TaskCountingCallable<T>> wrappedTasks = tasks.stream()
                .map(this::wrap)
                .collect(Collectors.toList());
        return executorService.invokeAll(wrappedTasks);
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException {
        List<TaskCountingCallable<T>> wrappedTasks = tasks.stream()
                .map(this::wrap)
                .collect(Collectors.toList());
        return executorService.invokeAll(wrappedTasks, timeout, unit);
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
        List<TaskCountingCallable<T>> wrappedTasks = tasks.stream()
                .map(this::wrap)
                .collect(Collectors.toList());
        return executorService.invokeAny(wrappedTasks);
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        List<TaskCountingCallable<T>> wrappedTasks = tasks.stream()
                .map(this::wrap)
                .collect(Collectors.toList());
        return executorService.invokeAny(wrappedTasks, timeout, unit);
    }

    @Override
    public void execute(Runnable command) {
        executorService.execute(wrap(command));
    }
}
