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


package com.tencent.trpc.proto.http.server;


import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.doAnswer;
import static org.powermock.api.mockito.PowerMockito.doCallRealMethod;
import static org.powermock.api.mockito.PowerMockito.doReturn;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

import com.tencent.trpc.core.common.config.ProviderConfig;
import com.tencent.trpc.core.exception.ErrorCode;
import com.tencent.trpc.core.exception.TRpcException;
import com.tencent.trpc.core.rpc.ProviderInvoker;
import com.tencent.trpc.core.rpc.RpcInvocation;
import com.tencent.trpc.core.rpc.common.RpcMethodInfo;
import com.tencent.trpc.core.rpc.common.RpcMethodInfoAndInvoker;
import com.tencent.trpc.core.rpc.def.DefRequest;
import com.tencent.trpc.core.worker.spi.WorkerPool;
import com.tencent.trpc.core.worker.spi.WorkerPool.Task;
import com.tencent.trpc.proto.http.common.HttpCodec;
import com.tencent.trpc.proto.http.common.HttpConstants;
import java.util.concurrent.CompletableFuture;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.http.HttpStatus;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;


@PowerMockIgnore({"javax.management.*"})
@RunWith(PowerMockRunner.class)
@PrepareForTest(AbstractHttpExecutor.class)
public class AbstractHttpExecutorTest {


    @Test
    public void buildRpcInvocation_shouldSuccess() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        RpcMethodInfo methodInfo = mock(RpcMethodInfo.class);
        AbstractHttpExecutor abstractHttpExecutor = mock(AbstractHttpExecutor.class);
        doReturn(null).when(abstractHttpExecutor, "parseRpcParams", request, methodInfo);
        doReturn("trpc.demo.server").when(request).getAttribute(HttpConstants.REQUEST_ATTRIBUTE_TRPC_SERVICE);
        doReturn("hello").when(request).getAttribute(HttpConstants.REQUEST_ATTRIBUTE_TRPC_METHOD);
        when(abstractHttpExecutor, "buildRpcInvocation", request, methodInfo).thenCallRealMethod();
        RpcInvocation rpcInvocation = Whitebox.invokeMethod(abstractHttpExecutor, "buildRpcInvocation", request,
                methodInfo);
        assertEquals(rpcInvocation.getFunc(), "/trpc.demo.server/hello");
    }

    @Test
    public void execute_shouldHandleTimeoutException() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        AbstractHttpExecutor abstractHttpExecutor = mock(AbstractHttpExecutor.class);
        RpcMethodInfoAndInvoker methodInfoAndInvoker = mock(RpcMethodInfoAndInvoker.class);
        RpcMethodInfo methodInfo = mock(RpcMethodInfo.class);
        ProviderInvoker<?> invoker = mock(ProviderInvoker.class);
        ProviderConfig providerConfig = mock(ProviderConfig.class);
        WorkerPool workerPool = mock(WorkerPool.class);

        when(methodInfoAndInvoker.getMethodInfo()).thenReturn(methodInfo);
        doReturn(invoker).when(methodInfoAndInvoker, "getInvoker");
        when(invoker.getConfig()).thenReturn(providerConfig);
        when(providerConfig.getRequestTimeout()).thenReturn(100); // 设置100ms超时
        when(providerConfig.getWorkerPoolObj()).thenReturn(workerPool);

        when(request.getAttribute(HttpConstants.REQUEST_ATTRIBUTE_TRPC_SERVICE)).thenReturn("trpc.demo.server");
        when(request.getAttribute(HttpConstants.REQUEST_ATTRIBUTE_TRPC_METHOD)).thenReturn("hello");
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(request.getRemotePort()).thenReturn(8080);

        CompletableFuture<com.tencent.trpc.core.rpc.Response> neverCompleteFuture = new CompletableFuture<>();
        when(invoker.invoke(any())).thenReturn(neverCompleteFuture);

        doReturn(null).when(abstractHttpExecutor, "parseRpcParams", any(), any());
        when(abstractHttpExecutor, "execute", request, response, methodInfoAndInvoker).thenCallRealMethod();
        doCallRealMethod().when(abstractHttpExecutor, "doErrorReply", any(), any(), any());
        doCallRealMethod().when(abstractHttpExecutor, "httpErrorReply", any(), any(), any());
        DefRequest defRequest = new DefRequest();
        doReturn(defRequest).when(abstractHttpExecutor, "buildDefRequest", any(), any(), any());
        HttpCodec httpCodec = mock(HttpCodec.class);
        Whitebox.setInternalState(abstractHttpExecutor, "httpCodec", httpCodec);
        when(abstractHttpExecutor, "invokeRpcRequest", any(), any(), any(), any()).thenCallRealMethod();

        Whitebox.invokeMethod(abstractHttpExecutor, "execute", request, response, methodInfoAndInvoker);

        verify(response).setStatus(HttpStatus.SC_REQUEST_TIMEOUT);
    }

    @Test
    public void execute_shouldHandleInvokeException() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        AbstractHttpExecutor abstractHttpExecutor = mock(AbstractHttpExecutor.class);
        RpcMethodInfoAndInvoker methodInfoAndInvoker = mock(RpcMethodInfoAndInvoker.class);
        RpcMethodInfo methodInfo = mock(RpcMethodInfo.class);
        ProviderInvoker<?> invoker = mock(ProviderInvoker.class);
        ProviderConfig providerConfig = mock(ProviderConfig.class);
        WorkerPool workerPool = mock(WorkerPool.class);

        when(methodInfoAndInvoker.getMethodInfo()).thenReturn(methodInfo);
        doReturn(invoker).when(methodInfoAndInvoker, "getInvoker");
        when(invoker.getConfig()).thenReturn(providerConfig);
        when(providerConfig.getRequestTimeout()).thenReturn(5000);
        when(providerConfig.getWorkerPoolObj()).thenReturn(workerPool);

        when(request.getAttribute(HttpConstants.REQUEST_ATTRIBUTE_TRPC_SERVICE)).thenReturn("trpc.demo.server");
        when(request.getAttribute(HttpConstants.REQUEST_ATTRIBUTE_TRPC_METHOD)).thenReturn("hello");
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(request.getRemotePort()).thenReturn(8080);
        when(request.getMethod()).thenReturn("POST");
        when(request.getRequestURI()).thenReturn("/api/test");

        doAnswer(invocation -> {
            Task task = invocation.getArgumentAt(0, Task.class);
            task.run();
            return null;
        }).when(workerPool).execute(any(Task.class));

        CompletableFuture<com.tencent.trpc.core.rpc.Response> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new RuntimeException("Service invoke failed"));
        when(invoker.invoke(any())).thenReturn(failedFuture);

        doReturn(null).when(abstractHttpExecutor, "parseRpcParams", any(), any());
        when(abstractHttpExecutor, "execute", request, response, methodInfoAndInvoker).thenCallRealMethod();
        doCallRealMethod().when(abstractHttpExecutor, "doErrorReply", any(), any(), any());
        doCallRealMethod().when(abstractHttpExecutor, "httpErrorReply", any(), any(), any());
        DefRequest defRequest = new DefRequest();
        doReturn(defRequest).when(abstractHttpExecutor, "buildDefRequest", any(), any(), any());
        HttpCodec httpCodec = mock(HttpCodec.class);
        Whitebox.setInternalState(abstractHttpExecutor, "httpCodec", httpCodec);
        when(abstractHttpExecutor, "invokeRpcRequest", any(), any(), any(), any()).thenCallRealMethod();

        Whitebox.invokeMethod(abstractHttpExecutor, "execute", request, response, methodInfoAndInvoker);

        verify(response).setStatus(HttpStatus.SC_REQUEST_TIMEOUT);
    }

    @Test
    public void execute_shouldHandleResponseException() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        AbstractHttpExecutor abstractHttpExecutor = mock(AbstractHttpExecutor.class);
        RpcMethodInfoAndInvoker methodInfoAndInvoker = mock(RpcMethodInfoAndInvoker.class);
        RpcMethodInfo methodInfo = mock(RpcMethodInfo.class);
        ProviderInvoker<?> invoker = mock(ProviderInvoker.class);
        ProviderConfig providerConfig = mock(ProviderConfig.class);
        WorkerPool workerPool = mock(WorkerPool.class);

        when(methodInfoAndInvoker.getMethodInfo()).thenReturn(methodInfo);
        doReturn(invoker).when(methodInfoAndInvoker, "getInvoker");
        when(invoker.getConfig()).thenReturn(providerConfig);
        when(providerConfig.getRequestTimeout()).thenReturn(5000);
        when(providerConfig.getWorkerPoolObj()).thenReturn(workerPool);

        when(request.getAttribute(HttpConstants.REQUEST_ATTRIBUTE_TRPC_SERVICE)).thenReturn("trpc.demo.server");
        when(request.getAttribute(HttpConstants.REQUEST_ATTRIBUTE_TRPC_METHOD)).thenReturn("hello");
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(request.getRemotePort()).thenReturn(8080);
        when(request.getMethod()).thenReturn("POST");
        when(request.getRequestURI()).thenReturn("/api/test");

        doAnswer(invocation -> {
            Task task = invocation.getArgumentAt(0, Task.class);
            task.run();
            return null;
        }).when(workerPool).execute(any(Task.class));

        com.tencent.trpc.core.rpc.Response rpcResponse = mock(com.tencent.trpc.core.rpc.Response.class);
        when(rpcResponse.getException()).thenReturn(
                TRpcException.newFrameException(ErrorCode.TRPC_SERVER_VALIDATE_ERR, "Validation failed"));
        CompletableFuture<com.tencent.trpc.core.rpc.Response> responseFuture = CompletableFuture.completedFuture(rpcResponse);
        when(invoker.invoke(any())).thenReturn(responseFuture);

        doReturn(null).when(abstractHttpExecutor, "parseRpcParams", any(), any());
        when(abstractHttpExecutor, "execute", request, response, methodInfoAndInvoker).thenCallRealMethod();
        doCallRealMethod().when(abstractHttpExecutor, "doErrorReply", any(), any(), any());
        doCallRealMethod().when(abstractHttpExecutor, "httpErrorReply", any(), any(), any());
        DefRequest defRequest = new DefRequest();
        doReturn(defRequest).when(abstractHttpExecutor, "buildDefRequest", any(), any(), any());
        HttpCodec httpCodec = mock(HttpCodec.class);
        Whitebox.setInternalState(abstractHttpExecutor, "httpCodec", httpCodec);
        when(abstractHttpExecutor, "invokeRpcRequest", any(), any(), any(), any()).thenCallRealMethod();

        Whitebox.invokeMethod(abstractHttpExecutor, "execute", request, response, methodInfoAndInvoker);

        verify(response).setStatus(HttpStatus.SC_REQUEST_TIMEOUT);
    }
}
