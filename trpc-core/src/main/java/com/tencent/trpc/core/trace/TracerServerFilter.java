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

package com.tencent.trpc.core.trace;

import com.tencent.trpc.core.exception.TRpcException;
import com.tencent.trpc.core.logger.Logger;
import com.tencent.trpc.core.logger.LoggerFactory;
import com.tencent.trpc.core.rpc.Invoker;
import com.tencent.trpc.core.rpc.Request;
import com.tencent.trpc.core.rpc.RequestMeta;
import com.tencent.trpc.core.rpc.Response;
import com.tencent.trpc.core.rpc.RpcContext;
import com.tencent.trpc.core.rpc.RpcContextValueKeys;
import com.tencent.trpc.core.utils.RpcContextUtils;
import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.Tracer.SpanBuilder;
import io.opentracing.tag.Tags;
import java.util.concurrent.CompletionStage;

public abstract class TracerServerFilter extends TracerFilter {

    private static final Logger logger = LoggerFactory.getLogger(TracerServerFilter.class);

    @Override
    public CompletionStage<Response> filter(Invoker<?> invoker, Request request)
            throws TRpcException {
        RpcContext context = request.getContext();
        RequestMeta meta = request.getMeta();
        Tracer tracer = getTracer(context, meta);
        Span span = null;
        try {
            if (tracer != null) {
                // get the incoming spanContext
                SpanContext parentSpanContext = extract(tracer, request.getAttachments());
                // create a span based on the spanContext
                SpanBuilder spanBuilder = createSpanBuilder(tracer, parentSpanContext, meta);
                if (spanBuilder != null) {
                    spanBuilder.withTag(Tags.SPAN_KIND.getKey(), Tags.SPAN_KIND_SERVER);
                    span = start(spanBuilder, context, invoker, request);
                }
                if (span != null) {
                    // put the span into the context
                    RpcContextUtils.putValueMapValue(context, RpcContextValueKeys.CTX_TRACE_SPAN, span);
                }
            }
            logger.debug("before tjg TraceServerFilter reporting,span: {}", span);
        } catch (Exception e) {
            logger.error("create trace server span error", e);
        }

        CompletionStage<Response> result = invoker.invoke(request);
        final Span tempSpan = span;
        result.whenComplete((r, t) -> {
            try {
                if (tempSpan != null) {
                    // change the current span error flag based on the call status
                    updateSpanErrorFlag(r, t, tempSpan);
                    // exception traceback, check if the current span has an error flag
                    if (TracerConstants.Keys.TRACE_ERROR_VALUE.equals(tempSpan
                            .getBaggageItem(TracerConstants.Keys.TRACE_ERROR_KEY))) {
                        // exception traceback, return tracer information
                        r.getAttachments().putAll(inject(tracer, tempSpan));
                    }
                    finish(tempSpan, request, r, t);
                }
                logger.debug("after tjg TraceClientFilter reporting,span:{}", tempSpan);
            } catch (Exception e) {
                logger.error("finish span error", e);
            }
        });
        return result;
    }

}
