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

package com.tencent.trpc.spring.cloud.gateway.client;

import com.tencent.trpc.core.rpc.CloseFuture;
import com.tencent.trpc.core.rpc.GenericClient;
import com.tencent.trpc.core.rpc.RpcClientContext;
import com.tencent.trpc.core.rpc.TRpcProxy;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.function.Predicate;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.cloud.gateway.filter.FilterDefinition;
import org.springframework.cloud.gateway.handler.predicate.PredicateDefinition;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.Route.Builder;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ServerWebExchange;

@RunWith(PowerMockRunner.class)
@PrepareForTest(TRpcProxy.class)
@PowerMockIgnore({"javax.management.*", "javax.security.*", "javax.ws.*"})
public class TrpcGatewayClientTest {

    private TrpcGatewayClient client;

    private Route route;

    private Builder builder;

    /**
     * 初始化build.
     *
     * @throws URISyntaxException
     */
    @Before
    public void setUp() throws URISyntaxException {
        PowerMockito.mockStatic(TRpcProxy.class);
        RouteDefinition definition = initData();
        builder = Route.builder(definition);
        Predicate<ServerWebExchange> predicate = serverWebExchange -> {
            serverWebExchange.checkNotModified("test");
            return true;
        };
        ReflectionTestUtils.setField(builder, "predicate", predicate);
        Predicate<ServerWebExchange> predicate2 = serverWebExchange -> {
            serverWebExchange.checkNotModified("test2");
            return true;
        };
        builder.and(predicate2);
        route = builder.build();
        Mockito.when(TRpcProxy.getProxy(Mockito.anyString())).thenReturn(new GenericClient() {

            @Override
            public CompletionStage<byte[]> asyncInvoke(RpcClientContext context, byte[] body) {
                return new CloseFuture<>();
            }

            @Override
            public byte[] invoke(RpcClientContext context, byte[] body) {
                return new byte[0];
            }
        });

        client = new TrpcGatewayClient(route);
    }

    private static @NotNull RouteDefinition initData() throws URISyntaxException {
        RouteDefinition definition = new RouteDefinition();
        definition.setId("1");
        List<FilterDefinition> list = new ArrayList<>();
        list.add(new FilterDefinition());
        definition.setFilters(list);
        URI uri = new URI("https://www.tencent.com/sayHello/nameParam");
        definition.setUri(uri);
        Map<String, Object> metaData = new HashMap<>();
        metaData.put("k1", new Object());
        definition.setMetadata(metaData);
        definition.setOrder(1024);
        List<PredicateDefinition> predicates = new ArrayList<>();
        PredicateDefinition p = new PredicateDefinition();
        p.setName("trpc-Name");
        p.setArgs(new HashMap<>());
        predicates.add(p);
        definition.setPredicates(predicates);
        return definition;
    }

    @Test
    public void testOpen() throws URISyntaxException {
        Object callInfo = ReflectionTestUtils.getField(client, "callInfo");
        Assert.assertNotNull(callInfo);
    }

    @Test(expected = NullPointerException.class)
    public void testParseCallInfoWithNull() {
        TrpcGatewayClient.parseCallInfo("");
    }

    @Test
    public void testParseCallInfoWithIllegalArgument() {
        try {
            TrpcGatewayClient.parseCallInfo("http://www.abc.com");
        } catch (Exception e) {
            Assert.assertTrue(
                    e.getMessage().contains("Create RpcInvocation fail: URI does not meet path specification"));
        }
    }
}
