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

package com.tencent.trpc.opentelemetry.sdk;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.DoubleHistogram;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.LongUpDownCounter;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.ContextKey;
import io.opentelemetry.instrumentation.api.instrumenter.OperationListener;
import io.opentelemetry.instrumentation.api.instrumenter.OperationMetrics;

/**
 * Client-side metrics reporting implementation
 */
public class TrpcClientMetrics implements OperationListener {

    private static final ContextKey<State> CLIENT_REQUEST_METRICS_STATE =
            ContextKey.named(Constants.CLIENT_REQUEST_METRICS_STATE_KEY);

    /**
     * Returns a {@link OperationMetrics} which can be used to enable recording of {@link
     * TrpcClientMetrics} on an {@link
     * io.opentelemetry.instrumentation.api.instrumenter.InstrumenterBuilder}.
     *
     * @return OperationMetrics instance
     */
    public static OperationMetrics get() {
        return TrpcClientMetrics::new;
    }

    private final OperationListener listener;

    private TrpcClientMetrics(Meter meter) {
        LongCounter requestCounts = meter.counterBuilder("trpc.client.counts")
                .setUnit("request")
                .setDescription("The count of tRPC requests")
                .build();
        LongUpDownCounter activeRequests = meter.upDownCounterBuilder("trpc.client.active_requests")
                .setUnit("requests")
                .setDescription("The number of concurrent tRPC requests that are currently in-flight")
                .build();
        DoubleHistogram duration = meter.histogramBuilder("trpc.client.duration")
                .setUnit("milliseconds")
                .setDescription("The duration of the inbound tRPC request")
                .build();
        this.listener = TrpcMetrics.create(CLIENT_REQUEST_METRICS_STATE, requestCounts, activeRequests, duration);
    }

    @Override
    public Context onStart(Context context, Attributes startAttributes, long startNanos) {
        return listener.onStart(context, startAttributes, startNanos);
    }

    @Override
    public void onEnd(Context context, Attributes endAttributes, long endNanos) {
        listener.onEnd(context, endAttributes, endNanos);
    }

}
