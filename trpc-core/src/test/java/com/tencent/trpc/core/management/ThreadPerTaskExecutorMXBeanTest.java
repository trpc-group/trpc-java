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

import java.util.concurrent.ExecutorService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class ThreadPerTaskExecutorMXBeanTest {

    @Test
    public void testThreadPerTaskExecutorMXBean() {
        ExecutorService executorService = Mockito.mock(ExecutorService.class);
        ThreadPerTaskExecutorWrapper wrapper = ThreadPerTaskExecutorWrapper.wrap(executorService);
        ThreadPoolMXBean mxBean = new ThreadPerTaskExecutorMXBeanImpl(wrapper);
        Assertions.assertEquals(0, mxBean.getPoolSize());
        Assertions.assertEquals(0, mxBean.getActiveThreadCount());
        Assertions.assertEquals(0, mxBean.getTaskCount());
        Assertions.assertEquals(0, mxBean.getCompletedTaskCount());
        Assertions.assertEquals(0, mxBean.getCorePoolSize());
        Assertions.assertEquals(Integer.MAX_VALUE, mxBean.getMaximumPoolSize());
    }

}
