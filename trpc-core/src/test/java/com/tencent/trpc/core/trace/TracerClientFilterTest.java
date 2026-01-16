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
import com.tencent.trpc.core.logger.Logger;
import com.tencent.trpc.core.logger.LoggerFactory;
import com.tencent.trpc.core.rpc.Invoker;
import com.tencent.trpc.core.rpc.Request;
import com.tencent.trpc.core.rpc.Response;
import com.tencent.trpc.core.rpc.RpcClientContext;
import com.tencent.trpc.core.rpc.RpcContext;
import com.tencent.trpc.core.rpc.RpcContextValueKeys;
import com.tencent.trpc.core.rpc.RpcInvocation;
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
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"javax.management.*"})
public class TracerClientFilterTest {

    private TracerClientFilter filter = new TracerClientFilter() {

        private final Logger logger = LoggerFactory.getLogger(TracerClientFilter.class);

        @Override
        public String getPluginName() {
            return "tjg";
        }

        @Override
        public Span start(SpanBuilder spanBuilder, RpcContext context, Invoker<?> invoker, Request request) {
            if (spanBuilder == null || request == null || request.getMeta() == null) {
                return null;
            }
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
            return null;
        }

        @Override
        public Map<String, Object> inject(Tracer tracer, Span span) {
            if (logger.isDebugEnabled()) {
                logger.debug("inject tracer:{},span:{}", tracer, span);
            }
            return new HashMap<>();
        }
    };

    @Before
    public void before() {
        TracerFactoryManager.getManager().registPlugin("mtest", TestTraceFactory.class);
    }

    @After
    public void after() {
        ExtensionLoader.destroyAllPlugin();
    }

    @Test
    public void testNormalSpan() {
        RpcClientContext context = new RpcClientContext();
        RpcContextUtils.putValueMapValue(context, RpcContextValueKeys.CTX_TRACE_SPAN, NoopSpan.INSTANCE);
        RpcContextUtils.putValueMapValue(context, RpcContextValueKeys.CTX_TRACER, NoopTracerFactory.create());
        Request request = new DefRequest();
        request.setContext(context);
        RpcInvocation invocation = new RpcInvocation();
        invocation.setRpcServiceName("rpcServiceName");
        invocation.setRpcMethodName("rpcMethodName");
        request.getMeta().setRemoteAddress(InetSocketAddress.createUnresolved("127.0.0.1", 8888));
        request.getMeta().setLocalAddress(InetSocketAddress.createUnresolved("127.0.0.1", 9999));
        request.setInvocation(invocation);
        CompletableFuture<Response> future = new CompletableFuture<Response>();
        Response rsp = new DefResponse();
        future.complete(rsp);
        Invoker<?> invoker = (Invoker<?>) PowerMockito.mock(Invoker.class);
        PowerMockito.when(invoker.invoke(request)).thenReturn(future);
        filter.filter(invoker, request);
    }

    @Test
    public void testDebugLog() {
        RpcClientContext context = new RpcClientContext();
        Request request = new DefRequest();
        request.setContext(context);
        RpcInvocation invocation = new RpcInvocation();
        invocation.setRpcServiceName("rpcServiceName");
        invocation.setRpcMethodName("rpcMethodName");
        request.getMeta().setRemoteAddress(InetSocketAddress.createUnresolved("127.0.0.1", 8888));
        request.getMeta().setLocalAddress(InetSocketAddress.createUnresolved("127.0.0.1", 9999));
        request.setInvocation(invocation);
        CompletableFuture<Response> future = new CompletableFuture<Response>();
        Response rsp = new DefResponse();
        future.complete(rsp);
        Invoker<?> invoker = (Invoker<?>) PowerMockito.mock(Invoker.class);
        PowerMockito.when(invoker.invoke(request)).thenReturn(future);
        filter.filter(invoker, request);
    }

    private static class TestTraceFactory implements TracerFactory {

        @Override
        public Tracer getTracer(String serverName, Integer port) {
            return NoopTracerFactory.create();
        }
    }
}
