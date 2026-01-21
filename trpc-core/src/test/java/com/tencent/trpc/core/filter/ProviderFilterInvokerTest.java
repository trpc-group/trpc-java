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

package com.tencent.trpc.core.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.common.collect.Lists;
import com.tencent.trpc.core.common.ConfigManager;
import com.tencent.trpc.core.common.config.ProtocolConfig;
import com.tencent.trpc.core.common.config.ProviderConfig;
import com.tencent.trpc.core.common.config.ServiceConfig;
import com.tencent.trpc.core.filter.spi.Filter;
import com.tencent.trpc.core.rpc.Invoker;
import com.tencent.trpc.core.rpc.ProviderInvoker;
import com.tencent.trpc.core.rpc.Request;
import com.tencent.trpc.core.rpc.Response;
import com.tencent.trpc.core.rpc.RpcInvocation;
import com.tencent.trpc.core.rpc.RpcServerContext;
import com.tencent.trpc.core.rpc.def.DefRequest;
import com.tencent.trpc.core.rpc.def.DefResponse;
import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ProviderFilterInvokerTest {

    @BeforeEach
    public void before() {
        ConfigManager.getInstance().stopTest();
    }

    @AfterEach
    public void after() {
        ConfigManager.getInstance().stopTest();
    }

    /**
     * Test purpose: To verify if the result after executing the filter is correct.
     */
    @Test
    public void buildNormalTest() {
        FilterManager.registerPlugin("ProviderInovkrMockFilter", ProviderInovkrMockFilter.class);
        ProviderConfig<Object> config = createProviderConfig();
        config.setFilters(Lists.newArrayList("ProviderInovkrMockFilter"));
        ProtocolConfig protoConfig = createProtoConfig();
        ProviderInvoker buildProviderChain = FilterChain.buildProviderChain(config,
                new ProviderInvokerMock(config, protoConfig));
        Assertions.assertEquals(config, buildProviderChain.getConfig());
        Assertions.assertEquals(protoConfig, buildProviderChain.getProtocolConfig());
        Assertions.assertNull(buildProviderChain.getImpl());
        Assertions.assertNull(buildProviderChain.getInterface());
        CompletionStage invoke = buildProviderChain.invoke(createRequest());
        Response r = (Response) (invoke.toCompletableFuture().join());
        assertEquals(r.getValue(), 20);
    }

    /**
     * Test purpose: To verify if the result after executing the filter exception is correct.
     */
    @Test
    public void buildExceptionTest() {
        FilterManager.registerPlugin("ProviderInovkrExceptionFilter",
                ProviderInovkrExceptionFilter.class);
        ProviderConfig<Object> config = createProviderConfig();
        config.setFilters(Lists.newArrayList("ProviderInovkrExceptionFilter"));
        ProtocolConfig protoConfig = createProtoConfig();
        ProviderInvoker buildProviderChain = FilterChain
                .buildProviderChain(config, new ProviderInvokerMock(config, protoConfig));
        CompletionStage invoke = buildProviderChain.invoke(createRequest());
        Response r = (Response) (invoke.toCompletableFuture().join());
        assertEquals(r.getException().getMessage(), "exception");
    }

    /**
     * Test purpose: To verify if the filters are executed in the order configured.
     */
    @Test
    public void filterOrderedTest() {
        FilterManager.registerPlugin("ProviderInovkr1Filter", ProviderInovkr1Filter.class);
        FilterManager.registerPlugin("ProviderInovkr2Filter", ProviderInovkr2Filter.class);
        ProviderConfig<Object> config = createProviderConfig();
        config.setFilters(Lists.newArrayList("ProviderInovkr1Filter", "ProviderInovkr2Filter"));
        ProtocolConfig protoConfig = createProtoConfig();
        ProviderInvoker buildProviderChain = FilterChain
                .buildProviderChain(config, new ProviderInvokerMock(config, protoConfig));
        CompletionStage invoke = buildProviderChain.invoke(createRequest());
        Response r = (Response) (invoke.toCompletableFuture().join());
        assertEquals("102,1,", r.getValue());
    }

    public RpcServerContext createServerContext() {
        RpcServerContext context = new RpcServerContext();
        return context;
    }

    public ProtocolConfig createProtoConfig() {
        ProtocolConfig config = new ProtocolConfig();
        config.setIp("127.0.0.1");
        config.setPort(1111);
        return config;
    }

    public ProviderConfig createProviderConfig() {
        ProviderConfig config = new ProviderConfig();
        ServiceConfig serviceConfig = new ServiceConfig();
        serviceConfig.setRequestTimeout(10);
        config.setServiceConfig(serviceConfig);
        config.setRequestTimeout(10);
        config.setDefault();
        return config;
    }

    public Request createRequest() {
        Request request = new DefRequest();
        request.setInvocation(new RpcInvocation());
        request.getInvocation().setRpcServiceName("service");
        request.getInvocation().setRpcMethodName("method");
        request.getMeta().setRemoteAddress(new InetSocketAddress("127.0.0.1", 8080));
        request.setContext(createServerContext());
        return request;
    }

    public static class ProviderInovkr1Filter implements Filter {

        @Override
        public CompletionStage<Response> filter(Invoker<?> invoker, Request req) {
            return invoker.invoke(req).handle((r, t) -> {
                r.setValue(r.getValue() + "1,");
                return r;
            });
        }
    }

    public static class ProviderInovkr2Filter implements Filter {

        @Override
        public CompletionStage<Response> filter(Invoker<?> invoker, Request req) {
            return invoker.invoke(req).handle((r, t) -> {
                r.setValue(r.getValue() + "2,");
                return r;
            });
        }
    }

    public static class ProviderInovkrMockFilter implements Filter {

        @Override
        public CompletionStage<Response> filter(Invoker<?> invoker, Request req) {
            return invoker.invoke(req).handle((r, t) -> {
                r.setValue((Integer) r.getValue() + 10);
                r.setRequest(req);
                return r;
            });
        }
    }

    public static class ProviderInovkrExceptionFilter<T> implements Filter {

        @Override
        public CompletionStage<Response> filter(Invoker<?> invoker, Request req) {
            return invoker.invoke(req).handle((r, t) -> {
                r.setException(new RuntimeException("exception"));
                return r;
            });
        }
    }

    private static class ProviderInvokerMock<T> implements ProviderInvoker {

        private ProviderConfig<T> config;
        private ProtocolConfig protoConfig;

        ProviderInvokerMock(ProviderConfig<T> config, ProtocolConfig protoConfig) {
            super();
            this.config = config;
            this.protoConfig = protoConfig;
        }

        @Override
        public Class getInterface() {
            return config.getServiceInterface();
        }

        @Override
        public CompletionStage<Response> invoke(Request request) {
            Response rsp = new DefResponse();
            rsp.setValue(10);
            assertEquals(10, request.getMeta().getTimeout());
            return CompletableFuture.completedFuture(rsp);
        }

        @Override
        public Object getImpl() {
            return config.getRef();
        }

        @Override
        public ProviderConfig getConfig() {
            return config;
        }

        @Override
        public ProtocolConfig getProtocolConfig() {
            return protoConfig;
        }
    }
}
