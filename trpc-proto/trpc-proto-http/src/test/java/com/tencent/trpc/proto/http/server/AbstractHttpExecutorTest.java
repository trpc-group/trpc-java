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
import com.tencent.trpc.core.rpc.Response;
import com.tencent.trpc.core.rpc.common.RpcMethodInfo;
import com.tencent.trpc.core.rpc.common.RpcMethodInfoAndInvoker;
import com.tencent.trpc.core.rpc.def.DefRequest;
import com.tencent.trpc.core.worker.spi.WorkerPool;
import com.tencent.trpc.core.worker.spi.WorkerPool.Task;
import com.tencent.trpc.proto.http.common.HttpCodec;
import com.tencent.trpc.proto.http.common.HttpConstants;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
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
        doReturn("trpc.demo.server").when(request).getAttribute(HttpConstants.REQUEST_ATTRIBUTE_TRPC_SERVICE);
        doReturn("hello").when(request).getAttribute(HttpConstants.REQUEST_ATTRIBUTE_TRPC_METHOD);
        
        RpcMethodInfo methodInfo = mock(RpcMethodInfo.class);
        AbstractHttpExecutor abstractHttpExecutor = mock(AbstractHttpExecutor.class);
        doReturn(null).when(abstractHttpExecutor, "parseRpcParams", request, methodInfo);
        when(abstractHttpExecutor, "buildRpcInvocation", request, methodInfo).thenCallRealMethod();
        
        RpcInvocation rpcInvocation = Whitebox.invokeMethod(abstractHttpExecutor, "buildRpcInvocation", request,
                methodInfo);
        assertEquals(rpcInvocation.getFunc(), "/trpc.demo.server/hello");
    }

    @Test
    public void execute_shouldCompleteSuccessfully() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getAttribute(HttpConstants.REQUEST_ATTRIBUTE_TRPC_SERVICE)).thenReturn("trpc.demo.server");
        when(request.getAttribute(HttpConstants.REQUEST_ATTRIBUTE_TRPC_METHOD)).thenReturn("hello");
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(request.getRemotePort()).thenReturn(8080);

        HttpServletResponse response = mock(HttpServletResponse.class);
        RpcMethodInfo methodInfo = mock(RpcMethodInfo.class);
        ProviderInvoker<?> invoker = mock(ProviderInvoker.class);

        // 创建一个成功的响应
        com.tencent.trpc.core.rpc.def.DefResponse successResponse =
                new com.tencent.trpc.core.rpc.def.DefResponse();
        successResponse.setValue("success");
        CompletableFuture<Response> successFuture = CompletableFuture.completedFuture(successResponse);
        when(invoker.invoke(any())).thenReturn(successFuture);

        ProviderConfig providerConfig = mock(ProviderConfig.class);
        when(providerConfig.getRequestTimeout()).thenReturn(0); // 不设置超时，走completionFuture.get()分支
        WorkerPool workerPool = mock(WorkerPool.class);
        doAnswer(invocation -> {
            Object arg = invocation.getArguments()[0];
            if (arg instanceof Runnable) {
                ((Runnable) arg).run();
            } else if (arg instanceof Task) {
                ((Task) arg).run();
            }
            return null;
        }).when(workerPool).execute(any());
        when(providerConfig.getWorkerPoolObj()).thenReturn(workerPool);
        when(invoker.getConfig()).thenReturn(providerConfig);

        RpcMethodInfoAndInvoker methodInfoAndInvoker = mock(RpcMethodInfoAndInvoker.class);
        when(methodInfoAndInvoker.getMethodInfo()).thenReturn(methodInfo);
        doReturn(invoker).when(methodInfoAndInvoker, "getInvoker");

        AbstractHttpExecutor abstractHttpExecutor = mock(AbstractHttpExecutor.class);
        DefRequest defRequest = new DefRequest();
        defRequest.getAttachments().put(HttpConstants.TRPC_ATTACH_SERVLET_RESPONSE, response);
        defRequest.getAttachments().put(HttpConstants.TRPC_ATTACH_SERVLET_REQUEST, request);
        doReturn(defRequest).when(abstractHttpExecutor, "buildDefRequest", any(), any(), any());

        HttpCodec httpCodec = mock(HttpCodec.class);
        Whitebox.setInternalState(abstractHttpExecutor, "httpCodec", httpCodec);
        doReturn(response).when(abstractHttpExecutor, "getOriginalResponse", any());
        doCallRealMethod().when(abstractHttpExecutor, "execute", request, response, methodInfoAndInvoker);
        doCallRealMethod().when(abstractHttpExecutor, "invokeRpcRequest", any(), any(), any(), any());

        // 执行测试
        Whitebox.invokeMethod(abstractHttpExecutor, "execute", request, response, methodInfoAndInvoker);

        // 验证正常响应
        verify(response).setStatus(HttpStatus.SC_OK);
        verify(httpCodec).writeHttpResponse(response, successResponse);
        verify(response).flushBuffer();
    }

    @Test
    public void execute_shouldHandleTimeoutException() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getAttribute(HttpConstants.REQUEST_ATTRIBUTE_TRPC_SERVICE)).thenReturn("trpc.demo.server");
        when(request.getAttribute(HttpConstants.REQUEST_ATTRIBUTE_TRPC_METHOD)).thenReturn("hello");
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(request.getRemotePort()).thenReturn(8080);

        HttpServletResponse response = mock(HttpServletResponse.class);
        
        RpcMethodInfo methodInfo = mock(RpcMethodInfo.class);
        ProviderInvoker<?> invoker = mock(ProviderInvoker.class);
        CompletableFuture<com.tencent.trpc.core.rpc.Response> neverCompleteFuture = new CompletableFuture<>();
        when(invoker.invoke(any())).thenReturn(neverCompleteFuture);
        
        ProviderConfig providerConfig = mock(ProviderConfig.class);
        when(providerConfig.getRequestTimeout()).thenReturn(100); // 设置100ms超时
        WorkerPool workerPool = mock(WorkerPool.class);
        when(providerConfig.getWorkerPoolObj()).thenReturn(workerPool);
        when(invoker.getConfig()).thenReturn(providerConfig);

        RpcMethodInfoAndInvoker methodInfoAndInvoker = mock(RpcMethodInfoAndInvoker.class);
        when(methodInfoAndInvoker.getMethodInfo()).thenReturn(methodInfo);
        doReturn(invoker).when(methodInfoAndInvoker, "getInvoker");

        AbstractHttpExecutor abstractHttpExecutor = mock(AbstractHttpExecutor.class);
        doReturn(null).when(abstractHttpExecutor, "parseRpcParams", any(), any());
        DefRequest defRequest = new DefRequest();
        doReturn(defRequest).when(abstractHttpExecutor, "buildDefRequest", any(), any(), any());
        HttpCodec httpCodec = mock(HttpCodec.class);
        Whitebox.setInternalState(abstractHttpExecutor, "httpCodec", httpCodec);
        when(abstractHttpExecutor, "execute", request, response, methodInfoAndInvoker).thenCallRealMethod();
        doCallRealMethod().when(abstractHttpExecutor, "doErrorReply", any(), any(), any());
        doCallRealMethod().when(abstractHttpExecutor, "httpErrorReply", any(), any(), any());
        when(abstractHttpExecutor, "invokeRpcRequest", any(), any(), any(), any()).thenCallRealMethod();

        Whitebox.invokeMethod(abstractHttpExecutor, "execute", request, response, methodInfoAndInvoker);

        verify(response).setStatus(HttpStatus.SC_REQUEST_TIMEOUT);
    }

    @Test
    public void handleError_shouldHandleErrorCorrectly() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(request.getMethod()).thenReturn("POST");
        when(request.getRequestURI()).thenReturn("/api/test");
        when(request.getQueryString()).thenReturn("param=value");

        HttpServletResponse response = mock(HttpServletResponse.class);
        DefRequest defRequest = new DefRequest();
        defRequest.getAttachments().put(HttpConstants.TRPC_ATTACH_SERVLET_REQUEST, request);

        AbstractHttpExecutor abstractHttpExecutor = mock(AbstractHttpExecutor.class);
        HttpCodec httpCodec = mock(HttpCodec.class);
        Whitebox.setInternalState(abstractHttpExecutor, "httpCodec", httpCodec);
        doCallRealMethod().when(abstractHttpExecutor, "handleError", any(Throwable.class), any(DefRequest.class),
                any(HttpServletResponse.class), any(AtomicBoolean.class), any(CompletableFuture.class));
        doCallRealMethod().when(abstractHttpExecutor, "httpErrorReply", any(), any(), any());
        doReturn(request).when(abstractHttpExecutor, "getOriginalRequest", any());

        AtomicBoolean responded = new AtomicBoolean(false);
        CompletableFuture<Void> completionFuture = new CompletableFuture<>();
        Throwable testException = new RuntimeException("Test error");
        Whitebox.invokeMethod(abstractHttpExecutor, "handleError", testException, defRequest, response,
                responded, completionFuture);

        assertEquals(true, responded.get());
        assertEquals(true, completionFuture.isCompletedExceptionally());
        verify(response).setStatus(HttpStatus.SC_SERVICE_UNAVAILABLE);
        verify(httpCodec).writeHttpResponse(any(HttpServletResponse.class), any());
    }

    @Test
    public void invokeRpcRequest_shouldHandleThrowable() throws Exception {
        HttpServletResponse response = mock(HttpServletResponse.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        DefRequest defRequest = new DefRequest();
        defRequest.getAttachments().put(HttpConstants.TRPC_ATTACH_SERVLET_RESPONSE, response);
        defRequest.getAttachments().put(HttpConstants.TRPC_ATTACH_SERVLET_REQUEST, request);

        WorkerPool workerPool = mock(WorkerPool.class);
        doAnswer(invocation -> {
            Object arg = invocation.getArguments()[0];
            if (arg instanceof Runnable) {
                ((Runnable) arg).run();
            } else if (arg instanceof Task) {
                ((Task) arg).run();
            }
            return null;
        }).when(workerPool).execute(any());

        ProviderConfig providerConfig = mock(ProviderConfig.class);
        when(providerConfig.getWorkerPoolObj()).thenReturn(workerPool);

        ProviderInvoker<?> invoker = mock(ProviderInvoker.class);
        when(invoker.getConfig()).thenReturn(providerConfig);
        CompletableFuture<Response> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new RuntimeException("boom"));
        when(invoker.invoke(any())).thenReturn(failedFuture);

        AbstractHttpExecutor abstractHttpExecutor = mock(AbstractHttpExecutor.class);
        HttpCodec httpCodec = mock(HttpCodec.class);
        Whitebox.setInternalState(abstractHttpExecutor, "httpCodec", httpCodec);
        doReturn(response).when(abstractHttpExecutor, "getOriginalResponse", any());
        doReturn(request).when(abstractHttpExecutor, "getOriginalRequest", any());
        doCallRealMethod().when(abstractHttpExecutor, "invokeRpcRequest", any(), any(), any(), any());
        doCallRealMethod().when(abstractHttpExecutor, "httpErrorReply", any(), any(), any());
        doCallRealMethod().when(abstractHttpExecutor, "handleError", any(Throwable.class), any(DefRequest.class),
                any(HttpServletResponse.class), any(AtomicBoolean.class), any(CompletableFuture.class));

        AtomicBoolean responded = new AtomicBoolean(false);
        CompletableFuture<Void> completionFuture = new CompletableFuture<>();
        Whitebox.invokeMethod(abstractHttpExecutor, "invokeRpcRequest", invoker, defRequest, completionFuture,
                responded);

        assertEquals(true, responded.get());
        assertEquals(true, completionFuture.isCompletedExceptionally());
        verify(response).setStatus(org.apache.http.HttpStatus.SC_SERVICE_UNAVAILABLE);
        verify(httpCodec).writeHttpResponse(any(HttpServletResponse.class), any());
    }

    @Test
    public void invokeRpcRequest_shouldHandleInvokeThrowsExceptionDirectly() throws Exception {
        HttpServletResponse response = mock(HttpServletResponse.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        DefRequest defRequest = new DefRequest();
        defRequest.getAttachments().put(HttpConstants.TRPC_ATTACH_SERVLET_RESPONSE, response);
        defRequest.getAttachments().put(HttpConstants.TRPC_ATTACH_SERVLET_REQUEST, request);

        WorkerPool workerPool = mock(WorkerPool.class);
        doAnswer(invocation -> {
            Object arg = invocation.getArguments()[0];
            if (arg instanceof Runnable) {
                ((Runnable) arg).run();
            } else if (arg instanceof Task) {
                ((Task) arg).run();
            }
            return null;
        }).when(workerPool).execute(any());

        ProviderConfig providerConfig = mock(ProviderConfig.class);
        when(providerConfig.getWorkerPoolObj()).thenReturn(workerPool);

        ProviderInvoker<?> invoker = mock(ProviderInvoker.class);
        when(invoker.getConfig()).thenReturn(providerConfig);
        when(invoker.invoke(any())).thenThrow(new RuntimeException("boom-direct"));

        AbstractHttpExecutor abstractHttpExecutor = mock(AbstractHttpExecutor.class);
        HttpCodec httpCodec = mock(HttpCodec.class);
        Whitebox.setInternalState(abstractHttpExecutor, "httpCodec", httpCodec);
        doReturn(response).when(abstractHttpExecutor, "getOriginalResponse", any());
        doReturn(request).when(abstractHttpExecutor, "getOriginalRequest", any());
        doCallRealMethod().when(abstractHttpExecutor, "invokeRpcRequest", any(), any(), any(), any());
        doCallRealMethod().when(abstractHttpExecutor, "httpErrorReply", any(), any(), any());
        doCallRealMethod().when(abstractHttpExecutor, "handleError", any(Throwable.class), any(DefRequest.class),
                any(HttpServletResponse.class), any(AtomicBoolean.class), any(CompletableFuture.class));

        AtomicBoolean responded = new AtomicBoolean(false);
        CompletableFuture<Void> completionFuture = new CompletableFuture<>();
        Whitebox.invokeMethod(abstractHttpExecutor, "invokeRpcRequest", invoker, defRequest, completionFuture,
                responded);

        assertEquals(true, responded.get());
        assertEquals(true, completionFuture.isCompletedExceptionally());
        verify(response).setStatus(HttpStatus.SC_SERVICE_UNAVAILABLE);
        verify(httpCodec).writeHttpResponse(any(HttpServletResponse.class), any());
    }
}