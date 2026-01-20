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

package com.tencent.trpc.core.trace;

import com.tencent.trpc.core.exception.TRpcException;
import com.tencent.trpc.core.extension.ExtensionLoader;
import com.tencent.trpc.core.logger.Logger;
import com.tencent.trpc.core.logger.LoggerFactory;
import com.tencent.trpc.core.rpc.Invoker;
import com.tencent.trpc.core.rpc.Request;
import com.tencent.trpc.core.rpc.Response;
import com.tencent.trpc.core.rpc.RpcContext;
import com.tencent.trpc.core.rpc.RpcContextValueKeys;
import com.tencent.trpc.core.rpc.RpcInvocation;
import com.tencent.trpc.core.rpc.RpcServerContext;
import com.tencent.trpc.core.rpc.def.DefRequest;
import com.tencent.trpc.core.rpc.def.DefResponse;
import com.tencent.trpc.core.trace.spi.TracerFactory;
import com.tencent.trpc.core.utils.RpcContextUtils;
import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.Tracer.SpanBuilder;
import io.opentracing.noop.NoopSpan;
import io.opentracing.noop.NoopSpanContext;
import io.opentracing.noop.NoopTracerFactory;
import io.opentracing.propagation.Format;
import io.opentracing.propagation.TextMapAdapter;
import io.opentracing.tag.Tags;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.Mockito;

@ExtendWith(MockitoExtension.class)
public class TraceServerFilterTest {

    private TracerServerFilter filter = new TracerServerFilter() {

        private final Logger logger = LoggerFactory.getLogger(TracerServerFilter.class);

        @Override
        public String getPluginName() {
            return "tjg";
        }

        @Override
        public Span start(SpanBuilder spanBuilder,
                RpcContext context, Invoker<?> invoker, Request request) {
            if (spanBuilder == null || request == null || request.getMeta() == null) {
                return null;
            }
            spanBuilder.withTag(Tags.PEER_SERVICE.getKey(),
                    request.getMeta().getCallInfo().getCallee());
            return spanBuilder.start();
        }

        @Override
        public void finish(Span span, Request request, Response response, Throwable t) {
            if (span == null) {
                return;
            }
            span.finish();
        }

        @Override
        public SpanContext extract(Tracer tracer, Map<String, Object> attachments) {
            try {
                if (logger.isDebugEnabled()) {
                    logger.debug("extract tracer:{},attachments:{}", tracer, attachments);
                }
                if (tracer == null || attachments == null) {
                    return null;
                }
                Map<String, String> attachMap = new HashMap<>();
                Set<Entry<String, Object>> setEntries = attachments.entrySet();
                for (Entry<String, Object> entry : setEntries) {
                    if (entry.getValue() instanceof byte[]) {
                        attachMap.put(entry.getKey(), new String((byte[]) entry.getValue(),
                                TracerConstants.DEFAULT_CHARSET));
                    }
                }
                return new SpanContext() {

                    @Override
                    public String toTraceId() {
                        return "";
                    }

                    @Override
                    public String toSpanId() {
                        return "";
                    }

                    @Override
                    public Iterable<Map.Entry<String, String>> baggageItems() {
                        Map<String, String> map = new HashMap<String, String>();
                        map.put(TracerConstants.Keys.TRACE_ERROR_KEY,
                                TracerConstants.Keys.TRACE_ERROR_VALUE);
                        List<Entry<String, String>> list = new ArrayList<Map.Entry<String,
                                String>>();
                        list.add(map.entrySet().iterator().next());
                        return list;
                    }

                    @Override
                    public String toString() {
                        return NoopSpanContext.class.getSimpleName();
                    }
                };
            } catch (Exception e) {
                logger.error("extract spancontext error", e);
            }
            return null;
        }

        @Override
        public Map<String, Object> inject(Tracer tracer, Span span) {
            logger.debug("inject tracer:{},span:{}", tracer, span);
            Map<String, Object> attachments = new HashMap<>();
            try {
                if (tracer == null || span == null) {
                    return attachments;
                }
                Map<String, String> traceMap = new HashMap<>();
                tracer.inject(span.context(), Format.Builtin.TEXT_MAP,
                        new TextMapAdapter(traceMap));
                for (Entry<String, String> entry : traceMap.entrySet()) {
                    if (entry.getValue() != null) {
                        attachments.put(entry.getKey(), entry.getValue()
                                .getBytes(TracerConstants.DEFAULT_CHARSET));
                    }
                }
            } catch (Exception e) {
                logger.error("inject spancontext error", e);
            }
            return attachments;
        }
    };

    @BeforeEach
    public void before() {
        TracerFactoryManager.getManager().registPlugin("mtest", TestTraceFactory.class);
    }

    @AfterEach
    public void after() {
        ExtensionLoader.destroyAllPlugin();
    }

    @Test
    public void testNormalSpan() {
        RpcServerContext context = new RpcServerContext();
        RpcContextUtils.putValueMapValue(context, RpcContextValueKeys.CTX_TRACE_SPAN, NoopSpan.INSTANCE);
        RpcContextUtils.putValueMapValue(context, RpcContextValueKeys.CTX_TRACER, NoopTracerFactory.create());
        Request request = new DefRequest();
        request.setContext(context);
        RpcInvocation invocation = new RpcInvocation();
        invocation.setRpcServiceName("rpcServiceName");
        invocation.setRpcMethodName("rpcMethodName");
        request.getMeta().setRemoteAddress(InetSocketAddress.createUnresolved("10.0.0.1", 8888));
        System.out.println(request.getMeta().getRemoteAddress().getAddress());
        request.getMeta().setLocalAddress(InetSocketAddress.createUnresolved("10.0.0.1", 9999));
        request.setInvocation(invocation);
        CompletableFuture<Response> future = new CompletableFuture<Response>();
        Response rsp = new DefResponse();
        future.complete(rsp);
        Invoker<?> invoker = (Invoker<?>) Mockito.mock(Invoker.class);
        Mockito.when(invoker.invoke(request)).thenReturn(future);
        filter.filter(invoker, request);
    }

    @Test
    public void testExceptionSpan() {
        RpcServerContext context = new RpcServerContext();
        RpcContextUtils.putValueMapValue(context, RpcContextValueKeys.CTX_TRACE_SPAN, NoopSpan.INSTANCE);
        RpcContextUtils.putValueMapValue(context, RpcContextValueKeys.CTX_TRACER, NoopTracerFactory.create());
        Request request = new DefRequest();
        request.setContext(context);
        RpcInvocation invocation = new RpcInvocation();
        invocation.setRpcServiceName("rpcServiceName");
        invocation.setRpcMethodName("rpcMethodName");
        request.getMeta().setRemoteAddress(InetSocketAddress.createUnresolved("10.0.0.1", 8888));
        request.getMeta().setLocalAddress(InetSocketAddress.createUnresolved("10.0.0.1", 9999));
        request.setInvocation(invocation);
        CompletableFuture<Response> future = new CompletableFuture<Response>();
        Response rsp = new DefResponse();
        rsp.setException(new IllegalArgumentException(""));
        future.complete(rsp);
        Invoker<?> invoker = (Invoker<?>) Mockito.mock(Invoker.class);
        Mockito.when(invoker.invoke(request)).thenReturn(future);
        filter.filter(invoker, request);
    }

    @Test
    public void testTRpcExceptionSpan() {
        RpcServerContext context = new RpcServerContext();
        RpcContextUtils.putValueMapValue(context, RpcContextValueKeys.CTX_TRACE_SPAN, NoopSpan.INSTANCE);
        RpcContextUtils.putValueMapValue(context, RpcContextValueKeys.CTX_TRACER, NoopTracerFactory.create());
        Request request = new DefRequest();
        request.setContext(context);
        RpcInvocation invocation = new RpcInvocation();
        invocation.setRpcServiceName("rpcServiceName");
        invocation.setRpcMethodName("rpcMethodName");
        request.getMeta().setRemoteAddress(InetSocketAddress.createUnresolved("10.0.0.1", 8888));
        request.getMeta().setLocalAddress(InetSocketAddress.createUnresolved("10.0.0.1", 9999));
        request.setInvocation(invocation);
        CompletableFuture<Response> future = new CompletableFuture<Response>();
        Response rsp = new DefResponse();
        rsp.setException(TRpcException.newBizException(10, ""));
        future.complete(rsp);
        Invoker<?> invoker = (Invoker<?>) Mockito.mock(Invoker.class);
        Mockito.when(invoker.invoke(request)).thenReturn(future);
        filter.filter(invoker, request);
    }

    private static class TestTraceFactory implements TracerFactory {

        @Override
        public Tracer getTracer(String serverName, Integer port) {
            return NoopTracerFactory.create();
        }
    }
}
