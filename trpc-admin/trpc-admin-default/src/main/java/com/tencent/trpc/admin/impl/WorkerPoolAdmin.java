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

package com.tencent.trpc.admin.impl;

import com.tencent.trpc.admin.dto.WorkerPoolInfoDto;
import com.tencent.trpc.core.admin.spi.Admin;
import com.tencent.trpc.core.worker.WorkerPoolManager;
import com.tencent.trpc.core.worker.spi.WorkerPool;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/cmds/workerpool")
public class WorkerPoolAdmin implements Admin {

    @Path("/info")
    @GET
    @Produces({MediaType.APPLICATION_JSON})
    public WorkerPoolInfoDto report() {
        Map<String, Object> workerReportMap = new HashMap<>();
        List<WorkerPool> allInitializedExtensions = WorkerPoolManager.getAllInitializedExtension();
        allInitializedExtensions.forEach(workerPool -> workerReportMap.put(workerPool.getName(), workerPool.report()));
        return new WorkerPoolInfoDto(workerReportMap);
    }

}
