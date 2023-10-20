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

import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.metrics.InstrumentType;
import io.opentelemetry.sdk.metrics.data.AggregationTemporality;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.metrics.export.CollectionRegistration;
import io.opentelemetry.sdk.metrics.export.MetricReader;
import io.opentelemetry.sdk.metrics.internal.export.MetricProducer;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collection;

/**
 * Implementation of {@link MetricReader}
 */
public class OpenTelemetryMetricsReader implements MetricReader {

    private static final OpenTelemetryMetricsReader READER = new OpenTelemetryMetricsReader();
    private volatile MetricProducer metricProducer = MetricProducer.noop();

    /**
     * Get the reader instance
     *
     * @return instance
     */
    public static OpenTelemetryMetricsReader getReader() {
        return READER;
    }

    /**
     * Get prometheus metrics
     *
     * @param accept accept
     * @return metrics string value
     */
    public String getMetrics(String accept) {
        try {
            Collection<MetricData> metrics = metricProducer.collectAllMetrics();
            Serializer serializer = Serializer.create(accept, unused -> true);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            serializer.write(metrics, bos);
            return bos.toString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void register(CollectionRegistration collectionRegistration) {
        this.metricProducer = MetricProducer.asMetricProducer(collectionRegistration);
    }

    @Override
    public CompletableResultCode forceFlush() {
        return CompletableResultCode.ofSuccess();
    }

    @Override
    public CompletableResultCode shutdown() {
        return CompletableResultCode.ofSuccess();
    }

    @Override
    public AggregationTemporality getAggregationTemporality(InstrumentType instrumentType) {
        return AggregationTemporality.CUMULATIVE;
    }

}
