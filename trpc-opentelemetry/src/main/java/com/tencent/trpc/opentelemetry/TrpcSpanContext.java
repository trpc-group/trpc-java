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

package com.tencent.trpc.opentelemetry;

import com.tencent.trpc.core.telemetry.SpanContext;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Context;

/**
 * TRPC span context implementation
 */
public class TrpcSpanContext implements SpanContext {

    private final Span span;
    private final Context context;

    private TrpcSpanContext(Context context, Span span) {
        this.context = context;
        this.span = span;
    }

    public static SpanContext create(Context context, Span span) {
        return new TrpcSpanContext(context, span);
    }

    public Context getContext() {
        return this.context;
    }

    @Override
    public String getTraceId() {
        return span.getSpanContext().getTraceId();
    }

    @Override
    public String getSpanId() {
        return span.getSpanContext().getSpanId();
    }

    @Override
    public boolean isSampled() {
        return span.getSpanContext().isSampled();
    }

}
