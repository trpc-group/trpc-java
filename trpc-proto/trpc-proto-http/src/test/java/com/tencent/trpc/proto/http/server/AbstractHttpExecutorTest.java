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
import com.tencent.trpc.core.worker.spi.WorkerPool;
import com.tencent.trpc.core.worker.spi.WorkerPool.Task;
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
        // 准备测试数据
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        AbstractHttpExecutor abstractHttpExecutor = mock(AbstractHttpExecutor.class);
        RpcMethodInfoAndInvoker methodInfoAndInvoker = mock(RpcMethodInfoAndInvoker.class);
        RpcMethodInfo methodInfo = mock(RpcMethodInfo.class);
        ProviderInvoker<?> invoker = mock(ProviderInvoker.class);
        ProviderConfig providerConfig = mock(ProviderConfig.class);
        WorkerPool workerPool = mock(WorkerPool.class);

        // 模拟配置和基本信息
        when(methodInfoAndInvoker.getMethodInfo()).thenReturn(methodInfo);
        doReturn(invoker).when(methodInfoAndInvoker, "getInvoker");
        when(invoker.getConfig()).thenReturn(providerConfig);
        when(providerConfig.getRequestTimeout()).thenReturn(100); // 设置100ms超时
        when(providerConfig.getWorkerPoolObj()).thenReturn(workerPool);

        // 模拟请求属性
        when(request.getAttribute(HttpConstants.REQUEST_ATTRIBUTE_TRPC_SERVICE)).thenReturn("trpc.demo.server");
        when(request.getAttribute(HttpConstants.REQUEST_ATTRIBUTE_TRPC_METHOD)).thenReturn("hello");
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(request.getRemotePort()).thenReturn(8080);

        // 模拟一个永远不会完成的CompletionStage，导致超时
        CompletableFuture<com.tencent.trpc.core.rpc.Response> neverCompleteFuture = new CompletableFuture<>();
        when(invoker.invoke(any())).thenReturn(neverCompleteFuture);

        // 模拟私有方法调用
        doReturn(null).when(abstractHttpExecutor, "parseRpcParams", any(), any());
        when(abstractHttpExecutor, "execute", request, response, methodInfoAndInvoker).thenCallRealMethod();
        doCallRealMethod().when(abstractHttpExecutor, "doErrorReply", any(), any(), any());
        doCallRealMethod().when(abstractHttpExecutor, "httpErrorReply", any(), any(), any());

        // 执行测试
        Whitebox.invokeMethod(abstractHttpExecutor, "execute", request, response, methodInfoAndInvoker);

        // 验证超时后调用了错误响应（由于异步执行，超时也通过handleError处理，返回503）
        verify(response).setStatus(HttpStatus.SC_SERVICE_UNAVAILABLE);
    }

    @Test
    public void execute_shouldHandleInvokeException() throws Exception {
        // 准备测试数据
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        AbstractHttpExecutor abstractHttpExecutor = mock(AbstractHttpExecutor.class);
        RpcMethodInfoAndInvoker methodInfoAndInvoker = mock(RpcMethodInfoAndInvoker.class);
        RpcMethodInfo methodInfo = mock(RpcMethodInfo.class);
        ProviderInvoker<?> invoker = mock(ProviderInvoker.class);
        ProviderConfig providerConfig = mock(ProviderConfig.class);
        WorkerPool workerPool = mock(WorkerPool.class);

        // 模拟配置和基本信息
        when(methodInfoAndInvoker.getMethodInfo()).thenReturn(methodInfo);
        doReturn(invoker).when(methodInfoAndInvoker, "getInvoker");
        when(invoker.getConfig()).thenReturn(providerConfig);
        when(providerConfig.getRequestTimeout()).thenReturn(5000);
        when(providerConfig.getWorkerPoolObj()).thenReturn(workerPool);

        // 模拟请求属性
        when(request.getAttribute(HttpConstants.REQUEST_ATTRIBUTE_TRPC_SERVICE)).thenReturn("trpc.demo.server");
        when(request.getAttribute(HttpConstants.REQUEST_ATTRIBUTE_TRPC_METHOD)).thenReturn("hello");
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(request.getRemotePort()).thenReturn(8080);
        when(request.getMethod()).thenReturn("POST");
        when(request.getRequestURI()).thenReturn("/api/test");

        // 模拟WorkerPool同步执行任务
        doAnswer(invocation -> {
            Task task = invocation.getArgumentAt(0, Task.class);
            task.run();
            return null;
        }).when(workerPool).execute(any(Task.class));

        // 模拟invoke抛出异常
        CompletableFuture<com.tencent.trpc.core.rpc.Response> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new RuntimeException("Service invoke failed"));
        when(invoker.invoke(any())).thenReturn(failedFuture);

        // 模拟私有方法调用
        doReturn(null).when(abstractHttpExecutor, "parseRpcParams", any(), any());
        when(abstractHttpExecutor, "execute", request, response, methodInfoAndInvoker).thenCallRealMethod();
        doCallRealMethod().when(abstractHttpExecutor, "doErrorReply", any(), any(), any());
        doCallRealMethod().when(abstractHttpExecutor, "httpErrorReply", any(), any(), any());

        // 执行测试
        Whitebox.invokeMethod(abstractHttpExecutor, "execute", request, response, methodInfoAndInvoker);

        // 验证调用了错误响应（invoke异常在handleError中处理，返回503）
        verify(response).setStatus(HttpStatus.SC_SERVICE_UNAVAILABLE);
    }

    @Test
    public void execute_shouldHandleResponseException() throws Exception {
        // 准备测试数据
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        AbstractHttpExecutor abstractHttpExecutor = mock(AbstractHttpExecutor.class);
        RpcMethodInfoAndInvoker methodInfoAndInvoker = mock(RpcMethodInfoAndInvoker.class);
        RpcMethodInfo methodInfo = mock(RpcMethodInfo.class);
        ProviderInvoker<?> invoker = mock(ProviderInvoker.class);
        ProviderConfig providerConfig = mock(ProviderConfig.class);
        WorkerPool workerPool = mock(WorkerPool.class);

        // 模拟配置和基本信息
        when(methodInfoAndInvoker.getMethodInfo()).thenReturn(methodInfo);
        doReturn(invoker).when(methodInfoAndInvoker, "getInvoker");
        when(invoker.getConfig()).thenReturn(providerConfig);
        when(providerConfig.getRequestTimeout()).thenReturn(5000);
        when(providerConfig.getWorkerPoolObj()).thenReturn(workerPool);

        // 模拟请求属性
        when(request.getAttribute(HttpConstants.REQUEST_ATTRIBUTE_TRPC_SERVICE)).thenReturn("trpc.demo.server");
        when(request.getAttribute(HttpConstants.REQUEST_ATTRIBUTE_TRPC_METHOD)).thenReturn("hello");
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(request.getRemotePort()).thenReturn(8080);
        when(request.getMethod()).thenReturn("POST");
        when(request.getRequestURI()).thenReturn("/api/test");

        // 模拟WorkerPool同步执行任务
        doAnswer(invocation -> {
            Task task = invocation.getArgumentAt(0, Task.class);
            task.run();
            return null;
        }).when(workerPool).execute(any(Task.class));

        // 模拟Response包含异常
        com.tencent.trpc.core.rpc.Response rpcResponse = mock(com.tencent.trpc.core.rpc.Response.class);
        when(rpcResponse.getException()).thenReturn(
                TRpcException.newFrameException(ErrorCode.TRPC_SERVER_VALIDATE_ERR, "Validation failed"));
        CompletableFuture<com.tencent.trpc.core.rpc.Response> responseFuture = CompletableFuture.completedFuture(rpcResponse);
        when(invoker.invoke(any())).thenReturn(responseFuture);

        // 模拟私有方法调用
        doReturn(null).when(abstractHttpExecutor, "parseRpcParams", any(), any());
        when(abstractHttpExecutor, "execute", request, response, methodInfoAndInvoker).thenCallRealMethod();
        doCallRealMethod().when(abstractHttpExecutor, "doErrorReply", any(), any(), any());
        doCallRealMethod().when(abstractHttpExecutor, "httpErrorReply", any(), any(), any());

        // 执行测试
        Whitebox.invokeMethod(abstractHttpExecutor, "execute", request, response, methodInfoAndInvoker);

        // 验证调用了错误响应（Response异常在handleError中处理，返回503）
        verify(response).setStatus(HttpStatus.SC_SERVICE_UNAVAILABLE);
    }
}
