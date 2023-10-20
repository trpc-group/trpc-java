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

package com.tencent.trpc.admin.dto.rpc;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tencent.trpc.core.worker.handler.TrpcThreadExceptionHandler;
import com.tencent.trpc.core.worker.spi.WorkerPool;
import java.math.BigDecimal;

/**
 * Statistics for RPC client
 */
public class RpcStatsClientDto extends RPCStatsCommonDto {

    /**
     * P99 latency for the client.
     * Latency percentile represents the percentage of requests that have a latency lower than a specific value.
     * For example, p90 represents the 90th percentile latency, indicating that only 10% of requests have a higher
     * latency than this value.
     * Note:
     * 1. The "xxx" mentioned above represents the service_name, used to differentiate different services.
     * 2. Latency-related fields are used to calculate percentile latency. The framework calculates p9999 and p999
     * latency by default.
     * latency_p1, latency_p2, and latency_p3 are user-configurable fields with default values of p80, p90, and p99
     * respectively. Not yet implemented.
     */
    @JsonProperty("latency_99")
    private BigDecimal latency99;

    /**
     * init configuration
     */
    public RpcStatsClientDto(WorkerPool workerPool) {
        this.workerPool = workerPool;

        this.connectionCount = getThreadPoolMXBean() == null ? getForkJoinPoolMXBean() == null ? 0
                : getForkJoinPoolMXBean().getPoolSize()
                : getThreadPoolMXBean().getPoolSize();

        this.reqTotal = getThreadPoolMXBean() == null ? getForkJoinPoolMXBean() == null ? 0
                : getForkJoinPoolMXBean().getQueuedSubmissionCount()
                : getThreadPoolMXBean().getCompletedTaskCount();

        this.reqActive = getThreadPoolMXBean() == null ? getForkJoinPoolMXBean() == null ? 0
                : getForkJoinPoolMXBean().getActiveThreadCount()
                : getThreadPoolMXBean().getActiveThreadCount();

        this.errorTotal = ((TrpcThreadExceptionHandler) getWorkerPool()
                .getUncaughtExceptionHandler()).getErrorCount();
    }

    public BigDecimal getLatency99() {
        return latency99;
    }

    public void setLatency99(BigDecimal latency99) {
        this.latency99 = latency99;
    }
}
