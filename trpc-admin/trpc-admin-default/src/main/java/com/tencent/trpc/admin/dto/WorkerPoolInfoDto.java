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

package com.tencent.trpc.admin.dto;

import java.util.Map;

/**
 * Thread pool information view class
 */
public class WorkerPoolInfoDto extends CommonDto {

    /**
     * Thread pool information mapping
     */
    private Map<String, Object> workerPoolInfo;

    public WorkerPoolInfoDto(Map<String, Object> workerPoolInfos) {
        this.workerPoolInfo = workerPoolInfos;
    }

    public Map<String, Object> getWorkerPoolInfo() {
        return workerPoolInfo;
    }

    public void setWorkerPoolInfo(Map<String, Object> workerPoolInfo) {
        this.workerPoolInfo = workerPoolInfo;
    }

    @Override
    public String toString() {
        return "WorkerPoolInfoDto{" + "workerPoolInfo=" + workerPoolInfo + "} " + super.toString();
    }
}
