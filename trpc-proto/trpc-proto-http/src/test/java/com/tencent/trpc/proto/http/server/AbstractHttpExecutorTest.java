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
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.doAnswer;
import static org.powermock.api.mockito.PowerMockito.doCallRealMethod;
import static org.powermock.api.mockito.PowerMockito.doReturn;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

import com.tencent.trpc.core.common.config.ProviderConfig;
import com.tencent.trpc.core.rpc.ProviderInvoker;
import com.tencent.trpc.core.rpc.RpcInvocation;
import com.tencent.trpc.core.rpc.Response;
import com.tencent.trpc.core.rpc.common.RpcMethodInfo;
import com.tencent.trpc.core.rpc.common.RpcMethodInfoAndInvoker;
import com.tencent.trpc.core.rpc.def.DefRequest;
import com.tencent.trpc.core.rpc.def.DefResponse;
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

    private static final String TEST_SERVICE = "trpc.demo.server";
    private static final String TEST_METHOD = "hello";
    private static final String TEST_IP = "127.0.0.1";
    private static final int TEST_PORT = 8080;

    private HttpServletRequest mockRequest() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getAttribute(HttpConstants.REQUEST_ATTRIBUTE_TRPC_SERVICE)).thenReturn(TEST_SERVICE);
        when(request.getAttribute(HttpConstants.REQUEST_ATTRIBUTE_TRPC_METHOD)).thenReturn(TEST_METHOD);
        when(request.getRemoteAddr()).thenReturn(TEST_IP);
        when(request.getRemotePort()).thenReturn(TEST_PORT);
        return request;
    }

    private WorkerPool mockSyncWorkerPool() {
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
        return workerPool;
    }

    private ProviderConfig mockProviderConfig(int timeout) {
        ProviderConfig config = mock(ProviderConfig.class);
        when(config.getRequestTimeout()).thenReturn(timeout);
        WorkerPool workerPool = mockSyncWorkerPool();
        when(config.getWorkerPoolObj()).thenReturn(workerPool);
        return config;
    }

    private DefRequest mockDefRequest(HttpServletRequest request, HttpServletResponse response) {
        DefRequest defRequest = new DefRequest();
        defRequest.getAttachments().put(HttpConstants.TRPC_ATTACH_SERVLET_RESPONSE, response);
        defRequest.getAttachments().put(HttpConstants.TRPC_ATTACH_SERVLET_REQUEST, request);
        return defRequest;
    }

    private AbstractHttpExecutor mockExecutorWithCodec() {
        AbstractHttpExecutor executor = mock(AbstractHttpExecutor.class);
        HttpCodec httpCodec = mock(HttpCodec.class);
        Whitebox.setInternalState(executor, "httpCodec", httpCodec);
        return executor;
    }

    @Test
    public void testBuildRpcInvocation() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        doReturn(TEST_SERVICE).when(request).getAttribute(HttpConstants.REQUEST_ATTRIBUTE_TRPC_SERVICE);
        doReturn(TEST_METHOD).when(request).getAttribute(HttpConstants.REQUEST_ATTRIBUTE_TRPC_METHOD);
        
        RpcMethodInfo methodInfo = mock(RpcMethodInfo.class);
        AbstractHttpExecutor executor = mock(AbstractHttpExecutor.class);
        doReturn(null).when(executor, "parseRpcParams", request, methodInfo);
        when(executor, "buildRpcInvocation", request, methodInfo).thenCallRealMethod();
        
        RpcInvocation invocation = Whitebox.invokeMethod(executor, "buildRpcInvocation", request, methodInfo);
        assertEquals("/trpc.demo.server/hello", invocation.getFunc());
    }

    @Test
    public void testExecuteSuccess() throws Exception {
        HttpServletRequest request = mockRequest();
        HttpServletResponse response = mock(HttpServletResponse.class);
        RpcMethodInfo methodInfo = mock(RpcMethodInfo.class);
        ProviderInvoker<?> invoker = mock(ProviderInvoker.class);

        DefResponse successResponse = new DefResponse();
        successResponse.setValue("success");
        CompletableFuture<Response> successFuture = CompletableFuture.completedFuture(successResponse);
        when(invoker.invoke(any())).thenReturn(successFuture);

        ProviderConfig config = mockProviderConfig(0);
        when(invoker.getConfig()).thenReturn(config);

        RpcMethodInfoAndInvoker methodInfoAndInvoker = mock(RpcMethodInfoAndInvoker.class);
        when(methodInfoAndInvoker.getMethodInfo()).thenReturn(methodInfo);
        doReturn(invoker).when(methodInfoAndInvoker, "getInvoker");

        AbstractHttpExecutor executor = mockExecutorWithCodec();
        DefRequest defRequest = mockDefRequest(request, response);
        doReturn(defRequest).when(executor, "buildDefRequest", any(), any(), any());
        doReturn(response).when(executor, "getOriginalResponse", any());
        doCallRealMethod().when(executor, "execute", request, response, methodInfoAndInvoker);
        doCallRealMethod().when(executor, "invokeRpcRequest", any(), any(), any(), any());

        Whitebox.invokeMethod(executor, "execute", request, response, methodInfoAndInvoker);

        HttpCodec httpCodec = Whitebox.getInternalState(executor, "httpCodec");
        verify(response).setStatus(HttpStatus.SC_OK);
        verify(httpCodec).writeHttpResponse(response, successResponse);
        verify(response).flushBuffer();
    }

    @Test
    public void testExecuteTimeout() throws Exception {
        HttpServletRequest request = mockRequest();
        HttpServletResponse response = mock(HttpServletResponse.class);
        
        RpcMethodInfo methodInfo = mock(RpcMethodInfo.class);
        ProviderInvoker<?> invoker = mock(ProviderInvoker.class);
        CompletableFuture<Response> neverCompleteFuture = new CompletableFuture<>();
        when(invoker.invoke(any())).thenReturn(neverCompleteFuture);
        
        ProviderConfig config = mockProviderConfig(100);
        when(invoker.getConfig()).thenReturn(config);

        RpcMethodInfoAndInvoker methodInfoAndInvoker = mock(RpcMethodInfoAndInvoker.class);
        when(methodInfoAndInvoker.getMethodInfo()).thenReturn(methodInfo);
        doReturn(invoker).when(methodInfoAndInvoker, "getInvoker");

        AbstractHttpExecutor executor = mockExecutorWithCodec();
        doReturn(null).when(executor, "parseRpcParams", any(), any());
        DefRequest defRequest = new DefRequest();
        doReturn(defRequest).when(executor, "buildDefRequest", any(), any(), any());
        when(executor, "execute", request, response, methodInfoAndInvoker).thenCallRealMethod();
        doCallRealMethod().when(executor, "doErrorReply", any(), any(), any());
        doCallRealMethod().when(executor, "httpErrorReply", any(), any(), any());
        when(executor, "invokeRpcRequest", any(), any(), any(), any()).thenCallRealMethod();

        Whitebox.invokeMethod(executor, "execute", request, response, methodInfoAndInvoker);

        verify(response).setStatus(HttpStatus.SC_REQUEST_TIMEOUT);
    }

    @Test
    public void testHandleError() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn(TEST_IP);
        when(request.getMethod()).thenReturn("POST");
        when(request.getRequestURI()).thenReturn("/api/test");
        when(request.getQueryString()).thenReturn("param=value");

        HttpServletResponse response = mock(HttpServletResponse.class);
        DefRequest defRequest = new DefRequest();
        defRequest.getAttachments().put(HttpConstants.TRPC_ATTACH_SERVLET_REQUEST, request);

        AbstractHttpExecutor executor = mockExecutorWithCodec();
        doCallRealMethod().when(executor, "handleError", any(Throwable.class), any(DefRequest.class),
                any(HttpServletResponse.class), any(AtomicBoolean.class), any(CompletableFuture.class));
        doCallRealMethod().when(executor, "httpErrorReply", any(), any(), any());
        doReturn(request).when(executor, "getOriginalRequest", any());

        AtomicBoolean responded = new AtomicBoolean(false);
        CompletableFuture<Void> completionFuture = new CompletableFuture<>();
        Throwable testException = new RuntimeException("Test error");
        Whitebox.invokeMethod(executor, "handleError", testException, defRequest, response,
                responded, completionFuture);

        HttpCodec httpCodec = Whitebox.getInternalState(executor, "httpCodec");
        assertTrue(responded.get());
        assertTrue(completionFuture.isCompletedExceptionally());
        verify(response).setStatus(HttpStatus.SC_SERVICE_UNAVAILABLE);
        verify(httpCodec).writeHttpResponse(any(HttpServletResponse.class), any());
    }

    @Test
    public void testInvokeRpcWithException() throws Exception {
        HttpServletResponse response = mock(HttpServletResponse.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        DefRequest defRequest = mockDefRequest(request, response);

        ProviderConfig config = mockProviderConfig(0);
        ProviderInvoker<?> invoker = mock(ProviderInvoker.class);
        when(invoker.getConfig()).thenReturn(config);
        CompletableFuture<Response> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new RuntimeException("boom"));
        when(invoker.invoke(any())).thenReturn(failedFuture);

        AbstractHttpExecutor executor = mockExecutorWithCodec();
        doReturn(response).when(executor, "getOriginalResponse", any());
        doReturn(request).when(executor, "getOriginalRequest", any());
        doCallRealMethod().when(executor, "invokeRpcRequest", any(), any(), any(), any());
        doCallRealMethod().when(executor, "httpErrorReply", any(), any(), any());
        doCallRealMethod().when(executor, "handleError", any(Throwable.class), any(DefRequest.class),
                any(HttpServletResponse.class), any(AtomicBoolean.class), any(CompletableFuture.class));

        AtomicBoolean responded = new AtomicBoolean(false);
        CompletableFuture<Void> completionFuture = new CompletableFuture<>();
        Whitebox.invokeMethod(executor, "invokeRpcRequest", invoker, defRequest, completionFuture, responded);

        HttpCodec httpCodec = Whitebox.getInternalState(executor, "httpCodec");
        assertTrue(responded.get());
        assertTrue(completionFuture.isCompletedExceptionally());
        verify(response).setStatus(HttpStatus.SC_SERVICE_UNAVAILABLE);
        verify(httpCodec).writeHttpResponse(any(HttpServletResponse.class), any());
    }

    @Test
    public void testInvokeRpcThrowsDirectly() throws Exception {
        HttpServletResponse response = mock(HttpServletResponse.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        DefRequest defRequest = mockDefRequest(request, response);

        ProviderConfig config = mockProviderConfig(0);
        ProviderInvoker<?> invoker = mock(ProviderInvoker.class);
        when(invoker.getConfig()).thenReturn(config);
        when(invoker.invoke(any())).thenThrow(new RuntimeException("boom-direct"));

        AbstractHttpExecutor executor = mockExecutorWithCodec();
        doReturn(response).when(executor, "getOriginalResponse", any());
        doReturn(request).when(executor, "getOriginalRequest", any());
        doCallRealMethod().when(executor, "invokeRpcRequest", any(), any(), any(), any());
        doCallRealMethod().when(executor, "httpErrorReply", any(), any(), any());
        doCallRealMethod().when(executor, "handleError", any(Throwable.class), any(DefRequest.class),
                any(HttpServletResponse.class), any(AtomicBoolean.class), any(CompletableFuture.class));

        AtomicBoolean responded = new AtomicBoolean(false);
        CompletableFuture<Void> completionFuture = new CompletableFuture<>();
        Whitebox.invokeMethod(executor, "invokeRpcRequest", invoker, defRequest, completionFuture, responded);

        HttpCodec httpCodec = Whitebox.getInternalState(executor, "httpCodec");
        assertTrue(responded.get());
        assertTrue(completionFuture.isCompletedExceptionally());
        verify(response).setStatus(HttpStatus.SC_SERVICE_UNAVAILABLE);
        verify(httpCodec).writeHttpResponse(any(HttpServletResponse.class), any());
    }
}