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

package com.tencent.trpc.opentelemetry;

import com.tencent.trpc.core.common.ConfigManager;
import com.tencent.trpc.core.common.config.PluginConfig;
import com.tencent.trpc.core.extension.ExtensionLoader;
import com.tencent.trpc.core.rpc.Invoker;
import com.tencent.trpc.core.rpc.Request;
import com.tencent.trpc.core.rpc.Response;
import com.tencent.trpc.core.rpc.RpcClientContext;
import com.tencent.trpc.core.rpc.RpcContext;
import com.tencent.trpc.core.rpc.RpcInvocation;
import com.tencent.trpc.core.rpc.RpcServerContext;
import com.tencent.trpc.core.rpc.def.DefRequest;
import com.tencent.trpc.core.rpc.def.DefResponse;
import com.tencent.trpc.core.utils.FutureUtils;
import com.tencent.trpc.opentelemetry.sdk.TrpcFilter;
import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class TrpcFilterTest {

    private static final String REQUEST_VALUE = "Hello, tRPC-Java";
    private static final String RESPONSE_VALUE = "Welcome to tRPC-Java";

    private TrpcFilter trpcFilter;
    private Request request;
    private Invoker<?> invoker;
    private Response response;

    @Before
    public void setUp() {
        ConfigManager.stopTest();
        ConfigManager.startTest();
        ConfigManager.getInstance().getServerConfig().setApp("trpc");
        ConfigManager.getInstance().getServerConfig().setServer("opentelemetry");
        ConfigManager.getInstance().getServerConfig().setLocalIp("0.0.0.0");
        PluginConfig pluginConfig = new PluginConfig(TrpcFilter.NAME, TrpcFilter.class);
        ExtensionLoader.registerPlugin(pluginConfig);
        this.trpcFilter = new TrpcFilter();
        this.request = new DefRequest();
        RpcInvocation rpcInvocation = new RpcInvocation();
        rpcInvocation.setArguments(new Object[]{REQUEST_VALUE});
        rpcInvocation.setFunc("/trpc.test.test.hello/sayHello");
        this.request.setInvocation(rpcInvocation);
        this.request.getMeta().setLocalAddress(new InetSocketAddress("127.0.0.1", 29999));
        this.response = new DefResponse();
        this.response.setValue(RESPONSE_VALUE);
        this.invoker = new Invoker<Object>() {
            @Override
            public Class<Object> getInterface() {
                return null;
            }

            @Override
            public CompletionStage<Response> invoke(Request request) {
                return FutureUtils.newSuccessFuture(response);
            }
        };
    }

    @After
    public void teardown() {
        ConfigManager.stopTest();
    }

    @Test
    public void testFilter() throws ExecutionException, InterruptedException {
        RpcContext rpcContext = new RpcClientContext();
        request.setContext(rpcContext);
        InetSocketAddress address = new InetSocketAddress(8080);
        request.getMeta().setRemoteAddress(address);
        request.getMeta().setLocalAddress(address);
        CompletableFuture<Response> clientFilter = (CompletableFuture<Response>) trpcFilter.filter(invoker, request);
        Assert.assertEquals(RESPONSE_VALUE, clientFilter.get().getValue());

        rpcContext = new RpcServerContext();
        request.setContext(rpcContext);
        trpcFilter.filter(invoker, request);
        CompletableFuture<Response> serverFilter = (CompletableFuture<Response>) trpcFilter.filter(invoker, request);
        Assert.assertEquals(RESPONSE_VALUE, serverFilter.get().getValue());
    }

}
