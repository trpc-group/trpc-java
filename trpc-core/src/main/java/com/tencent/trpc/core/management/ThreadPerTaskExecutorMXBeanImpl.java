package com.tencent.trpc.core.management;

import com.tencent.trpc.core.logger.Logger;
import com.tencent.trpc.core.logger.LoggerFactory;

import java.lang.reflect.Method;
import java.util.concurrent.ExecutorService;

/**
 * ThreadPoolMXBean for ThreadPerTaskExecutor
 * <p>
 * JEP 444 recommends using JFR to monitor virtual threads.
 * This bean implements JMX by using reflection to get thread count from ThreadPerTaskExecutor.
 * To enable this feature, you need to open module java.base/java.util.concurrent to your module.
 * JVM option: --add-opens java.base/java.util.concurrent=ALL-UNNAMED
 */
public class ThreadPerTaskExecutorMXBeanImpl extends BaseThreadPoolMXBean {

    protected static final Logger logger = LoggerFactory.getLogger(ThreadPerTaskExecutorMXBeanImpl.class);
    private static final String THREAD_COUNT_METHOD_NAME = "threadCount";

    private final ExecutorService executorService;
    private Method threadCountMethod;

    public ThreadPerTaskExecutorMXBeanImpl(ExecutorService executorService) {
        this.executorService = executorService;
        try {
            threadCountMethod = executorService.getClass().getDeclaredMethod(THREAD_COUNT_METHOD_NAME);
            threadCountMethod.setAccessible(true);
        } catch (Exception e) {
            logger.info("cannot access ThreadPerTaskExecutor, please " +
                    "check module java.base/java.util.concurrent open, " +
                    "or ignore this warning, error: {}", e.getMessage());
        }
    }

    private int getThreadCount() {
        try {
            if (threadCountMethod == null) {
                return 0;
            }
            return (int) threadCountMethod.invoke(executorService);
        } catch (Exception ignored) {
            return 0;
        }
    }

    @Override
    public long getTaskCount() {
        return getThreadCount();
    }

    @Override
    public long getCompletedTaskCount() {
        return 0;
    }

    @Override
    public int getCorePoolSize() {
        return 0;
    }

    @Override
    public int getMaximumPoolSize() {
        return Integer.MAX_VALUE;
    }

    @Override
    public int getPoolSize() {
        return getThreadCount();
    }

    @Override
    public int getActiveThreadCount() {
        return getThreadCount();
    }
}
