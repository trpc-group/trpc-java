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
import com.tencent.trpc.core.worker.handler.TrpcThreadExceptionHandler;
import com.tencent.trpc.core.worker.spi.WorkerPool;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Statistics of RPC service server
 */
public class RpcStatsServiceDto extends RPCStatsCommonDto {

    @JsonIgnore
    private static final BigDecimal EIGHTY_PERCENT = new BigDecimal(0.8);

    @JsonIgnore
    private static final BigDecimal EVERY_DAY = BigDecimal.valueOf(86400 * 0.2);

    /**
     * Average size of request packets received by the service
     */
    @JsonProperty("req_avg_len")
    private Double reqAvgLen;
    /**
     * Average size of response packets sent by the service
     */
    @JsonProperty("rsp_avg_len")
    private Double rspAvgLen;
    /**
     * total errors
     */
    @JsonProperty("error_total")
    private Long errorTotal;
    /**
     * Number of errors returned by the service business code
     */
    @JsonProperty("business_error")
    private Long businessError;
    /**
     * Number of protocol errors in the service
     */
    @JsonProperty("protocol_error")
    private Long protocolError;

    /**
     * Service p9999 latency
     * Latency percentile: the percentage of latency. For example, p90 means that only 10% of requests have a latency
     * higher than this value.
     * Note:
     * 1、The "xxx" in the above refers to the service name, which is used to distinguish different services.
     * 2、Latency-related fields are used to calculate percentile latency. The framework will calculate p9999 and p999
     * latency. latency_p1, latency_p2, and latency_p3 are user-configurable, with default values of p80, p90, and p99,
     * respectively. Not yet implemented.
     */
    @JsonProperty("latency_9999")
    private BigDecimal latency9999;

    /**
     * Service qps
     */
    @JsonProperty("qps")
    private Long qps;

    /**
     * Init configuration
     */
    public RpcStatsServiceDto(WorkerPool workerPool) {

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

        this.businessError = ((TrpcThreadExceptionHandler) getWorkerPool()
                .getUncaughtExceptionHandler())
                .getBusinessError();

        this.protocolError = ((TrpcThreadExceptionHandler) getWorkerPool()
                .getUncaughtExceptionHandler())
                .getProtocolError();

        this.qps = (new BigDecimal(reqTotal).multiply(EIGHTY_PERCENT))
                .divide(EVERY_DAY, 0, RoundingMode.HALF_UP)
                .longValue();
    }

    public Double getReqAvgLen() {
        return reqAvgLen;
    }

    public void setReqAvgLen(Double reqAvgLen) {
        this.reqAvgLen = reqAvgLen;
    }

    public Double getRspAvgLen() {
        return rspAvgLen;
    }

    public void setRspAvgLen(Double rspAvgLen) {
        this.rspAvgLen = rspAvgLen;
    }

    public Long getErrorTotal() {
        return errorTotal;
    }

    public void setErrorTotal(Long errorTotal) {
        this.errorTotal = errorTotal;
    }

    public Long getBusinessError() {
        return businessError;
    }

    public void setBusinessError(Long businessError) {
        this.businessError = businessError;
    }

    public Long getProtocolError() {
        return protocolError;
    }

    public void setProtocolError(Long protocolError) {
        this.protocolError = protocolError;
    }

    public BigDecimal getLatency9999() {
        return latency9999;
    }

    public void setLatency9999(BigDecimal latency9999) {
        this.latency9999 = latency9999;
    }

    public Long getQps() {
        return qps;
    }

    public void setQps(Long qps) {
        this.qps = qps;
    }
}
