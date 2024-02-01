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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.tencent.trpc.core.management.ForkJoinPoolMXBean;
import com.tencent.trpc.core.management.PoolMXBean;
import com.tencent.trpc.core.management.ThreadPoolMXBean;
import com.tencent.trpc.core.worker.handler.TrpcThreadExceptionHandler;
import com.tencent.trpc.core.worker.spi.WorkerPool;
import java.math.BigDecimal;
import java.util.Map;

public class RPCStatsCommonDto {

    /**
     * RPC framework-related field: number of connections
     */
    @JsonProperty("connection_count")
    protected Integer connectionCount;

    /**
     * Total number of requests, number of requests sent by the client, or number of requests received by the service
     */
    @JsonProperty("req_total")
    protected Long reqTotal;

    /**
     * Number of requests awaiting reply from the client, or number of requests being processed by the service
     */
    @JsonProperty("req_active")
    protected Integer reqActive;

    /**
     * Accumulated number of replies received by the client, or accumulated number of responses returned by the service
     */
    @JsonProperty("rsp_total")
    protected Long rspTotal;

    /**
     * Number of error results received by the client, or number of errors in the service
     */
    @JsonProperty("error_total")
    protected Long errorTotal;

    /**
     * Percentile latency for client or service, with customizable percentile values.
     * Latency percentile represents the percentage of requests that have a latency lower than a specific value.
     * For example, p90 represents the 90th percentile latency, indicating that only 10% of requests have a higher
     * latency than this value.
     * Note:
     * Latency-related fields are used to calculate percentile latency. The framework calculates p999
     * latency by default.
     * latency_p1, latency_p2, and latency_p3 are user-configurable fields with default values of p80, p90, and p99
     * respectively.
     */
    @JsonProperty("latency_p1")
    protected BigDecimal latencyP1;

    /**
     * Percentile latency for the client, with customizable percentile values.
     * Latency percentile represents the percentage of requests that have a latency lower than a specific value.
     * For example, p90 represents the 90th percentile latency, indicating that only 10% of requests have a higher
     * latency than this value.
     * Note:
     * Latency-related fields are used to calculate percentile latency. The framework calculates p999
     * latency by default.
     * latency_p1, latency_p2, and latency_p3 are user-configurable fields with default values of p80, p90, and p99
     * respectively.
     */
    @JsonProperty("latency_p2")
    protected BigDecimal latencyP2;

    /**
     * Percentile latency for the client, with customizable percentile values.
     * Latency percentile represents the percentage of requests that have a latency lower than a specific value.
     * For example, p90 represents the 90th percentile latency, indicating that only 10% of requests have a higher
     * latency than this value.
     * Note:
     * Latency-related fields are used to calculate percentile latency. The framework calculates p999
     * latency by default.
     * latency_p1, latency_p2, and latency_p3 are user-configurable fields with default values of p80, p90, and p99
     * respectively.
     */
    @JsonProperty("latency_p3")
    protected BigDecimal latencyP3;

    /**
     * Percentile latency for the client, with customizable percentile values.
     * Latency percentile represents the percentage of requests that have a latency lower than a specific value.
     * For example, p90 represents the 90th percentile latency, indicating that only 10% of requests have a higher
     * latency than this value.
     * Note:
     * Latency-related fields are used to calculate percentile latency. The framework calculates p999
     * latency by default.
     * latency_p1, latency_p2, and latency_p3 are user-configurable fields with default values of p80, p90, and p99
     * respectively.
     */
    @JsonProperty("latency_999")
    protected BigDecimal latency999;

    /**
     * Average, maximum, and minimum latency in milliseconds for the client or service.
     * avg: 1ms
     * max: 2ms
     * min: 1ms
     */
    @JsonProperty("latency_avg")
    protected Map<String, Double> latencyAvg;


    @JsonIgnore
    protected WorkerPool workerPool;

    /**
     * Init common configuration
     */
    public RPCStatsCommonDto(WorkerPool workerPool) {
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

    public Integer getConnectionCount() {
        return connectionCount;
    }

    public void setConnectionCount(Integer connectionCount) {
        this.connectionCount = connectionCount;
    }

    public Long getReqTotal() {
        return reqTotal;
    }

    public void setReqTotal(Long reqTotal) {
        this.reqTotal = reqTotal;
    }

    public Integer getReqActive() {
        return reqActive;
    }

    public void setReqActive(Integer reqActive) {
        this.reqActive = reqActive;
    }

    public Long getRspTotal() {
        return rspTotal;
    }

    public void setRspTotal(Long rspTotal) {
        this.rspTotal = rspTotal;
    }

    public Long getErrorTotal() {
        return errorTotal;
    }

    public void setErrorTotal(Long errorTotal) {
        this.errorTotal = errorTotal;
    }

    public BigDecimal getLatencyP1() {
        return latencyP1;
    }

    public void setLatencyP1(BigDecimal latencyP1) {
        this.latencyP1 = latencyP1;
    }

    public BigDecimal getLatencyP2() {
        return latencyP2;
    }

    public void setLatencyP2(BigDecimal latencyP2) {
        this.latencyP2 = latencyP2;
    }

    public BigDecimal getLatencyP3() {
        return latencyP3;
    }

    public void setLatencyP3(BigDecimal latencyP3) {
        this.latencyP3 = latencyP3;
    }

    public BigDecimal getLatency999() {
        return latency999;
    }

    public void setLatency999(BigDecimal latency999) {
        this.latency999 = latency999;
    }

    public Map<String, Double> getLatencyAvg() {
        return latencyAvg;
    }

    public void setLatencyAvg(Map<String, Double> latencyAvg) {
        this.latencyAvg = latencyAvg;
    }

    protected void setWorkerPool(WorkerPool workerPool) {
        this.workerPool = workerPool;
    }

    protected WorkerPool getWorkerPool() {
        return this.workerPool;
    }

    private PoolMXBean getPoolMXBean() {
        return getWorkerPool().report();
    }


    protected ThreadPoolMXBean getThreadPoolMXBean() {
        return getPoolMXBean() instanceof ThreadPoolMXBean ? (ThreadPoolMXBean) getPoolMXBean()
                : null;
    }

    protected ForkJoinPoolMXBean getForkJoinPoolMXBean() {
        return getPoolMXBean() instanceof ForkJoinPoolMXBean ? (ForkJoinPoolMXBean) getPoolMXBean()
                : null;
    }
}
