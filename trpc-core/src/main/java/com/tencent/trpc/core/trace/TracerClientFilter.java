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
import com.tencent.trpc.core.utils.RpcContextUtils;
import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.Tracer.SpanBuilder;
import io.opentracing.tag.Tags;
import java.util.Map.Entry;
import java.util.concurrent.CompletionStage;

public abstract class TracerClientFilter extends TracerFilter {

    private final Logger logger = LoggerFactory.getLogger(TracerClientFilter.class);

    @Override
    public CompletionStage<Response> filter(Invoker<?> invoker, Request request)
            throws TRpcException {
        RpcContext context = request.getContext();
        RequestMeta meta = request.getMeta();
        Tracer tracer = getTracer(context, meta);
        Span parentSpan = RpcContextUtils.getSpan(context);
        Span span = null;
        try {
            span = buildSpan(invoker, request, context, meta, tracer, parentSpan);
            if (logger.isDebugEnabled()) {
                logger.debug("before tjg TraceClientFilter reporting, span: {}", span);
            }
        } catch (Exception e) {
            logger.error("create trace client span error: ", e);
        }
        CompletionStage<Response> result = invoker.invoke(request);
        final Span tempSpan = span;
        result.whenComplete((r, t) -> {
            try {
                if (tempSpan != null) {
                    // change the current span error flag based on the call status
                    updateSpanErrorFlag(r, t, tempSpan);
                    // exception traceback, get the tracer information returned by the downstream
                    getDownstreamTracerInfo(tracer, tempSpan, r);
                    // exception traceback, determine the self-exception flag
                    updateUpstreamSpanFlag(parentSpan, tempSpan);
                    finish(tempSpan, request, r, t);
                }
                if (logger.isDebugEnabled()) {
                    logger.debug("after tjg TraceClientFilter reporting, span: {}", tempSpan);
                }
            } catch (Exception e) {
                logger.error("finish span error: ", e);
            }
        });
        return result;
    }

    private void updateUpstreamSpanFlag(Span parentSpan, Span tempSpan) {
        if (parentSpan != null
                && TracerConstants.Keys.TRACE_ERROR_VALUE.equals(tempSpan
                .getBaggageItem(TracerConstants.Keys.TRACE_ERROR_KEY))) {
            // change the upstream (parent span, in-service span) error flag
            parentSpan.setBaggageItem(TracerConstants.Keys.TRACE_ERROR_KEY,
                    TracerConstants.Keys.TRACE_ERROR_VALUE);
        }
    }

    private void getDownstreamTracerInfo(Tracer tracer, Span tempSpan, Response r) {
        if (r != null) {
            SpanContext spanContext = extract(tracer, r.getAttachments());
            if (spanContext != null) {
                for (Entry<String, String> entry : spanContext.baggageItems()) {
                    if (TracerConstants.Keys.TRACE_ERROR_KEY.equals(entry.getKey())
                            && TracerConstants.Keys.TRACE_ERROR_VALUE
                            .equals(entry.getValue())) {
                        // change the current main call error flag
                        tempSpan.setBaggageItem(TracerConstants.Keys.TRACE_ERROR_KEY,
                                entry.getValue());
                        break;
                    }
                }
            }
        }
    }

    private Span buildSpan(Invoker<?> invoker, Request request, RpcContext context,
            RequestMeta meta, Tracer tracer, Span parentSpan) {
        Span span = null;
        if (tracer != null) {
            // get the upstream spanContext
            SpanContext parentSpanContext = parentSpan == null ? null : parentSpan.context();
            // create a span based on the spanContext
            SpanBuilder builder = createSpanBuilder(tracer, parentSpanContext, meta);
            if (builder != null) {
                builder.withTag(Tags.SPAN_KIND.getKey(), Tags.SPAN_KIND_CLIENT);
                span = start(builder, context, invoker, request);
            }
            if (span != null) {
                // serialize the span and put it into the request
                request.getAttachments().putAll(inject(tracer, span));
            }
        }
        return span;
    }

}