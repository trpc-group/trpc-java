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
import com.tencent.trpc.core.common.config.BackendConfig;
import com.tencent.trpc.core.common.config.ConsumerConfig;
import com.tencent.trpc.core.common.config.GlobalConfig;
import com.tencent.trpc.core.common.config.ProtocolConfig;
import com.tencent.trpc.core.common.config.ServerConfig;
import com.tencent.trpc.core.filter.spi.Filter;
import com.tencent.trpc.core.rpc.ConsumerInvoker;
import com.tencent.trpc.core.rpc.Invoker;
import com.tencent.trpc.core.rpc.Request;
import com.tencent.trpc.core.rpc.Response;
import com.tencent.trpc.core.rpc.RpcClientContext;
import com.tencent.trpc.core.rpc.RpcInvocation;
import com.tencent.trpc.core.rpc.TrpcTransInfoKeys;
import com.tencent.trpc.core.rpc.def.DefRequest;
import com.tencent.trpc.core.rpc.def.DefResponse;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ConsumerFilterInvokerTest {

    @BeforeEach
    public void before() {
        ServerConfig serverConfig = new ServerConfig();
        serverConfig.setLocalIp("127.0.1.1");
        serverConfig.init();
        ConfigManager.getInstance().setServerConfig(serverConfig);
        GlobalConfig globalConfig = new GlobalConfig();
        globalConfig.setContainerName("container");
        globalConfig.setEnableSet(true);
        globalConfig.setFullSetName("set");
        ConfigManager.getInstance().setGlobalConfig(globalConfig);
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
        FilterManager.registerPlugin("consumerInvokerMock", ConsumerInovkrMockFilter.class);
        ConsumerConfig<Object> config = createConsumerConfig();
        config.getBackendConfig().setFilters(Lists.newArrayList("consumerInvokerMock"));
        ProtocolConfig protoConfig = createProtoConfig();
        ConsumerInvoker<Object> buildConsumerChain = FilterChain
                .buildConsumerChain(config, new ConsumerInvokerMock<>(config, protoConfig));
        Assertions.assertEquals(config, buildConsumerChain.getConfig());
        Assertions.assertEquals(protoConfig, buildConsumerChain.getProtocolConfig());
        Assertions.assertNull(buildConsumerChain.getInterface());
        CompletionStage<Response> invoke = buildConsumerChain.invoke(createRequest());
        Response r = invoke.toCompletableFuture().join();
        assertEquals(r.getValue(), 20);
        assertEquals("container", r.getRequest().getAttachment(TrpcTransInfoKeys.CALLER_CONTAINER_NAME));
        assertEquals("set", r.getRequest().getAttachment(TrpcTransInfoKeys.CALLER_SET_NAME));
    }

    /**
     * Test purpose: To verify if the result after executing the filter exception is correct.
     */
    @Test
    public void buildExceptionTest() {
        FilterManager
                .registerPlugin("consumerInvokerMockException", ConsumerInovkrExceptionFilter.class);
        ConsumerConfig<Object> config = createConsumerConfig();
        config.getBackendConfig().setFilters(Lists.newArrayList("consumerInvokerMockException"));
        ProtocolConfig protoConfig = createProtoConfig();
        ConsumerInvoker buildConsumerChain =
                FilterChain.buildConsumerChain(config, new ConsumerInvokerMock(config, protoConfig));
        CompletionStage invoke = buildConsumerChain.invoke(createRequest());
        Response r = (Response) (invoke.toCompletableFuture().join());
        assertEquals(r.getException().getMessage(), "exception");
    }

    /**
     * Test purpose: To verify if the filters are executed in the order configured.
     */
    @Test
    public void filterOrderedTest() {
        FilterManager.registerPlugin("ConsumerInovkr1Filter", ProviderInovkr1Filter.class);
        FilterManager.registerPlugin("ConsumerInovkr2Filter", ProviderInovkr2Filter.class);
        ConsumerConfig<Object> config = createConsumerConfig();
        config.getBackendConfig()
                .setFilters(Lists.newArrayList("ConsumerInovkr1Filter", "ConsumerInovkr2Filter"));
        ProtocolConfig protoConfig = createProtoConfig();
        ConsumerInvoker buildProviderChain =
                FilterChain.buildConsumerChain(config, new ConsumerInvokerMock(config, protoConfig));
        CompletionStage invoke = buildProviderChain.invoke(createRequest());
        Response r = (Response) (invoke.toCompletableFuture().join());
        assertEquals("102,1,", r.getValue());
    }

    public RpcClientContext createClientContext() {
        RpcClientContext context = new RpcClientContext();
        return context;
    }

    public ProtocolConfig createProtoConfig() {
        ProtocolConfig config = new ProtocolConfig();
        config.setIp("127.0.0.1");
        config.setPort(1111);
        return config;
    }

    public ConsumerConfig<Object> createConsumerConfig() {
        ConsumerConfig<Object> config = new ConsumerConfig<>();
        config.setBackendConfig(new BackendConfig());
        return config;
    }

    public Request createRequest() {
        Request request = new DefRequest();
        request.setInvocation(new RpcInvocation());
        request.getInvocation().setRpcServiceName("service");
        request.getInvocation().setRpcMethodName("method");
        request.setContext(createClientContext());
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

    public static class ConsumerInovkrMockFilter implements Filter {

        @Override
        public CompletionStage<Response> filter(Invoker<?> invoker, Request req) {
            return invoker.invoke(req).handle((r, t) -> {
                r.setValue(Integer.valueOf((Integer) r.getValue() + 10));
                r.setRequest(req);
                return r;
            });
        }
    }

    public static class ConsumerInovkrExceptionFilter<T> implements Filter {

        @Override
        public CompletionStage<Response> filter(Invoker<?> invoker, Request req) {
            return invoker.invoke(req).handle((r, t) -> {
                r.setException(new RuntimeException("exception"));
                return r;
            });
        }
    }

    public static class ConsumerInvokerMock<T> implements ConsumerInvoker<T> {

        private ConsumerConfig<T> config;
        private ProtocolConfig protoConfig;

        public ConsumerInvokerMock(ConsumerConfig<T> config, ProtocolConfig protoConfig) {
            super();
            this.config = config;
            this.protoConfig = protoConfig;
        }

        @Override
        public Class<T> getInterface() {
            return config.getServiceInterface();
        }

        @Override
        public CompletionStage<Response> invoke(Request request) {
            Response rsp = new DefResponse();
            rsp.setValue(10);
            return CompletableFuture.completedFuture(rsp);
        }

        @Override
        public ConsumerConfig<T> getConfig() {
            return config;
        }

        @Override
        public ProtocolConfig getProtocolConfig() {
            return protoConfig;
        }
    }
}
