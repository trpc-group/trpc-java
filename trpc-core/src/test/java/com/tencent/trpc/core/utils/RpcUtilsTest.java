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

package com.tencent.trpc.core.utils;

import com.tencent.trpc.core.cluster.ClusterInvoker;
import com.tencent.trpc.core.common.RpcResult;
import com.tencent.trpc.core.common.config.BackendConfig;
import com.tencent.trpc.core.common.config.ConsumerConfig;
import com.tencent.trpc.core.exception.ErrorCode;
import com.tencent.trpc.core.exception.TRpcException;
import com.tencent.trpc.core.rpc.GenericClient;
import com.tencent.trpc.core.rpc.InvokeMode;
import com.tencent.trpc.core.rpc.Request;
import com.tencent.trpc.core.rpc.Response;
import com.tencent.trpc.core.rpc.RpcClientContext;
import com.tencent.trpc.core.rpc.RpcContextValueKeys;
import com.tencent.trpc.core.rpc.anno.TRpcMethod;
import com.tencent.trpc.core.rpc.common.CommonResultClient;
import com.tencent.trpc.core.rpc.common.RpcMethodInfo;
import com.tencent.trpc.core.rpc.def.DefRequest;
import com.tencent.trpc.core.rpc.def.DefResponse;
import com.tencent.trpc.core.rpc.def.LeftTimeout;
import com.tencent.trpc.core.rpc.def.LinkInvokeTimeout;
import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import org.junit.Assert;
import org.junit.Test;

public class RpcUtilsTest {

    public String reMethod(String name) {
        return "hello" + name;
    }

    public CompletableFuture<String> futureMethod() {
        return new CompletableFuture<>();
    }

    @TRpcMethod(name = "9527")
    public String method1(String name) {
        return "hello" + name;
    }

    public String method2(String name) {
        return "hello" + name;
    }

    @TRpcMethod(name = "", isDefault = true)
    public String defaultMethod() {
        return "default";
    }

    @TRpcMethod(name = "", isGeneric = true)
    public String genericMethod() {
        return "generic";
    }

    @Test
    public void testIsReturnFutureType() throws Exception {
        boolean flag = InvokeMode.isAsync(RpcUtils.parseInvokeMode(
                RpcUtilsTest.class.getMethod("reMethod", String.class)));
        Assert.assertFalse(flag);

        boolean flagFuture = InvokeMode.isAsync(RpcUtils.parseInvokeMode(RpcUtilsTest.class.getMethod("futureMethod")));
        Assert.assertTrue(flagFuture);

        String methodName1 = RpcUtils.parseRpcMethodName(RpcUtilsTest.class.getMethod("method1", String.class), "100");
        Assert.assertEquals("9527", methodName1);

        String methodName2 = RpcUtils.parseRpcMethodName(RpcUtilsTest.class.getMethod("method2", String.class), "100");
        Assert.assertEquals("100", methodName2);

        String serviceName = RpcUtils.parseRpcServiceName(RpcUtilsTest.class, "helloService");
        Assert.assertEquals("helloService", serviceName);
    }

    @Test
    public void testIsGenericClient() {
        Assert.assertFalse(RpcUtils.isGenericClient(Object.class));
        Assert.assertTrue(RpcUtils.isGenericClient(GenericClient.class));
        Assert.assertTrue(RpcUtils.isGenericClient(SimpleGenericClient.class));
    }

    @Test
    public void testIsGenericMethod() throws NoSuchMethodException {
        Method method = RpcUtilsTest.class.getMethod("method1", String.class);
        Assert.assertFalse(RpcUtils.isGenericMethod(method));

        method = RpcUtilsTest.class.getMethod("genericMethod");
        Assert.assertTrue(RpcUtils.isGenericMethod(method));
    }

    @Test
    public void testIsDefaultMethod() throws NoSuchMethodException {
        Method method = RpcUtilsTest.class.getMethod("method1", String.class);
        Assert.assertFalse(RpcUtils.isDefaultRpcMethod(method));

        method = RpcUtilsTest.class.getMethod("defaultMethod");
        Assert.assertTrue(RpcUtils.isDefaultRpcMethod(method));
    }

    @Test
    public void testNewResponse() {
        Request request = new DefRequest();
        Assert.assertNotNull(RpcUtils.newResponse(request, new Object(), null));
    }

    @Test
    public void testParseAsyncInvokeResult() throws ExecutionException, InterruptedException {
        CompletableFuture<Response> response = FutureUtils.newFuture();
        RpcClientContext clientContext = new RpcClientContext();
        clientContext.setOneWay(Boolean.FALSE);
        CompletableFuture<Object> future = RpcUtils.parseAsyncInvokeResult(response, clientContext, null);
        response.complete(new DefResponse());
        CompletableFuture.allOf(future).join();
        Assert.assertNull(future.get());

        future = RpcUtils.parseAsyncInvokeResult(response, clientContext, null);
        response.complete(null);
        CompletableFuture.allOf(future).join();
        Assert.assertNull(future.get());
    }

    @Test
    public void testParseAsyncInvokeCommonResult() throws ExecutionException, InterruptedException,
            NoSuchMethodException {
        CompletableFuture<Response> response = FutureUtils.newFuture();
        RpcClientContext clientContext = new RpcClientContext();
        clientContext.setOneWay(Boolean.FALSE);
        Method method = CommonResultClient.class.getDeclaredMethod("asyncHello");
        RpcMethodInfo methodInfo = new RpcMethodInfo(CommonResultClient.class, method);
        CompletableFuture<Object> future = RpcUtils.parseAsyncInvokeResult(response, clientContext, methodInfo);
        response.complete(new DefResponse());
        CompletableFuture.allOf(future).join();
        Object o = future.get();
        Assert.assertNotNull(o);
        Assert.assertTrue(o instanceof RpcResult);
        response = FutureUtils.newFuture();
        future = RpcUtils.parseAsyncInvokeResult(response, clientContext, methodInfo);
        response.complete(null);
        Assert.assertNotNull(future.get());
        Assert.assertTrue(o instanceof RpcResult);
    }

    @Test(expected = CompletionException.class)
    public void testParseAsyncInvokeResult1() {
        CompletableFuture<Response> response = FutureUtils.newFuture();
        RpcClientContext clientContext = new RpcClientContext();
        clientContext.setOneWay(Boolean.FALSE);
        CompletableFuture<Object> future = RpcUtils.parseAsyncInvokeResult(response, clientContext, null);
        response.completeExceptionally(TRpcException.newBizException(1, "aaa"));
        CompletableFuture.allOf(future).join();
    }

    @Test(expected = CompletionException.class)
    public void testParseAsyncInvokeResult2() {
        CompletableFuture<Response> response = FutureUtils.newFuture();
        RpcClientContext clientContext = new RpcClientContext();
        clientContext.setOneWay(Boolean.FALSE);
        RpcContextUtils.putValueMapValue(clientContext, RpcContextValueKeys.CTX_LINK_INVOKE_TIMEOUT,
                LinkInvokeTimeout.builder()
                        .serviceEnableLinkTimeout(true)
                        .build());
        CompletableFuture<Object> future = RpcUtils.parseAsyncInvokeResult(response, clientContext, null);
        response.completeExceptionally(TRpcException.newFrameException(ErrorCode.TRPC_CLIENT_INVOKE_TIMEOUT_ERR,
                "aaa"));
        CompletableFuture.allOf(future).join();
    }

    @Test(expected = CompletionException.class)
    public void testParseAsyncInvokeResult3() {
        CompletableFuture<Response> response = FutureUtils.newFuture();
        RpcClientContext clientContext = new RpcClientContext();
        clientContext.setOneWay(Boolean.FALSE);
        RpcContextUtils.putValueMapValue(clientContext, RpcContextValueKeys.CTX_LINK_INVOKE_TIMEOUT,
                LinkInvokeTimeout.builder()
                        .serviceEnableLinkTimeout(true)
                        .build());
        CompletableFuture<Object> future = RpcUtils.parseAsyncInvokeResult(response, clientContext, null);
        response.completeExceptionally(TRpcException.newBizException(ErrorCode.TRPC_CLIENT_INVOKE_TIMEOUT_ERR,
                "aaa"));
        CompletableFuture.allOf(future).join();
    }

    @Test
    public void testParseAsyncInvoke4() throws NoSuchMethodException, ExecutionException,
            InterruptedException {
        CompletableFuture<Response> response = FutureUtils.newFuture();
        RpcClientContext clientContext = new RpcClientContext();
        clientContext.setOneWay(Boolean.FALSE);
        Method method = CommonResultClient.class.getDeclaredMethod("asyncHello");
        RpcMethodInfo methodInfo = new RpcMethodInfo(CommonResultClient.class, method);
        CompletableFuture<Object> future = RpcUtils
                .parseAsyncInvokeResult(response, clientContext, methodInfo);
        response.completeExceptionally(TRpcException.newBizException(1, "aaa"));
        CompletableFuture.allOf(future).join();
        Object result = future.get();
        Assert.assertTrue(result instanceof RpcResult);
    }

    @Test
    public void testParseSyncInvokeResult() {
        CompletableFuture<Response> future = FutureUtils.newFuture();
        Response response = new DefResponse();
        Object value = new Object();
        response.setValue(value);
        new Thread(() -> {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            future.complete(response);
        }).start();

        Object result = RpcUtils.parseSyncInvokeResult(future, new RpcClientContext(), 2000, 3000, null);
        Assert.assertEquals(value, result);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testParseSyncInvokeCommonResult() throws NoSuchMethodException {
        CompletableFuture<Response> future = FutureUtils.newFuture();
        Response response = new DefResponse();
        Object value = new Object();
        response.setValue(value);
        new Thread(() -> {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            future.complete(response);
        }).start();
        Method method = CommonResultClient.class.getDeclaredMethod("hello");
        RpcMethodInfo methodInfo = new RpcMethodInfo(CommonResultClient.class, method);
        RpcResult<Object> result = (RpcResult<Object>) RpcUtils.parseSyncInvokeResult(future,
                new RpcClientContext(), 2000, 3000, methodInfo);
        Assert.assertEquals(value, result.getData());
    }

    @Test(expected = TRpcException.class)
    public void testParseSyncInvoke1() {
        RpcUtils.parseSyncInvokeResult(FutureUtils.newFuture(), new RpcClientContext(),
                1, 20, null);
    }

    @Test(expected = TRpcException.class)
    public void testParseSyncInvoke2() {
        CompletableFuture<Response> future = FutureUtils.newFuture();
        new Thread(() -> {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            future.completeExceptionally(TRpcException.newBizException(1, "aaa"));
        }).start();

        RpcUtils.parseSyncInvokeResult(future, new RpcClientContext(), 2000, 3000, null);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testParseSyncInvoke3() throws NoSuchMethodException {
        Method method = CommonResultClient.class.getDeclaredMethod("hello");
        RpcMethodInfo methodInfo = new RpcMethodInfo(CommonResultClient.class, method);
        RpcResult<Object> result = (RpcResult<Object>) RpcUtils.parseSyncInvokeResult(FutureUtils.newFuture(),
                new RpcClientContext(), 1, 20, methodInfo);
        Assert.assertEquals(result.getCode(), ErrorCode.TRPC_CLIENT_INVOKE_TIMEOUT_ERR);
    }

    @Test
    public void testParseInvokeBackupNormalResult() {
        try {
            Response responseFuture = new DefResponse();
            String value = "hello world!";
            responseFuture.setValue(value);
            ClusterInvoker<?> invoker = new ClusterInvoker<Object>() {
                @Override
                public Class<Object> getInterface() {
                    return null;
                }

                @Override
                public ConsumerConfig<Object> getConfig() {
                    return null;
                }

                @Override
                public BackendConfig getBackendConfig() {
                    return null;
                }

                @Override
                public CompletionStage<Response> invoke(Request request) {
                    return CompletableFuture.supplyAsync(() -> responseFuture);
                }
            };
            CompletionStage<Response> response2 = invoker.invoke(new DefRequest());
            String defResponse = (String) RpcUtils.parseSyncInvokeBackupResult(response2.toCompletableFuture(), 1,
                    new LeftTimeout(3000, 2000), invoker, new DefRequest());
            Assert.assertEquals(value, defResponse);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testParseInvokeBackupAbnormalResult() {
        try {
            Response responseFuture = new DefResponse();
            String value = "hello world!";
            responseFuture.setValue(value);
            ClusterInvoker<?> invoker = new ClusterInvoker<Object>() {
                @Override
                public Class<Object> getInterface() {
                    return null;
                }

                @Override
                public ConsumerConfig<Object> getConfig() {
                    return null;
                }

                @Override
                public BackendConfig getBackendConfig() {
                    return null;
                }

                @Override
                public CompletionStage<Response> invoke(Request request) {
                    return CompletableFuture.supplyAsync(() -> {
                        try {
                            Thread.sleep(300);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        return responseFuture;
                    });
                }
            };
            CompletionStage<Response> response2 = invoker.invoke(new DefRequest());
            String defResponse = (String) RpcUtils.parseSyncInvokeBackupResult(response2.toCompletableFuture(), 1,
                    new LeftTimeout(3000, 2000), invoker, new DefRequest());
            Assert.assertEquals(value, defResponse);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static class SimpleGenericClient implements GenericClient {

        @Override
        public CompletionStage<byte[]> asyncInvoke(RpcClientContext context, byte[] body) {
            return null;
        }

        @Override
        public byte[] invoke(RpcClientContext context, byte[] body) {
            return new byte[0];
        }
    }
}
