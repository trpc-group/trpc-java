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
import java.util.concurrent.TimeUnit;

/**
 * {@link OperationListener} which keeps track of tRPC server metrics
 *
 * <p>To use this class, you may need to add the {@code opentelemetry-api-metrics} artifact to your
 * dependencies.</p>
 */
public final class TrpcServerMetrics implements OperationListener {

    private static final double NANOS_PER_MS = TimeUnit.MILLISECONDS.toNanos(1);

    private static final ContextKey<State> SERVER_REQUEST_METRICS_STATE =
            ContextKey.named(Constants.SERVER_REQUEST_METRICS_STATE_KEY);

    /**
     * Returns a {@link OperationMetrics} which can be used to enable recording of {@link
     * TrpcServerMetrics} on an {@link
     * io.opentelemetry.instrumentation.api.instrumenter.InstrumenterBuilder}.
     */
    public static OperationMetrics get() {
        return TrpcServerMetrics::new;
    }

    private final OperationListener listener;

    private TrpcServerMetrics(Meter meter) {
        LongCounter requestCounts = meter.counterBuilder("trpc.server.counts")
                .setUnit("request")
                .setDescription("The count of tRPC requests")
                .build();
        LongUpDownCounter activeRequests = meter
                .upDownCounterBuilder("trpc.server.active_requests")
                .setUnit("requests")
                .setDescription("The number of concurrent tRPC requests that are currently in-flight")
                .build();
        DoubleHistogram duration = meter
                .histogramBuilder("trpc.server.duration")
                .setUnit("milliseconds")
                .setDescription("The duration of the inbound tRPC request")
                .build();
        this.listener = TrpcMetrics.create(SERVER_REQUEST_METRICS_STATE, requestCounts, activeRequests, duration);
    }

    @Override
    public Context onStart(Context context, Attributes attributes, long startNanos) {
        return listener.onStart(context, attributes, startNanos);
    }

    @Override
    public void onEnd(Context context, Attributes attributes, long endNanos) {
        listener.onEnd(context, attributes, endNanos);
    }

}
