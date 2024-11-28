package com.tencent.trpc.core.management;

import org.junit.Assert;
import org.junit.Test;
import org.powermock.api.mockito.PowerMockito;

import java.util.concurrent.ExecutorService;

public class ThreadPerTaskExecutorMXBeanTest {

    @Test
    public void testThreadPerTaskExecutorMXBean() {
        ExecutorService executorService = PowerMockito.mock(ExecutorService.class);
        ThreadPoolMXBean mxBean = new ThreadPerTaskExecutorMXBeanImpl(executorService);
        Assert.assertEquals(0, mxBean.getPoolSize());
        Assert.assertEquals(0, mxBean.getActiveThreadCount());
        Assert.assertEquals(0, mxBean.getTaskCount());
        Assert.assertEquals(0, mxBean.getCompletedTaskCount());
        Assert.assertEquals(0, mxBean.getCorePoolSize());
        Assert.assertEquals(Integer.MAX_VALUE, mxBean.getMaximumPoolSize());
    }

}