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

package com.tencent.trpc.admin;

import com.tencent.trpc.admin.dto.CommonDto;
import com.tencent.trpc.admin.dto.WorkerPoolInfoDto;
import com.tencent.trpc.admin.impl.WorkerPoolAdmin;
import com.tencent.trpc.core.common.ConfigManager;
import com.tencent.trpc.core.worker.WorkerPoolManager;
import com.tencent.trpc.core.worker.spi.WorkerPool;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class WorkerPoolAdminTest {

    @Before
    public void setUp() {
        ConfigManager.stopTest();
        ConfigManager.startTest();
    }

    @After
    public void tearDown() {
        ConfigManager.stopTest();
    }

    @Test
    public void testGetWorkerPoolInfo() {
        WorkerPoolAdmin workerPoolAdmin = new WorkerPoolAdmin();
        WorkerPoolInfoDto workerPoolInfoDto = workerPoolAdmin.report();
        Assert.assertTrue(workerPoolInfoDto.toString().contains("WorkerPoolInfoDto{"));
        Assert.assertEquals(CommonDto.SUCCESS, workerPoolInfoDto.getErrorcode());
        Assert.assertTrue(StringUtils.isEmpty(workerPoolInfoDto.getMessage()));
        Assert.assertTrue(MapUtils.isEmpty(workerPoolInfoDto.getWorkerPoolInfo()));

        WorkerPool workerPool = WorkerPoolManager.get(WorkerPoolManager.DEF_CONSUMER_WORKER_POOL_NAME);
        Assert.assertNotNull(workerPool);
        workerPoolInfoDto = workerPoolAdmin.report();
        Assert.assertTrue(MapUtils.isNotEmpty(workerPoolInfoDto.getWorkerPoolInfo()));
    }

}
