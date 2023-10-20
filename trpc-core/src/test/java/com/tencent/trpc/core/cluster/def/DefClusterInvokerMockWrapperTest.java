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

package com.tencent.trpc.core.cluster.def;

import com.tencent.trpc.core.cluster.ClusterInvoker;
import com.tencent.trpc.core.cluster.def.DefClusterInvocationHandlerTest.BlankRpcServiceName;
import com.tencent.trpc.core.common.config.BackendConfig;
import com.tencent.trpc.core.common.config.ConsumerConfig;
import com.tencent.trpc.core.rpc.Request;
import com.tencent.trpc.core.rpc.Response;
import com.tencent.trpc.core.rpc.RpcContext;
import com.tencent.trpc.core.rpc.RpcInvocation;
import com.tencent.trpc.core.rpc.common.RpcMethodInfo;
import com.tencent.trpc.core.rpc.def.DefRequest;
import com.tencent.trpc.core.rpc.def.DefResponse;
import com.tencent.trpc.core.utils.FutureUtils;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class DefClusterInvokerMockWrapperTest {

    private DefClusterInvokerMockWrapper<BlankRpcServiceName> invokerNotMock;

    private DefClusterInvokerMockWrapper<BlankRpcServiceName> invokerMock;

    /**
     * Init test ConsumerConfig & BackendConfig & invokerNotMock & invokerMock
     */
    @Before
    public void setUp() {
        ConsumerConfig<BlankRpcServiceName> consumerConfig = new ConsumerConfig<>();
        consumerConfig.setMock(false);
        consumerConfig.setServiceInterface(BlankRpcServiceName.class);
        BackendConfig backendConfig = new BackendConfig();
        backendConfig.setNamingUrl("ip://127.0.0.1:8080");
        backendConfig.setServiceInterface(BlankRpcServiceName.class);
        backendConfig.setDefault();
        consumerConfig.setBackendConfig(backendConfig);
        invokerNotMock = new DefClusterInvokerMockWrapper<>(new ClusterInvoker<BlankRpcServiceName>() {
            @Override
            public Class<BlankRpcServiceName> getInterface() {
                return BlankRpcServiceName.class;
            }

            @Override
            public ConsumerConfig<BlankRpcServiceName> getConfig() {
                return consumerConfig;
            }

            @Override
            public BackendConfig getBackendConfig() {
                return backendConfig;
            }

            @Override
            public CompletionStage<Response> invoke(Request request) {
                return FutureUtils.newSuccessFuture(new DefResponse());
            }
        });

        ConsumerConfig<BlankRpcServiceName> cc = new ConsumerConfig<>();
        cc.setMock(true);
        cc.setMockClass("com.tencent.trpc.core.cluster.def.DefClusterInvocationHandlerTest$BlankRpcServiceNameImpl");
        cc.setServiceInterface(BlankRpcServiceName.class);
        BackendConfig bc = new BackendConfig();
        bc.setNamingUrl("ip://127.0.0.1:8080");
        bc.setServiceInterface(BlankRpcServiceName.class);
        bc.setDefault();
        cc.setBackendConfig(bc);
        invokerMock = new DefClusterInvokerMockWrapper<>(new ClusterInvoker<BlankRpcServiceName>() {
            @Override
            public Class<BlankRpcServiceName> getInterface() {
                return BlankRpcServiceName.class;
            }

            @Override
            public ConsumerConfig<BlankRpcServiceName> getConfig() {
                return cc;
            }

            @Override
            public BackendConfig getBackendConfig() {
                return bc;
            }

            @Override
            public CompletionStage<Response> invoke(Request request) {
                return FutureUtils.newSuccessFuture(new DefResponse());
            }
        });
    }

    @Test
    public void testGetInterface() {
        Assert.assertEquals(BlankRpcServiceName.class, invokerNotMock.getInterface());
    }

    @Test
    public void testInvoke() throws NoSuchMethodException {
        RpcInvocation invocation = new RpcInvocation();
        invocation.setFunc("a");
        RpcMethodInfo rpcMethodInfo = new RpcMethodInfo(BlankRpcServiceName.class,
                BlankRpcServiceName.class.getDeclaredMethod("blank", RpcContext.class));
        invocation.setRpcMethodInfo(rpcMethodInfo);
        invocation.setArguments(new Object[]{});
        Request request = new DefRequest();
        request.setInvocation(invocation);
        CompletionStage<Response> invoke = invokerMock.invoke(request);
        Assert.assertEquals("blank", invoke.toCompletableFuture().join().getValue());
        invokerNotMock.invoke(request);
    }

    @Test
    public void testInvokeExceptionally() throws NoSuchMethodException {
        RpcInvocation invocation = new RpcInvocation();
        invocation.setFunc("a");
        RpcMethodInfo rpcMethodInfo = new RpcMethodInfo(BlankRpcServiceName.class,
                BlankRpcServiceName.class.getDeclaredMethod("throwing", RpcContext.class));
        invocation.setRpcMethodInfo(rpcMethodInfo);
        invocation.setArguments(new Object[]{});
        Request request = new DefRequest();
        request.setInvocation(invocation);
        try {
            invokerMock.invoke(request).toCompletableFuture().join().getValue();
            Assert.fail();
        } catch (CompletionException e) {
            Assert.assertTrue(IllegalStateException.class.isAssignableFrom(e.getCause().getClass()));
            Assert.assertEquals("boom", e.getCause().getMessage());
        }
    }

    @Test
    public void testGetConfig() {
        Assert.assertFalse(invokerNotMock.getConfig().getMock());
        Assert.assertTrue(invokerMock.getConfig().getMock());
    }

    @Test
    public void testGetBackendConfig() {
        Assert.assertNotNull(invokerMock.getBackendConfig());
        Assert.assertNotNull(invokerNotMock.getBackendConfig());
    }

}
