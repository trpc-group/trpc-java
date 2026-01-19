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

package com.tencent.trpc.core.logger;

import com.tencent.trpc.core.common.ConfigManager;
import com.tencent.trpc.core.extension.ExtensionLoader;
import com.tencent.trpc.core.filter.spi.Filter;
import com.tencent.trpc.core.logger.HelloRequestProtocol.HelloResponse;
import com.tencent.trpc.core.rpc.GenericClient;
import com.tencent.trpc.core.rpc.Invoker;
import com.tencent.trpc.core.rpc.Request;
import com.tencent.trpc.core.rpc.Response;
import com.tencent.trpc.core.rpc.RpcClientContext;
import com.tencent.trpc.core.rpc.RpcInvocation;
import com.tencent.trpc.core.rpc.common.RpcMethodInfo;
import com.tencent.trpc.core.rpc.def.DefRequest;
import com.tencent.trpc.core.rpc.def.DefResponse;
import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class RemoteLoggerFilterTest {

    @BeforeEach
    public void before() {
        ConfigManager.stopTest();
        ConfigManager.startTest();
    }

    @AfterEach
    public void after() {
        ConfigManager.stopTest();
    }


    @Test
    public void testRemoteLoggerFilter() throws NoSuchMethodException {

        ExtensionLoader<?> extensionLoader = ExtensionLoader.getExtensionLoader(Filter.class);
        TestRemoteLoggerFilter filter = (TestRemoteLoggerFilter) extensionLoader.getExtension("test");

        Assertions.assertNotNull(filter);
        Assertions.assertEquals("test", filter.getPluginName());
        RpcInvocation rpcInvocation = new RpcInvocation();
        rpcInvocation.setRpcServiceName("a");
        rpcInvocation.setRpcMethodName("b");
        rpcInvocation.setArguments(new Object[]{"a", "b"});
        rpcInvocation.setRpcMethodInfo(new RpcMethodInfo(GenericClient.class,
                GenericClient.class.getMethod("invoke", RpcClientContext.class, byte[].class)));

        DefRequest defRequest = new DefRequest();
        defRequest.setInvocation(rpcInvocation);
        defRequest.getMeta().setRemoteAddress(new InetSocketAddress("127.0.0.1", 8080));
        filter.filter(new TestInvoker(), defRequest);
    }

    @Test
    public void testFilterMessage() {
        RemoteLoggerFilter remoteLoggerFilter = new RemoteLoggerFilter() {
            @Override
            public String getPluginName() {
                return "test";
            }
        };

        Request request = new DefRequest();
        request.setInvocation(new RpcInvocation());
        request.getMeta().setRemoteAddress(new InetSocketAddress("127.0.0.1", 8080));
        remoteLoggerFilter.filter(new EmptyInvoker(), request);
    }

    private static class EmptyInvoker implements Invoker {

        @Override
        public Class getInterface() {
            return null;
        }

        @Override
        public CompletionStage<Response> invoke(Request request) {
            Response response = new DefResponse();

            response.setValue(HelloResponse.newBuilder().build());
            return CompletableFuture.completedFuture(response);
        }
    }


}
