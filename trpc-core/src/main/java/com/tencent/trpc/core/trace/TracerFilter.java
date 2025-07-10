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

import static com.tencent.trpc.core.trace.TracerConstants.CUSTOM_EXCEPTION_CODE;

import com.tencent.trpc.core.exception.TRpcException;
import com.tencent.trpc.core.filter.spi.Filter;
import com.tencent.trpc.core.logger.Logger;
import com.tencent.trpc.core.logger.LoggerFactory;
import com.tencent.trpc.core.rpc.Invoker;
import com.tencent.trpc.core.rpc.Request;
import com.tencent.trpc.core.rpc.RequestMeta;
import com.tencent.trpc.core.rpc.Response;
import com.tencent.trpc.core.rpc.RpcContext;
import com.tencent.trpc.core.rpc.RpcContextValueKeys;
import com.tencent.trpc.core.trace.spi.TracerFactory;
import com.tencent.trpc.core.utils.RpcContextUtils;
import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.Tracer.SpanBuilder;
import io.opentracing.tag.Tags;
import java.util.Map;
import java.util.Optional;

public abstract class TracerFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(TracerFilter.class);
    private TracerFactory traceFactory;
    private boolean traceInit = false;

    public abstract String getPluginName();

    /**
     * Start assembling a span.
     *
     * @param spanBuilder the spanBuilder
     * @param context the RpcContext
     * @param invoker the invoker
     * @param request the request
     * @return the span
     */
    public abstract Span start(SpanBuilder spanBuilder, RpcContext context, Invoker<?> invoker,
            Request request);

    /**
     * Finish the span and report.
     *
     * @param span the span
     * @param request the trpc request
     * @param response the trpc response
     * @param t the throwable
     */
    public abstract void finish(Span span, Request request, Response response, Throwable t);

    /**
     * Deserialize.
     *
     * @param tracer tracer
     * @param attachments attachments
     * @return SpanContext
     */
    public abstract SpanContext extract(Tracer tracer, Map<String, Object> attachments);

    /**
     * Serialize.
     *
     * @param tracer tracer
     * @param span span
     * @return the serialized object
     */
    public abstract Map<String, Object> inject(Tracer tracer, Span span);

    /**
     * Get the tracer currently processing the span.
     *
     * @param context RpcContext
     * @param meta RequestMeta
     * @return Tracer
     * @throws TRpcException trpc exception
     */
    public Tracer getTracer(RpcContext context, RequestMeta meta) throws TRpcException {
        try {
            logger.debug("c context:{},meta:{}", context, meta);
            Tracer tracer = RpcContextUtils.getTracer(context);
            if (tracer != null) {
                return tracer;
            }
            if (!traceInit) {
                if (TracerFactoryManager.getManager().hasExtension(getPluginName())) {
                    traceFactory = TracerFactoryManager.getManager().get(getPluginName());
                } else {
                    traceFactory = null;
                }
                traceInit = true;
            }
            if (traceFactory != null && meta != null) {
                Integer port =
                        meta.getLocalAddress() == null ? 0 : meta.getLocalAddress().getPort();
                tracer = traceFactory.getTracer(meta.getCallInfo().getCaller(), port);
                if (tracer != null) {
                    RpcContextUtils.putValueMapValue(context, RpcContextValueKeys.CTX_TRACER, tracer);
                } else {
                    logger.error("tracer is null,this self server name is null");
                }
            }
            logger.debug("getTracer tracer:{}}", tracer);
            return tracer;
        } catch (Exception e) {
            logger.error("getTracer error", e);
            return null;
        }
    }

    /**
     * Create a basic spanBuilder.
     *
     * @param tracer the tracer
     * @param parentSpanContext the parentSpanContext
     * @param meta the RequestMeta
     * @return the SpanBuilder
     */
    public SpanBuilder createSpanBuilder(Tracer tracer, SpanContext parentSpanContext,
            RequestMeta meta) {
        try {
            logger.debug("createSpanBuilder tracer:{},parentSpanContext:{},meta:{}", tracer,
                    parentSpanContext, meta);
            if (tracer == null) {
                return null;
            }
            Optional<SpanBuilder> spanBuilder = Optional.empty();
            // create a span based on the parentSpanContext
            if (parentSpanContext != null) {
                spanBuilder = Optional.ofNullable(
                        tracer.buildSpan(meta.getCallInfo().getCalleeMethod())
                                .asChildOf(parentSpanContext));
            } else {
                spanBuilder = Optional
                        .ofNullable(tracer.buildSpan(meta.getCallInfo().getCalleeMethod()));
            }
            return spanBuilder.get();
        } catch (Exception e) {
            logger.error("create SpanBuilder error", e);
            return null;
        }
    }

    /**
     * Handle exception traceback.
     *
     * @param response the Response
     * @param throwable the Throwable
     * @param span the Span
     */
    public void updateSpanErrorFlag(Response response, Throwable throwable, Span span) {
        try {
            logger.debug("updateSpanErrorFlag response:{},throwable:{},span:{}", response, throwable, span);
            if (span == null) {
                return;
            }
            int code = 0;
            // determine if there is an exception
            Throwable ex = (throwable != null ? throwable :
                    (response.getException() == null ? null : response.getException()));
            if (ex != null) {
                // handle TRpcException
                if (ex instanceof TRpcException) {
                    // system error code, getBizCode() for business error code
                    code = ((TRpcException) ex).getCode();
                    span.setTag(Tags.ERROR.getKey(), true);
                    // change the current span
                    span.setBaggageItem(TracerConstants.Keys.TRACE_ERROR_KEY,
                            TracerConstants.Keys.TRACE_ERROR_VALUE);
                } else {
                    span.setTag(Tags.ERROR.getKey(), true);
                    // change the current main call
                    span.setBaggageItem(TracerConstants.Keys.TRACE_ERROR_KEY,
                            TracerConstants.Keys.TRACE_ERROR_VALUE);
                    // set custom exception code
                    code = CUSTOM_EXCEPTION_CODE;
                }
            }
            span.setTag(TracerConstants.Keys.RESULT_CODE, code);
        } catch (Exception e) {
            logger.error("update span error flag error", e);
        }
    }

}
