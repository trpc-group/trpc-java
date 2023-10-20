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

package com.tencent.trpc.opentelemetry.sdk.metrics;

import io.opentelemetry.sdk.metrics.data.AggregationTemporality;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.metrics.data.SumData;

public enum PrometheusType {
    GAUGE("gauge"),
    COUNTER("counter"),
    SUMMARY("summary"),
    HISTOGRAM("histogram");

    private final String typeString;

    PrometheusType(String typeString) {
        this.typeString = typeString;
    }

    static PrometheusType forMetric(MetricData metric) {
        switch (metric.getType()) {
            case LONG_GAUGE:
            case DOUBLE_GAUGE:
                return GAUGE;
            case LONG_SUM:
                return forSumDataType(metric.getLongSumData());
            case DOUBLE_SUM:
                return forSumDataType(metric.getDoubleSumData());
            case SUMMARY:
                return SUMMARY;
            case HISTOGRAM:
            case EXPONENTIAL_HISTOGRAM:
                return HISTOGRAM;
            default:
        }
        throw new IllegalArgumentException(
                "Unsupported metric type, this generally indicates version misalignment "
                        + "among opentelemetry dependencies. Please make sure to use opentelemetry-bom.");
    }

    private static PrometheusType forSumDataType(SumData<?> sumData) {
        if (sumData.isMonotonic()
                && sumData.getAggregationTemporality() == AggregationTemporality.CUMULATIVE) {
            return COUNTER;
        }
        return GAUGE;
    }

    String getTypeString() {
        return typeString;
    }
}
