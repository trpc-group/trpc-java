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

import com.tencent.trpc.core.extension.ExtensionLoader;
import com.tencent.trpc.core.rpc.Invoker;
import com.tencent.trpc.core.rpc.Request;
import com.tencent.trpc.core.rpc.Response;
import com.tencent.trpc.core.rpc.RpcClientContext;
import com.tencent.trpc.core.rpc.RpcContext;
import com.tencent.trpc.core.rpc.RpcContextValueKeys;
import com.tencent.trpc.core.rpc.def.DefRequest;
import com.tencent.trpc.core.rpc.def.DefResponse;
import com.tencent.trpc.core.trace.spi.TracerFactory;
import com.tencent.trpc.core.utils.RpcContextUtils;
import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.Tracer.SpanBuilder;
import io.opentracing.noop.NoopSpan;
import io.opentracing.noop.NoopTracerFactory;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class TracerFilterTest {

    private TracerFilter filter = new TracerFilter() {
        @Override
        public String getPluginName() {
            return "test";
        }

        @Override
        public Span start(SpanBuilder spanBuilder, RpcContext context, Invoker<?> invoker, Request request) {
            if (spanBuilder == null) {
                return null;
            }
            return spanBuilder.start();
        }

        @Override
        public void finish(Span span, Request request, Response response, Throwable t) {
            if (span != null) {
                span.finish();
            }
        }

        @Override
        public SpanContext extract(Tracer tracer, Map<String, Object> attachments) {
            return null;
        }

        @Override
        public Map<String, Object> inject(Tracer tracer, Span span) {
            return new HashMap<>();
        }

        @Override
        public CompletionStage<Response> filter(Invoker<?> invoker, Request request) {
            return invoker.invoke(request);
        }
    };

    @Before
    public void before() {
        TracerFactoryManager.getManager().registPlugin("test", TestTraceFactory.class);
    }

    @After
    public void after() {
        ExtensionLoader.destroyAllPlugin();
    }

    @Test
    public void testGetTracer() throws Exception {
        RpcClientContext context = new RpcClientContext();
        RpcContextUtils.putValueMapValue(context, RpcContextValueKeys.CTX_TRACER, NoopTracerFactory.create());
        DefRequest request = new DefRequest();
        request.setContext(context);
        Tracer tracer = filter.getTracer(context, request.getMeta());
        Assert.assertNotNull(tracer);
    }

    @Test
    public void testCreateSpanBuilder() {
        Tracer tracer = NoopTracerFactory.create();
        DefRequest request = new DefRequest();
        Tracer.SpanBuilder spanBuilder = filter.createSpanBuilder(tracer, null, request.getMeta());
        Assert.assertNotNull(spanBuilder);
    }

    @Test
    public void testUpdateSpanErrorFlag() {
        DefResponse response = new DefResponse();
        Span span = NoopSpan.INSTANCE;
        filter.updateSpanErrorFlag(response, null, span);
        filter.updateSpanErrorFlag(response, new RuntimeException("test"), span);
    }

    @Test
    public void testDebugLog() {
        RpcClientContext context = new RpcClientContext();
        DefRequest request = new DefRequest();
        request.setContext(context);
        try {
            filter.getTracer(context, request.getMeta());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testGetTracerWithNullMeta() throws Exception {
        RpcClientContext context = new RpcClientContext();
        Tracer tracer = filter.getTracer(context, null);
        Assert.assertNull(tracer);
    }

    @Test
    public void testCreateSpanBuilderWithParent() {
        Tracer tracer = NoopTracerFactory.create();
        SpanContext parentContext = NoopSpan.INSTANCE.context();
        DefRequest request = new DefRequest();
        Tracer.SpanBuilder spanBuilder = filter.createSpanBuilder(tracer, parentContext, request.getMeta());
        Assert.assertNotNull(spanBuilder);
    }

    @Test
    public void testCreateSpanBuilderWithNullTracer() {
        DefRequest request = new DefRequest();
        Tracer.SpanBuilder spanBuilder = filter.createSpanBuilder(null, null, request.getMeta());
        Assert.assertNull(spanBuilder);
    }

    @Test
    public void testCreateSpanBuilderException() {
        Tracer tracer = NoopTracerFactory.create();
        Tracer.SpanBuilder spanBuilder = filter.createSpanBuilder(tracer, null, null);
        Assert.assertNull(spanBuilder);
    }

    @Test
    public void testUpdateSpanErrorFlagWithTRpcException() {
        DefResponse response = new DefResponse();
        response.setException(com.tencent.trpc.core.exception.TRpcException.newFrameException(100, "test"));
        Span span = NoopSpan.INSTANCE;
        filter.updateSpanErrorFlag(response, null, span);
    }

    @Test
    public void testUpdateSpanErrorFlagWithThrowable() {
        DefResponse response = new DefResponse();
        Span span = NoopSpan.INSTANCE;
        filter.updateSpanErrorFlag(response, new RuntimeException("test"), span);
    }

    @Test
    public void testUpdateSpanErrorFlagWithNullSpan() {
        DefResponse response = new DefResponse();
        filter.updateSpanErrorFlag(response, new RuntimeException("test"), null);
    }

    @Test
    public void testUpdateSpanErrorFlagException() {
        DefResponse response = new DefResponse();
        response.setException(new RuntimeException("test"));
        Span span = NoopSpan.INSTANCE;
        filter.updateSpanErrorFlag(response, null, span);
    }

    @Test
    public void testUpdateSpanErrorFlagWithBothExceptions() {
        DefResponse response = new DefResponse();
        response.setException(
                com.tencent.trpc.core.exception.TRpcException.newFrameException(200, "response exception"));
        Span span = NoopSpan.INSTANCE;
        filter.updateSpanErrorFlag(response, new RuntimeException("throwable"), span);
    }

    private static class TestTraceFactory implements TracerFactory {

        @Override
        public Tracer getTracer(String serverName, Integer port) {
            return NoopTracerFactory.create();
        }
    }
}
