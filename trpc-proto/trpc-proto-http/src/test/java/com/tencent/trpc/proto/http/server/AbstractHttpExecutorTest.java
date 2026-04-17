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

package com.tencent.trpc.proto.http.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tencent.trpc.core.common.ConfigManager;
import com.tencent.trpc.core.common.config.ProviderConfig;
import com.tencent.trpc.core.exception.ErrorCode;
import com.tencent.trpc.core.exception.TRpcException;
import com.tencent.trpc.core.rpc.ProviderInvoker;
import com.tencent.trpc.core.rpc.RpcContext;
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
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class AbstractHttpExecutorTest {

    private static final String TEST_SERVICE = "trpc.demo.server";
    private static final String TEST_METHOD = "hello";
    private static final String TEST_IP = "127.0.0.1";
    private static final int TEST_PORT = 8080;

    @BeforeEach
    public void setUp() {
        ConfigManager.stopTest();
        ConfigManager.startTest();
    }

    @AfterEach
    public void tearDown() {
        ConfigManager.stopTest();
    }

    private HttpServletRequest mockRequest() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getAttribute(HttpConstants.REQUEST_ATTRIBUTE_TRPC_SERVICE)).thenReturn(TEST_SERVICE);
        when(request.getAttribute(HttpConstants.REQUEST_ATTRIBUTE_TRPC_METHOD)).thenReturn(TEST_METHOD);
        when(request.getRemoteAddr()).thenReturn(TEST_IP);
        when(request.getRemotePort()).thenReturn(TEST_PORT);
        when(request.getMethod()).thenReturn("POST");
        when(request.getRequestURI()).thenReturn("/api/test");
        when(request.getHeaderNames()).thenReturn(Collections.emptyEnumeration());
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

    private AbstractHttpExecutor createExecutorWithCodec() {
        AbstractHttpExecutor executor = new AbstractHttpExecutor() {
            @Override
            protected RpcMethodInfoAndInvoker getRpcMethodInfoAndInvoker(Object object) {
                return null;
            }
        };
        HttpCodec httpCodec = mock(HttpCodec.class);
        setField(executor, "httpCodec", httpCodec);
        return executor;
    }

    private AbstractHttpExecutor createExecutorWithInvoker(RpcMethodInfoAndInvoker methodInfoAndInvoker) {
        AbstractHttpExecutor executor = new AbstractHttpExecutor() {
            @Override
            protected RpcMethodInfoAndInvoker getRpcMethodInfoAndInvoker(Object object) {
                return methodInfoAndInvoker;
            }
        };
        HttpCodec httpCodec = mock(HttpCodec.class);
        setField(executor, "httpCodec", httpCodec);
        return executor;
    }

    private void setField(Object target, String fieldName, Object value) {
        try {
            Field field = AbstractHttpExecutor.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Object getField(Object target, String fieldName) {
        try {
            Field field = AbstractHttpExecutor.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(target);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Object invokePrivate(Object target, String methodName, Class<?>[] paramTypes, Object... args)
            throws Exception {
        Method method = AbstractHttpExecutor.class.getDeclaredMethod(methodName, paramTypes);
        method.setAccessible(true);
        try {
            return method.invoke(target, args);
        } catch (java.lang.reflect.InvocationTargetException e) {
            if (e.getCause() instanceof Exception) {
                throw (Exception) e.getCause();
            }
            throw e;
        }
    }

    private RpcMethodInfoAndInvoker buildMethodInfoAndInvoker(ProviderInvoker<?> invoker) throws Exception {
        Method method = TestService.class.getMethod("hello", RpcContext.class, String.class);
        RpcMethodInfo methodInfo = new RpcMethodInfo(TestService.class, method);
        RpcMethodInfoAndInvoker methodInfoAndInvoker = new RpcMethodInfoAndInvoker();
        methodInfoAndInvoker.setMethodInfo(methodInfo);
        methodInfoAndInvoker.setInvoker(invoker);
        return methodInfoAndInvoker;
    }

    // ==================== buildRpcInvocation ====================

    @Test
    public void testBuildRpcInvocation() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getAttribute(HttpConstants.REQUEST_ATTRIBUTE_TRPC_SERVICE)).thenReturn(TEST_SERVICE);
        when(request.getAttribute(HttpConstants.REQUEST_ATTRIBUTE_TRPC_METHOD)).thenReturn(TEST_METHOD);

        RpcMethodInfo methodInfo = mock(RpcMethodInfo.class);
        when(methodInfo.getParamsTypes()).thenReturn(new Type[]{RpcContext.class, String.class});

        AbstractHttpExecutor executor = createExecutorWithCodec();
        HttpCodec httpCodec = (HttpCodec) getField(executor, "httpCodec");
        when(httpCodec.convertToJavaBean(any(), any())).thenReturn("param");

        RpcInvocation invocation = (RpcInvocation) invokePrivate(executor, "buildRpcInvocation",
                new Class[]{HttpServletRequest.class, RpcMethodInfo.class}, request, methodInfo);

        assertEquals("/" + TEST_SERVICE + "/" + TEST_METHOD, invocation.getFunc());
    }

    // ==================== parseRpcParams ====================

    @Test
    public void testParseRpcParamsUnsupported() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        RpcMethodInfo methodInfo = mock(RpcMethodInfo.class);
        when(methodInfo.getParamsTypes()).thenReturn(new Type[]{String.class});

        AbstractHttpExecutor executor = createExecutorWithCodec();
        try {
            invokePrivate(executor, "parseRpcParams",
                    new Class[]{HttpServletRequest.class, RpcMethodInfo.class}, request, methodInfo);
        } catch (UnsupportedOperationException e) {
            assertNotNull(e);
        }
    }

    @Test
    public void testParseRpcParamsMap() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        RpcMethodInfo methodInfo = mock(RpcMethodInfo.class);
        when(methodInfo.getParamsTypes()).thenReturn(new Type[]{RpcContext.class, Map.class});

        AbstractHttpExecutor executor = createExecutorWithCodec();
        HttpCodec httpCodec = (HttpCodec) getField(executor, "httpCodec");
        Map<String, Object> mockMap = new HashMap<>();
        when(httpCodec.convertToJsonParam(any())).thenReturn(mockMap);

        Object[] result = (Object[]) invokePrivate(executor, "parseRpcParams",
                new Class[]{HttpServletRequest.class, RpcMethodInfo.class}, request, methodInfo);

        assertNotNull(result);
        assertEquals(mockMap, result[0]);
    }

    @Test
    public void testParseRpcParamsParameterized() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        RpcMethodInfo methodInfo = mock(RpcMethodInfo.class);
        ParameterizedType paramType = mock(ParameterizedType.class);
        when(methodInfo.getParamsTypes()).thenReturn(new Type[]{RpcContext.class, paramType});

        AbstractHttpExecutor executor = createExecutorWithCodec();
        HttpCodec httpCodec = (HttpCodec) getField(executor, "httpCodec");
        when(httpCodec.convertToParameterizedBean(any(), any())).thenReturn("paramResult");

        Object[] result = (Object[]) invokePrivate(executor, "parseRpcParams",
                new Class[]{HttpServletRequest.class, RpcMethodInfo.class}, request, methodInfo);

        assertNotNull(result);
        assertEquals("paramResult", result[0]);
    }

    // ==================== invokeRpcRequest ====================

    @Test
    public void testInvokeRpcSuccess() throws Exception {
        ProviderInvoker<?> invoker = mock(ProviderInvoker.class);
        DefResponse successResponse = new DefResponse();
        successResponse.setValue("success");
        CompletableFuture<Response> successFuture = CompletableFuture.completedFuture(successResponse);
        when(invoker.invoke(any())).thenReturn(successFuture);
        ProviderConfig config = mockProviderConfig(0);
        when(invoker.getConfig()).thenReturn(config);

        HttpServletRequest request = mockRequest();
        HttpServletResponse response = mock(HttpServletResponse.class);
        DefRequest defRequest = mockDefRequest(request, response);

        AbstractHttpExecutor executor = createExecutorWithCodec();
        AtomicBoolean responded = new AtomicBoolean(false);
        CompletableFuture<Void> completionFuture = new CompletableFuture<>();

        invokePrivate(executor, "invokeRpcRequest",
                new Class[]{ProviderInvoker.class, DefRequest.class, CompletableFuture.class, AtomicBoolean.class},
                invoker, defRequest, completionFuture, responded);

        completionFuture.get();
        verify(response).setStatus(HttpStatus.SC_OK);
        HttpCodec httpCodec = (HttpCodec) getField(executor, "httpCodec");
        verify(httpCodec).writeHttpResponse(response, successResponse);
        verify(response).flushBuffer();
    }

    @Test
    public void testInvokeRpcWorkerPoolNull() throws Exception {
        ProviderConfig config = mock(ProviderConfig.class);
        when(config.getWorkerPoolObj()).thenReturn(null);
        ProviderInvoker<?> invoker = mock(ProviderInvoker.class);
        when(invoker.getConfig()).thenReturn(config);

        HttpServletRequest request = mockRequest();
        HttpServletResponse response = mock(HttpServletResponse.class);
        DefRequest defRequest = mockDefRequest(request, response);

        AbstractHttpExecutor executor = createExecutorWithCodec();
        AtomicBoolean responded = new AtomicBoolean(false);
        CompletableFuture<Void> completionFuture = new CompletableFuture<>();

        invokePrivate(executor, "invokeRpcRequest",
                new Class[]{ProviderInvoker.class, DefRequest.class, CompletableFuture.class, AtomicBoolean.class},
                invoker, defRequest, completionFuture, responded);

        assertTrue(completionFuture.isCompletedExceptionally());
    }

    @Test
    public void testInvokeRpcAlreadyResponded() throws Exception {
        ProviderInvoker<?> invoker = mock(ProviderInvoker.class);
        DefResponse successResponse = new DefResponse();
        successResponse.setValue("success");
        CompletableFuture<Response> successFuture = CompletableFuture.completedFuture(successResponse);
        when(invoker.invoke(any())).thenReturn(successFuture);
        ProviderConfig config = mockProviderConfig(0);
        when(invoker.getConfig()).thenReturn(config);

        HttpServletRequest request = mockRequest();
        HttpServletResponse response = mock(HttpServletResponse.class);
        DefRequest defRequest = mockDefRequest(request, response);

        AbstractHttpExecutor executor = createExecutorWithCodec();
        AtomicBoolean responded = new AtomicBoolean(true);
        CompletableFuture<Void> completionFuture = new CompletableFuture<>();

        invokePrivate(executor, "invokeRpcRequest",
                new Class[]{ProviderInvoker.class, DefRequest.class, CompletableFuture.class, AtomicBoolean.class},
                invoker, defRequest, completionFuture, responded);

        // when responded=true, completionFuture won't complete, verify it's not done
        assertTrue(responded.get());
        assertTrue(!completionFuture.isDone());
    }

    @Test
    public void testInvokeRpcBusinessException() throws Exception {
        ProviderInvoker<?> invoker = mock(ProviderInvoker.class);
        DefResponse responseWithEx = new DefResponse();
        responseWithEx.setException(new RuntimeException("business error"));
        CompletableFuture<Response> future = CompletableFuture.completedFuture(responseWithEx);
        when(invoker.invoke(any())).thenReturn(future);
        ProviderConfig config = mockProviderConfig(0);
        when(invoker.getConfig()).thenReturn(config);

        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn(TEST_IP);
        when(request.getMethod()).thenReturn("POST");
        when(request.getRequestURI()).thenReturn("/api/test");
        HttpServletResponse response = mock(HttpServletResponse.class);
        DefRequest defRequest = mockDefRequest(request, response);

        AbstractHttpExecutor executor = createExecutorWithCodec();
        AtomicBoolean responded = new AtomicBoolean(false);
        CompletableFuture<Void> completionFuture = new CompletableFuture<>();

        invokePrivate(executor, "invokeRpcRequest",
                new Class[]{ProviderInvoker.class, DefRequest.class, CompletableFuture.class, AtomicBoolean.class},
                invoker, defRequest, completionFuture, responded);

        assertTrue(responded.get());
        assertTrue(completionFuture.isCompletedExceptionally());
        verify(response).setStatus(HttpStatus.SC_SERVICE_UNAVAILABLE);
    }

    @Test
    public void testInvokeRpcWithException() throws Exception {
        ProviderConfig config = mockProviderConfig(0);
        ProviderInvoker<?> invoker = mock(ProviderInvoker.class);
        when(invoker.getConfig()).thenReturn(config);
        CompletableFuture<Response> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new RuntimeException("boom"));
        when(invoker.invoke(any())).thenReturn(failedFuture);

        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn(TEST_IP);
        when(request.getMethod()).thenReturn("POST");
        when(request.getRequestURI()).thenReturn("/api/test");
        HttpServletResponse response = mock(HttpServletResponse.class);
        DefRequest defRequest = mockDefRequest(request, response);

        AbstractHttpExecutor executor = createExecutorWithCodec();
        AtomicBoolean responded = new AtomicBoolean(false);
        CompletableFuture<Void> completionFuture = new CompletableFuture<>();

        invokePrivate(executor, "invokeRpcRequest",
                new Class[]{ProviderInvoker.class, DefRequest.class, CompletableFuture.class, AtomicBoolean.class},
                invoker, defRequest, completionFuture, responded);

        assertTrue(responded.get());
        assertTrue(completionFuture.isCompletedExceptionally());
        verify(response).setStatus(HttpStatus.SC_SERVICE_UNAVAILABLE);
    }

    @Test
    public void testInvokeRpcThrowsDirectly() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn(TEST_IP);
        when(request.getMethod()).thenReturn("POST");
        when(request.getRequestURI()).thenReturn("/api/test");
        HttpServletResponse response = mock(HttpServletResponse.class);
        DefRequest defRequest = mockDefRequest(request, response);

        ProviderConfig config = mockProviderConfig(0);
        ProviderInvoker<?> invoker = mock(ProviderInvoker.class);
        when(invoker.getConfig()).thenReturn(config);
        when(invoker.invoke(any())).thenThrow(new RuntimeException("boom-direct"));

        AbstractHttpExecutor executor = createExecutorWithCodec();
        AtomicBoolean responded = new AtomicBoolean(false);
        CompletableFuture<Void> completionFuture = new CompletableFuture<>();

        invokePrivate(executor, "invokeRpcRequest",
                new Class[]{ProviderInvoker.class, DefRequest.class, CompletableFuture.class, AtomicBoolean.class},
                invoker, defRequest, completionFuture, responded);

        assertTrue(responded.get());
        assertTrue(completionFuture.isCompletedExceptionally());
        verify(response).setStatus(HttpStatus.SC_SERVICE_UNAVAILABLE);
    }

    // ==================== handleError ====================

    @Test
    public void testHandleError() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn(TEST_IP);
        when(request.getMethod()).thenReturn("POST");
        when(request.getRequestURI()).thenReturn("/api/test");
        when(request.getQueryString()).thenReturn("param=value");
        HttpServletResponse response = mock(HttpServletResponse.class);
        DefRequest defRequest = mockDefRequest(request, response);

        AbstractHttpExecutor executor = createExecutorWithCodec();
        AtomicBoolean responded = new AtomicBoolean(false);
        CompletableFuture<Void> completionFuture = new CompletableFuture<>();
        Throwable testException = new RuntimeException("Test error");

        invokePrivate(executor, "handleError",
                new Class[]{Throwable.class, DefRequest.class, HttpServletResponse.class,
                        AtomicBoolean.class, CompletableFuture.class},
                testException, defRequest, response, responded, completionFuture);

        assertTrue(responded.get());
        assertTrue(completionFuture.isCompletedExceptionally());
        verify(response).setStatus(HttpStatus.SC_SERVICE_UNAVAILABLE);
        HttpCodec httpCodec = (HttpCodec) getField(executor, "httpCodec");
        verify(httpCodec).writeHttpResponse(any(HttpServletResponse.class), any());
    }

    // ==================== doErrorReply ====================

    @Test
    public void testDoErrorReplyTimeout() throws Exception {
        HttpServletRequest request = mockRequest();
        HttpServletResponse response = mock(HttpServletResponse.class);

        AbstractHttpExecutor executor = createExecutorWithCodec();
        TRpcException ex = TRpcException.newFrameException(ErrorCode.TRPC_SERVER_TIMEOUT_ERR, "timeout");

        executor.doErrorReply(request, response, ex);

        verify(response).setStatus(HttpStatus.SC_REQUEST_TIMEOUT);
    }

    @Test
    public void testDoErrorReplyNotFound() throws Exception {
        HttpServletRequest request = mockRequest();
        HttpServletResponse response = mock(HttpServletResponse.class);

        AbstractHttpExecutor executor = createExecutorWithCodec();
        TRpcException ex = TRpcException.newFrameException(ErrorCode.TRPC_SERVER_NOSERVICE_ERR, "not found");

        executor.doErrorReply(request, response, ex);

        verify(response).setStatus(HttpStatus.SC_NOT_FOUND);
    }

    @Test
    public void testDoErrorReplyNoFunc() throws Exception {
        HttpServletRequest request = mockRequest();
        HttpServletResponse response = mock(HttpServletResponse.class);

        AbstractHttpExecutor executor = createExecutorWithCodec();
        TRpcException ex = TRpcException.newFrameException(ErrorCode.TRPC_SERVER_NOFUNC_ERR, "no func");

        executor.doErrorReply(request, response, ex);

        verify(response).setStatus(HttpStatus.SC_NOT_FOUND);
    }

    @Test
    public void testDoErrorReplyValidate() throws Exception {
        HttpServletRequest request = mockRequest();
        HttpServletResponse response = mock(HttpServletResponse.class);

        AbstractHttpExecutor executor = createExecutorWithCodec();
        TRpcException ex = TRpcException.newFrameException(ErrorCode.TRPC_SERVER_VALIDATE_ERR, "validate error");

        executor.doErrorReply(request, response, ex);

        verify(response).setStatus(HttpStatus.SC_BAD_REQUEST);
    }

    @Test
    public void testDoErrorReplyAuth() throws Exception {
        HttpServletRequest request = mockRequest();
        HttpServletResponse response = mock(HttpServletResponse.class);

        AbstractHttpExecutor executor = createExecutorWithCodec();
        TRpcException ex = TRpcException.newFrameException(ErrorCode.TRPC_SERVER_AUTH_ERR, "no auth");

        executor.doErrorReply(request, response, ex);

        verify(response).setStatus(HttpStatus.SC_UNAUTHORIZED);
    }

    @Test
    public void testDoErrorReplyOverload() throws Exception {
        HttpServletRequest request = mockRequest();
        HttpServletResponse response = mock(HttpServletResponse.class);

        AbstractHttpExecutor executor = createExecutorWithCodec();
        TRpcException ex = TRpcException.newFrameException(ErrorCode.TRPC_SERVER_OVERLOAD_ERR, "overload");

        executor.doErrorReply(request, response, ex);

        verify(response).setStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR);
    }

    @Test
    public void testDoErrorReplyEncode() throws Exception {
        HttpServletRequest request = mockRequest();
        HttpServletResponse response = mock(HttpServletResponse.class);

        AbstractHttpExecutor executor = createExecutorWithCodec();
        TRpcException ex = TRpcException.newFrameException(ErrorCode.TRPC_SERVER_ENCODE_ERR, "encode error");

        executor.doErrorReply(request, response, ex);

        verify(response).setStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR);
    }

    @Test
    public void testDoErrorReplySystem() throws Exception {
        HttpServletRequest request = mockRequest();
        HttpServletResponse response = mock(HttpServletResponse.class);

        AbstractHttpExecutor executor = createExecutorWithCodec();
        TRpcException ex = TRpcException.newFrameException(ErrorCode.TRPC_SERVER_SYSTEM_ERR, "system error");

        executor.doErrorReply(request, response, ex);

        verify(response).setStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR);
    }

    @Test
    public void testDoErrorReplyDefault() throws Exception {
        HttpServletRequest request = mockRequest();
        HttpServletResponse response = mock(HttpServletResponse.class);

        AbstractHttpExecutor executor = createExecutorWithCodec();

        executor.doErrorReply(request, response, new RuntimeException("unknown"));

        verify(response).setStatus(HttpStatus.SC_SERVICE_UNAVAILABLE);
    }

    @Test
    public void testHttpErrorReplyFlushException() throws Exception {
        HttpServletRequest request = mockRequest();
        HttpServletResponse response = mock(HttpServletResponse.class);
        doThrow(new IOException("flush error")).when(response).flushBuffer();

        AbstractHttpExecutor executor = createExecutorWithCodec();

        executor.doErrorReply(request, response, new RuntimeException("err"));

        verify(response).setStatus(HttpStatus.SC_SERVICE_UNAVAILABLE);
    }

    @Test
    public void testExecuteSuccess() throws Exception {
        ProviderInvoker<?> invoker = mock(ProviderInvoker.class);
        ProviderConfig config = mockProviderConfig(0);
        when(invoker.getConfig()).thenReturn(config);

        DefResponse successResponse = new DefResponse();
        successResponse.setValue("ok");
        when(invoker.invoke(any())).thenReturn(CompletableFuture.completedFuture(successResponse));

        HttpServletRequest request = mockRequest();
        HttpServletResponse response = mock(HttpServletResponse.class);

        RpcMethodInfoAndInvoker methodInfoAndInvoker = buildMethodInfoAndInvoker(invoker);
        AbstractHttpExecutor executor = createExecutorWithInvoker(methodInfoAndInvoker);
        HttpCodec httpCodec = (HttpCodec) getField(executor, "httpCodec");
        when(httpCodec.convertToJavaBean(any(), any())).thenReturn("param");

        executor.execute(request, response, methodInfoAndInvoker);

        verify(response).setStatus(HttpStatus.SC_OK);
    }

    @Test
    public void testExecuteWithTimeout() throws Exception {
        ProviderInvoker<?> invoker = mock(ProviderInvoker.class);
        ProviderConfig config = mock(ProviderConfig.class);
        when(config.getRequestTimeout()).thenReturn(50);
        WorkerPool workerPool = mock(WorkerPool.class);
        when(config.getWorkerPoolObj()).thenReturn(workerPool);
        when(invoker.getConfig()).thenReturn(config);

        CompletableFuture<Response> neverFuture = new CompletableFuture<>();
        when(invoker.invoke(any())).thenReturn(neverFuture);

        HttpServletRequest request = mockRequest();
        HttpServletResponse response = mock(HttpServletResponse.class);

        RpcMethodInfoAndInvoker methodInfoAndInvoker = buildMethodInfoAndInvoker(invoker);
        AbstractHttpExecutor executor = createExecutorWithInvoker(methodInfoAndInvoker);
        HttpCodec httpCodec = (HttpCodec) getField(executor, "httpCodec");
        when(httpCodec.convertToJavaBean(any(), any())).thenReturn("param");

        executor.execute(request, response, methodInfoAndInvoker);

        verify(response).setStatus(HttpStatus.SC_REQUEST_TIMEOUT);
    }

    @Test
    public void testExecuteWithCallerCallee() throws Exception {
        ProviderInvoker<?> invoker = mock(ProviderInvoker.class);
        ProviderConfig config = mockProviderConfig(0);
        when(invoker.getConfig()).thenReturn(config);

        DefResponse successResponse = new DefResponse();
        successResponse.setValue("ok");
        when(invoker.invoke(any())).thenReturn(CompletableFuture.completedFuture(successResponse));

        HttpServletRequest request = mockRequest();
        when(request.getHeader(HttpConstants.HTTP_HEADER_TRPC_CALLER)).thenReturn("trpc.app.server.service");
        when(request.getHeader(HttpConstants.HTTP_HEADER_TRPC_CALLEE)).thenReturn("trpc.app.server.service.method");
        when(request.getHeader(HttpConstants.HTTP_HEADER_TRPC_MESSAGE_TYPE)).thenReturn("1");
        when(request.getHeader(HttpConstants.HTTP_HEADER_TRPC_REQUEST_ID)).thenReturn("12345");
        when(request.getHeader(HttpConstants.HTTP_HEADER_TRPC_TIMEOUT)).thenReturn("3000");
        HttpServletResponse response = mock(HttpServletResponse.class);

        RpcMethodInfoAndInvoker methodInfoAndInvoker = buildMethodInfoAndInvoker(invoker);
        AbstractHttpExecutor executor = createExecutorWithInvoker(methodInfoAndInvoker);
        HttpCodec httpCodec = (HttpCodec) getField(executor, "httpCodec");
        when(httpCodec.convertToJavaBean(any(), any())).thenReturn("param");

        executor.execute(request, response, methodInfoAndInvoker);

        verify(response).setStatus(HttpStatus.SC_OK);
    }

    @Test
    public void testExecuteWithTransInfo() throws Exception {
        ProviderInvoker<?> invoker = mock(ProviderInvoker.class);
        ProviderConfig config = mockProviderConfig(0);
        when(invoker.getConfig()).thenReturn(config);

        DefResponse successResponse = new DefResponse();
        successResponse.setValue("ok");
        when(invoker.invoke(any())).thenReturn(CompletableFuture.completedFuture(successResponse));

        HttpServletRequest request = mockRequest();
        String transInfo = "{\"key\":\"dmFsdWU=\"}";
        when(request.getHeader(HttpConstants.HTTP_HEADER_TRPC_TRANS_INFO)).thenReturn(transInfo);
        HttpServletResponse response = mock(HttpServletResponse.class);

        RpcMethodInfoAndInvoker methodInfoAndInvoker = buildMethodInfoAndInvoker(invoker);
        AbstractHttpExecutor executor = createExecutorWithInvoker(methodInfoAndInvoker);
        HttpCodec httpCodec = (HttpCodec) getField(executor, "httpCodec");
        when(httpCodec.convertToJavaBean(any(), any())).thenReturn("param");

        executor.execute(request, response, methodInfoAndInvoker);

        verify(response).setStatus(HttpStatus.SC_OK);
    }

    private interface TestService {

        String hello(RpcContext ctx, String req);
    }
}