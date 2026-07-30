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

package com.tencent.trpc.proto.http.client;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tencent.trpc.core.common.ConfigManager;
import com.tencent.trpc.core.common.config.BackendConfig;
import com.tencent.trpc.core.common.config.ConsumerConfig;
import com.tencent.trpc.core.common.config.ProtocolConfig;
import com.tencent.trpc.core.rpc.Request;
import com.tencent.trpc.core.rpc.RequestMeta;
import com.tencent.trpc.core.rpc.Response;
import com.tencent.trpc.core.rpc.RpcInvocation;
import com.tencent.trpc.core.rpc.common.RpcMethodInfo;
import com.tencent.trpc.core.worker.spi.WorkerPool;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import org.apache.hc.client5.http.async.methods.SimpleHttpResponse;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpStatus;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for {@link Http2ConsumerInvoker}: the {@code send} error-handling branches and the
 * response-header / body parsing logic in {@code handleResponse}.
 */
public class Http2ConsumerInvokerTest {

    private Http2cRpcClient mockHttp2cRpcClient;
    private ConsumerConfig<TestService> mockConsumerConfig;
    private ProtocolConfig mockProtocolConfig;
    private BackendConfig mockBackendConfig;
    private WorkerPool mockWorkerPool;

    private Http2ConsumerInvoker<TestService> invoker;

    @Before
    public void setUp() {
        ConfigManager.stopTest();
        ConfigManager.startTest();

        mockHttp2cRpcClient = mock(Http2cRpcClient.class);
        mockConsumerConfig = mock(ConsumerConfig.class);
        mockProtocolConfig = mock(ProtocolConfig.class);
        mockBackendConfig = mock(BackendConfig.class);
        mockWorkerPool = mock(WorkerPool.class);

        when(mockConsumerConfig.getBackendConfig()).thenReturn(mockBackendConfig);
        when(mockBackendConfig.getWorkerPoolObj()).thenReturn(mockWorkerPool);
        when(mockProtocolConfig.getIp()).thenReturn("127.0.0.1");
        when(mockProtocolConfig.getPort()).thenReturn(8080);
        when(mockProtocolConfig.getExtMap()).thenReturn(new HashMap<>());

        invoker = new Http2ConsumerInvoker<>(mockHttp2cRpcClient, mockConsumerConfig, mockProtocolConfig);
    }

    @After
    public void tearDown() {
        AbstractConsumerInvoker.reset();
        ConfigManager.stopTest();
    }

    /**
     * Verifies that a failure while building the HTTP request (here: a non-numeric
     * {@code connection_request_timeout} makes {@code Integer.parseInt} throw inside
     * {@code buildRequest}) is caught by {@link Http2ConsumerInvoker#send(Request)} and surfaced
     * as a response carrying the exception rather than propagating out.
     */
    @Test
    public void testSendBuildRequestFailureReturnsErrorResponse() throws Exception {
        java.util.Map<String, Object> extMap = new HashMap<>();
        extMap.put(com.tencent.trpc.proto.http.common.HttpConstants.CONNECTION_REQUEST_TIMEOUT,
                "not-a-number");
        when(mockBackendConfig.getExtMap()).thenReturn(extMap);
        Request request = buildMockRequest();
        Response response = invoker.send(request);
        assertNotNull(response);
        assertNotNull(response.getException());
    }

    /**
     * Verifies that a failure during the actual request execution (here: the async client handle
     * is {@code null}, triggering an NPE inside {@code execute}) is caught by
     * {@link Http2ConsumerInvoker#send(Request)} and surfaced as a response carrying the exception.
     */
    @Test
    public void testSendExecuteFailureReturnsErrorResponse() throws Exception {
        // Let buildRequest succeed.
        when(mockBackendConfig.getExtMap()).thenReturn(new HashMap<>());
        when(mockBackendConfig.getConnTimeout()).thenReturn(1000);
        when(mockBackendConfig.getRequestTimeout()).thenReturn(1000);
        when(mockBackendConfig.getBasePath()).thenReturn("/");
        // getHttpAsyncClient() returns null (unstubbed) → NPE inside execute() → caught by send().
        Request request = buildMockRequest();
        Response response = invoker.send(request);
        assertNotNull(response);
        assertNotNull(response.getException());
    }

    /**
     * Verifies a simple header value is parsed completely and stored as {@code byte[]}.
     */
    @Test
    public void testHeaderValueParsedAndStoredAsByteArray() throws Exception {
        SimpleHttpResponse resp = SimpleHttpResponse.create(HttpStatus.SC_OK, "\"hello\"",
                ContentType.APPLICATION_JSON);
        resp.setHeader("X-Custom-Header", "simple-value");

        Request request = buildMockRequest();
        Response response = invokeHandleResponse(request, resp);

        assertNotNull(response);
        Object stored = response.getAttachments().get("X-Custom-Header");
        assertNotNull(stored);
        assertEquals(byte[].class, stored.getClass());
        assertArrayEquals("simple-value".getBytes(StandardCharsets.UTF_8), (byte[]) stored);
        assertEquals("hello", response.getValue());
    }

    /**
     * Verifies that a {@code Content-Length: 0} response yields an empty body.
     */
    @Test
    public void testZeroContentLengthReturnsEmptyResponse() throws Exception {
        SimpleHttpResponse resp = SimpleHttpResponse.create(HttpStatus.SC_OK);
        resp.setHeader(org.apache.hc.core5.http.HttpHeaders.CONTENT_LENGTH, "0");

        Request request = buildMockRequest();
        Response response = invokeHandleResponse(request, resp);

        assertNotNull(response);
        assertNull(response.getValue());
    }

    /**
     * Verifies that a non-200 status code raises a {@link com.tencent.trpc.core.exception.TRpcException}.
     */
    @Test(expected = com.tencent.trpc.core.exception.TRpcException.class)
    public void testNon200StatusCodeThrowsException() throws Exception {
        SimpleHttpResponse resp = SimpleHttpResponse.create(HttpStatus.SC_NOT_FOUND);
        Request request = buildMockRequest();
        invokeHandleResponse(request, resp);
    }

    /**
     * Drives the logging-only {@link org.apache.hc.core5.concurrent.FutureCallback} returned by
     * {@code newResponseCallback}: its {@code completed} / {@code failed} / {@code cancelled}
     * branches only emit logs and must never throw.
     */
    @Test
    public void testResponseCallbackLogsAllOutcomes() throws Exception {
        Request request = buildMockRequest();
        org.apache.hc.core5.concurrent.FutureCallback<SimpleHttpResponse> callback =
                invoker.newResponseCallback(request, 1000);
        callback.completed(SimpleHttpResponse.create(HttpStatus.SC_OK, "\"hi\"",
                ContentType.APPLICATION_JSON));
        callback.failed(new RuntimeException("boom"));
        callback.cancelled();
    }

    // ==================== Helper methods ====================

    /**
     * Builds a mock {@link Request} backed by a real {@link RpcInvocation} / {@link RpcMethodInfo}
     * whose return type is {@link String}.
     */
    private Request buildMockRequest() throws Exception {
        Request mockRequest = mock(Request.class);
        RequestMeta mockMeta = mock(RequestMeta.class);

        RpcInvocation invocation = new RpcInvocation();
        invocation.setFunc("/test");
        invocation.setRpcMethodName("testMethod");
        Method method = TestService.class.getMethod("testMethod", String.class);
        RpcMethodInfo methodInfo = new RpcMethodInfo(TestService.class, method);
        invocation.setRpcMethodInfo(methodInfo);

        when(mockRequest.getInvocation()).thenReturn(invocation);
        when(mockRequest.getMeta()).thenReturn(mockMeta);
        when(mockRequest.getAttachments()).thenReturn(new HashMap<>());

        return mockRequest;
    }

    /**
     * Invokes the private {@code handleResponse} method via reflection.
     */
    private Response invokeHandleResponse(Request request, SimpleHttpResponse httpResponse)
            throws Exception {
        Method handleResponseMethod = Http2ConsumerInvoker.class
                .getDeclaredMethod("handleResponse", Request.class, SimpleHttpResponse.class);
        handleResponseMethod.setAccessible(true);
        try {
            return (Response) handleResponseMethod.invoke(invoker, request, httpResponse);
        } catch (java.lang.reflect.InvocationTargetException e) {
            if (e.getCause() instanceof Exception) {
                throw (Exception) e.getCause();
            }
            throw e;
        }
    }

    /**
     * Stub service interface used only for constructing {@link RpcMethodInfo} in tests.
     */
    private interface TestService {

        String testMethod(String input);
    }
}
