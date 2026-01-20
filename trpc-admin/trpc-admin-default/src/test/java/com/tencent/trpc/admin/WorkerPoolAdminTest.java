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

package com.tencent.trpc.admin;

import com.tencent.trpc.admin.dto.CommonDto;
import com.tencent.trpc.admin.dto.WorkerPoolInfoDto;
import com.tencent.trpc.admin.impl.WorkerPoolAdmin;
import com.tencent.trpc.core.common.ConfigManager;
import com.tencent.trpc.core.worker.WorkerPoolManager;
import com.tencent.trpc.core.worker.spi.WorkerPool;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class WorkerPoolAdminTest {

    @BeforeEach
    public void setUp() {
        ConfigManager.stopTest();
        ConfigManager.startTest();
    }

    @AfterEach
    public void tearDown() {
        ConfigManager.stopTest();
    }

    @Test
    public void testGetWorkerPoolInfo() {
        WorkerPoolAdmin workerPoolAdmin = new WorkerPoolAdmin();
        WorkerPoolInfoDto workerPoolInfoDto = workerPoolAdmin.report();
        Assertions.assertTrue(workerPoolInfoDto.toString().contains("WorkerPoolInfoDto{"));
        Assertions.assertEquals(CommonDto.SUCCESS, workerPoolInfoDto.getErrorcode());
        Assertions.assertTrue(StringUtils.isEmpty(workerPoolInfoDto.getMessage()));
        Assertions.assertTrue(MapUtils.isEmpty(workerPoolInfoDto.getWorkerPoolInfo()));

        WorkerPool workerPool = WorkerPoolManager.get(WorkerPoolManager.DEF_CONSUMER_WORKER_POOL_NAME);
        Assertions.assertNotNull(workerPool);
        workerPoolInfoDto = workerPoolAdmin.report();
        Assertions.assertTrue(MapUtils.isNotEmpty(workerPoolInfoDto.getWorkerPoolInfo()));
    }

}
