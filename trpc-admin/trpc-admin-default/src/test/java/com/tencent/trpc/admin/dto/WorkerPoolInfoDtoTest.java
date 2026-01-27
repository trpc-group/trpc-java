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

package com.tencent.trpc.admin.dto;

import com.tencent.trpc.core.management.ForkJoinPoolMXBeanImpl;
import java.util.HashMap;
import java.util.concurrent.ForkJoinPool;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * WorkerPoolInfoDtoTest
 */
public class WorkerPoolInfoDtoTest {

    private WorkerPoolInfoDto workerPoolInfoDto;

    @BeforeEach
    public void setUp() {
        this.workerPoolInfoDto = new WorkerPoolInfoDto(null);
    }

    @Test
    public void testGetWorkerPoolInfo() {
        Assertions.assertNull(workerPoolInfoDto.getWorkerPoolInfo());
    }

    @Test
    public void testSetWorkerPoolInfo() {
        workerPoolInfoDto.setWorkerPoolInfo(new HashMap<>());
        Assertions.assertNotNull(workerPoolInfoDto.getWorkerPoolInfo());
        ForkJoinPoolMXBeanImpl report = new ForkJoinPoolMXBeanImpl(new ForkJoinPool());
        workerPoolInfoDto.getWorkerPoolInfo().put("a", report);
        Assertions.assertEquals(workerPoolInfoDto.getWorkerPoolInfo().get("a"), report);
    }

    @Test
    public void testToString() {
        Assertions.assertEquals(workerPoolInfoDto.toString(),
                "WorkerPoolInfoDto{workerPoolInfo=null} CommonDto{errorcode='0', message=''}");
    }
}
