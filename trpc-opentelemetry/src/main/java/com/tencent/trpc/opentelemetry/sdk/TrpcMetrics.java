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
import io.opentelemetry.context.Context;
import io.opentelemetry.context.ContextKey;
import io.opentelemetry.instrumentation.api.instrumenter.OperationListener;
import java.util.concurrent.TimeUnit;

/**
 * TRPC Common metrics information reporting
 */
public class TrpcMetrics implements OperationListener {

    private static final double NANOS_PER_MS = TimeUnit.MILLISECONDS.toNanos(1);

    private final LongCounter requestCounts;
    private final LongUpDownCounter activeRequests;
    private final DoubleHistogram duration;

    private final ContextKey<State> metricsStateKey;

    private TrpcMetrics(ContextKey<State> metricsStateKey, LongCounter requestCounts, LongUpDownCounter activeRequests,
            DoubleHistogram duration) {
        this.metricsStateKey = metricsStateKey;
        this.requestCounts = requestCounts;
        this.activeRequests = activeRequests;
        this.duration = duration;
    }

    /**
     * Creating indicator information
     *
     * @param metricsStateKey Context KEY
     * @param requestCounts request count
     * @param activeRequests active count
     * @param duration duration
     * @return OperationListener object
     */
    public static OperationListener create(ContextKey<State> metricsStateKey, LongCounter requestCounts,
            LongUpDownCounter activeRequests, DoubleHistogram duration) {
        return new TrpcMetrics(metricsStateKey, requestCounts, activeRequests, duration);
    }

    @Override
    public Context onStart(Context context, Attributes startAttributes, long startNanos) {
        activeRequests.add(1, TemporaryMetricsView.applyActiveRequestsView(startAttributes));
        return context.with(metricsStateKey, new State(startAttributes, startNanos));
    }

    @Override
    public void onEnd(Context context, Attributes endAttributes, long endNanos) {
        State state = context.get(metricsStateKey);
        if (state == null) {
            return;
        }
        requestCounts.add(1, TemporaryMetricsView.applyActiveRequestsView(state.startAttributes()), context);
        activeRequests.add(-1, TemporaryMetricsView.applyActiveRequestsView(state.startAttributes()));
        duration.record((endNanos - state.startTimeNanos()) / NANOS_PER_MS,
                TemporaryMetricsView.applyServerDurationView(state.startAttributes(), endAttributes), context);
    }

}
