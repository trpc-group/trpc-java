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

package com.tencent.trpc.core.management;

import java.util.concurrent.ExecutorService;
import org.junit.Assert;
import org.junit.Test;
import org.powermock.api.mockito.PowerMockito;

public class ThreadPerTaskExecutorMXBeanTest {

    @Test
    public void testThreadPerTaskExecutorMXBean() {
        ExecutorService executorService = PowerMockito.mock(ExecutorService.class);
        ThreadPerTaskExecutorWrapper wrapper = ThreadPerTaskExecutorWrapper.wrap(executorService);
        ThreadPoolMXBean mxBean = new ThreadPerTaskExecutorMXBeanImpl(wrapper);
        Assert.assertEquals(0, mxBean.getPoolSize());
        Assert.assertEquals(0, mxBean.getActiveThreadCount());
        Assert.assertEquals(0, mxBean.getTaskCount());
        Assert.assertEquals(0, mxBean.getCompletedTaskCount());
        Assert.assertEquals(0, mxBean.getCorePoolSize());
        Assert.assertEquals(Integer.MAX_VALUE, mxBean.getMaximumPoolSize());
    }

}