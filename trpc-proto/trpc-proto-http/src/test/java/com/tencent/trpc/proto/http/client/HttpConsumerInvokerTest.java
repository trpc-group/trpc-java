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
import com.tencent.trpc.core.rpc.CallInfo;
import com.tencent.trpc.core.rpc.Request;
import com.tencent.trpc.core.rpc.RequestMeta;
import com.tencent.trpc.core.rpc.Response;
import com.tencent.trpc.core.rpc.RpcInvocation;
import com.tencent.trpc.core.rpc.common.RpcMethodInfo;
import com.tencent.trpc.core.worker.spi.WorkerPool;
import java.io.ByteArrayInputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import org.apache.http.Header;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicStatusLine;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests the response header parsing logic in {@link HttpConsumerInvoker}.
 * Verifies that {@code header.getValue()} returns the complete header value instead of
 * the truncated result previously produced by {@code HeaderElement.getName()}.
 */
public class HttpConsumerInvokerTest {

    private HttpRpcClient mockHttpRpcClient;
    private ConsumerConfig<TestService> mockConsumerConfig;
    private ProtocolConfig mockProtocolConfig;
    private BackendConfig mockBackendConfig;
    private WorkerPool mockWorkerPool;

    private HttpConsumerInvoker<TestService> invoker;

    @Before
    public void setUp() {
        ConfigManager.stopTest();
        ConfigManager.startTest();

        mockHttpRpcClient = mock(HttpRpcClient.class);
        mockConsumerConfig = mock(ConsumerConfig.class);
        mockProtocolConfig = mock(ProtocolConfig.class);
        mockBackendConfig = mock(BackendConfig.class);
        mockWorkerPool = mock(WorkerPool.class);

        when(mockConsumerConfig.getBackendConfig()).thenReturn(mockBackendConfig);
        when(mockBackendConfig.getWorkerPoolObj()).thenReturn(mockWorkerPool);
        when(mockProtocolConfig.getIp()).thenReturn("127.0.0.1");
        when(mockProtocolConfig.getPort()).thenReturn(8080);
        when(mockProtocolConfig.getExtMap()).thenReturn(new HashMap<>());

        invoker = new HttpConsumerInvoker<>(mockHttpRpcClient, mockConsumerConfig, mockProtocolConfig);
    }

    @After
    public void tearDown() {
        AbstractConsumerInvoker.reset();
        ConfigManager.stopTest();
    }

    /**
     * Verifies that a simple header value (no delimiters) is parsed correctly.
     * e.g. X-Custom-Header: simple-value
     */
    @Test
    public void testSimpleHeaderValueParsedCorrectly() throws Exception {
        CloseableHttpResponse mockResponse = buildMockResponse(
                HttpStatus.SC_OK,
                new Header[]{
                        new BasicHeader("X-Custom-Header", "simple-value"),
                        new BasicHeader(HttpHeaders.CONTENT_LENGTH, "0")
                },
                null
        );

        Request mockRequest = buildMockRequest();

        Response response = invokeHandleResponse(mockRequest, mockResponse);

        assertNotNull(response);
        assertNotNull(response.getAttachments());
        byte[] headerValue = (byte[]) response.getAttachments().get("X-Custom-Header");
        assertNotNull(headerValue);
        assertEquals("simple-value", new String(headerValue, StandardCharsets.UTF_8));
    }

    /**
     * Verifies that a composite header value containing semicolons is parsed completely
     * (this is the core fix scenario). The old {@code HeaderElement.getName()} only returned
     * the token before the first {@code ;} (e.g. {@code "application/json"}), whereas
     * {@code header.getValue()} returns the full value
     * (e.g. {@code "application/json; charset=utf-8"}).
     */
    @Test
    public void testComplexHeaderWithSemicolonParsedCompletely() throws Exception {
        String fullContentType = "application/json; charset=utf-8";
        CloseableHttpResponse mockResponse = buildMockResponse(
                HttpStatus.SC_OK,
                new Header[]{
                        new BasicHeader(HttpHeaders.CONTENT_TYPE, fullContentType),
                        new BasicHeader(HttpHeaders.CONTENT_LENGTH, "0")
                },
                null
        );

        Request mockRequest = buildMockRequest();

        Response response = invokeHandleResponse(mockRequest, mockResponse);

        assertNotNull(response);
        byte[] contentTypeValue = (byte[]) response.getAttachments().get(HttpHeaders.CONTENT_TYPE);
        assertNotNull(contentTypeValue);
        // After the fix, the full value should be returned, not just "application/json"
        assertEquals(fullContentType, new String(contentTypeValue, StandardCharsets.UTF_8));
    }

    /**
     * Verifies that a header value containing an equals sign is parsed completely.
     * The old {@code HeaderElement.getName()} also truncated values with {@code =},
     * whereas {@code header.getValue()} returns the full value.
     * e.g. X-Token: key=abc123
     */
    @Test
    public void testHeaderWithEqualSignParsedCompletely() throws Exception {
        String tokenValue = "key=abc123";
        CloseableHttpResponse mockResponse = buildMockResponse(
                HttpStatus.SC_OK,
                new Header[]{
                        new BasicHeader("X-Token", tokenValue),
                        new BasicHeader(HttpHeaders.CONTENT_LENGTH, "0")
                },
                null
        );

        Request mockRequest = buildMockRequest();

        Response response = invokeHandleResponse(mockRequest, mockResponse);

        assertNotNull(response);
        byte[] tokenBytes = (byte[]) response.getAttachments().get("X-Token");
        assertNotNull(tokenBytes);
        assertEquals(tokenValue, new String(tokenBytes, StandardCharsets.UTF_8));
    }

    /**
     * Verifies that multiple response headers are all parsed correctly and stored in attachments.
     */
    @Test
    public void testMultipleHeadersAllParsedCorrectly() throws Exception {
        CloseableHttpResponse mockResponse = buildMockResponse(
                HttpStatus.SC_OK,
                new Header[]{
                        new BasicHeader("X-Trace-Id", "trace-abc-123"),
                        new BasicHeader("X-Caller", "service-a"),
                        new BasicHeader("X-Callee", "service-b"),
                        new BasicHeader(HttpHeaders.CONTENT_LENGTH, "0")
                },
                null
        );

        Request mockRequest = buildMockRequest();

        Response response = invokeHandleResponse(mockRequest, mockResponse);

        assertNotNull(response);
        assertEquals("trace-abc-123",
                new String((byte[]) response.getAttachments().get("X-Trace-Id"), StandardCharsets.UTF_8));
        assertEquals("service-a",
                new String((byte[]) response.getAttachments().get("X-Caller"), StandardCharsets.UTF_8));
        assertEquals("service-b",
                new String((byte[]) response.getAttachments().get("X-Callee"), StandardCharsets.UTF_8));
    }

    /**
     * Verifies that header values are stored as {@code byte[]} to maintain consistency
     * with the tRPC protocol.
     */
    @Test
    public void testHeaderValueStoredAsByteArray() throws Exception {
        String expectedValue = "test-value";
        CloseableHttpResponse mockResponse = buildMockResponse(
                HttpStatus.SC_OK,
                new Header[]{
                        new BasicHeader("X-Test", expectedValue),
                        new BasicHeader(HttpHeaders.CONTENT_LENGTH, "0")
                },
                null
        );

        Request mockRequest = buildMockRequest();

        Response response = invokeHandleResponse(mockRequest, mockResponse);

        assertNotNull(response);
        Object storedValue = response.getAttachments().get("X-Test");
        // Verify that the stored type is byte[]
        assertNotNull(storedValue);
        assertEquals(byte[].class, storedValue.getClass());
        assertArrayEquals(expectedValue.getBytes(StandardCharsets.UTF_8), (byte[]) storedValue);
    }

    /**
     * Verifies that a non-200 HTTP status code causes a {@link com.tencent.trpc.core.exception.TRpcException}
     * to be thrown.
     */
    @Test(expected = com.tencent.trpc.core.exception.TRpcException.class)
    public void testNon200StatusCodeThrowsException() throws Exception {
        CloseableHttpResponse mockResponse = buildMockResponse(
                HttpStatus.SC_NOT_FOUND,
                new Header[]{},
                null
        );

        Request mockRequest = buildMockRequest();
        invokeHandleResponse(mockRequest, mockResponse);
    }

    /**
     * Verifies that a {@code Content-Length: 0} response returns an empty response body.
     */
    @Test
    public void testZeroContentLengthReturnsEmptyResponse() throws Exception {
        CloseableHttpResponse mockResponse = buildMockResponse(
                HttpStatus.SC_OK,
                new Header[]{
                        new BasicHeader(HttpHeaders.CONTENT_LENGTH, "0")
                },
                null
        );

        Request mockRequest = buildMockRequest();

        Response response = invokeHandleResponse(mockRequest, mockResponse);

        assertNotNull(response);
        assertNull(response.getValue());
    }

    /**
     * Verifies that a complex header value with multiple semicolons and equals signs
     * is parsed completely.
     * e.g. Set-Cookie: sessionId=abc; Path=/; HttpOnly
     */
    @Test
    public void testComplexCookieHeaderParsedCompletely() throws Exception {
        String cookieValue = "sessionId=abc; Path=/; HttpOnly";
        CloseableHttpResponse mockResponse = buildMockResponse(
                HttpStatus.SC_OK,
                new Header[]{
                        new BasicHeader("Set-Cookie", cookieValue),
                        new BasicHeader(HttpHeaders.CONTENT_LENGTH, "0")
                },
                null
        );

        Request mockRequest = buildMockRequest();

        Response response = invokeHandleResponse(mockRequest, mockResponse);

        assertNotNull(response);
        byte[] cookieBytes = (byte[]) response.getAttachments().get("Set-Cookie");
        assertNotNull(cookieBytes);
        // After the fix, the full cookie value should be returned, not just "sessionId"
        assertEquals(cookieValue, new String(cookieBytes, StandardCharsets.UTF_8));
    }

    /**
     * Verifies that the response body is decoded correctly when Content-Length is non-zero.
     */
    @Test
    public void testResponseBodyParsedWhenContentLengthNonZero() throws Exception {
        String jsonBody = "\"hello\"";
        CloseableHttpResponse mockResponse = buildMockResponse(
                HttpStatus.SC_OK,
                new Header[]{
                        new BasicHeader("X-Custom", "custom-value"),
                        new BasicHeader(HttpHeaders.CONTENT_LENGTH, String.valueOf(jsonBody.length()))
                },
                jsonBody
        );

        Request mockRequest = buildMockRequest();

        Response response = invokeHandleResponse(mockRequest, mockResponse);

        assertNotNull(response);
        // Verify that response headers are also parsed correctly
        byte[] customValue = (byte[]) response.getAttachments().get("X-Custom");
        assertNotNull(customValue);
        assertEquals("custom-value", new String(customValue, StandardCharsets.UTF_8));
        // Verify that the response body is decoded correctly
        assertEquals("hello", response.getValue());
    }

    // ==================== Helper methods ====================

    /**
     * Builds a mock HTTP response with the given status code, headers, and optional body.
     */
    private CloseableHttpResponse buildMockResponse(int statusCode, Header[] headers, String body) {
        CloseableHttpResponse mockResponse = mock(CloseableHttpResponse.class);
        StatusLine statusLine = new BasicStatusLine(HttpVersion.HTTP_1_1, statusCode,
                statusCode == HttpStatus.SC_OK ? "OK" : "Not Found");
        when(mockResponse.getStatusLine()).thenReturn(statusLine);
        when(mockResponse.getAllHeaders()).thenReturn(headers);

        // Wire up Content-Length header lookup
        for (Header header : headers) {
            if (HttpHeaders.CONTENT_LENGTH.equalsIgnoreCase(header.getName())) {
                when(mockResponse.getFirstHeader(HttpHeaders.CONTENT_LENGTH)).thenReturn(header);
            }
        }

        if (body != null) {
            BasicHttpEntity entity = new BasicHttpEntity();
            entity.setContent(new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8)));
            when(mockResponse.getEntity()).thenReturn(entity);
        }

        return mockResponse;
    }

    /**
     * Builds a mock {@link Request} backed by a real {@link RpcMethodInfo} whose return type is
     * {@link String}.
     */
    private Request buildMockRequest() throws Exception {
        Request mockRequest = mock(Request.class);
        RequestMeta mockMeta = mock(RequestMeta.class);
        CallInfo mockCallInfo = mock(CallInfo.class);

        // Build real RpcInvocation and RpcMethodInfo instances
        RpcInvocation invocation = new RpcInvocation();
        Method method = TestService.class.getMethod("testMethod", String.class);
        RpcMethodInfo methodInfo = new RpcMethodInfo(TestService.class, method);
        invocation.setRpcMethodInfo(methodInfo);

        when(mockRequest.getInvocation()).thenReturn(invocation);
        when(mockRequest.getMeta()).thenReturn(mockMeta);
        when(mockMeta.getCallInfo()).thenReturn(mockCallInfo);
        when(mockCallInfo.getCaller()).thenReturn("test-caller");
        when(mockCallInfo.getCallee()).thenReturn("test-callee");
        when(mockRequest.getAttachments()).thenReturn(new HashMap<>());

        return mockRequest;
    }

    /**
     * Invokes the private {@code handleResponse} method via reflection.
     */
    private Response invokeHandleResponse(Request request, CloseableHttpResponse httpResponse)
            throws Exception {
        Method handleResponseMethod = HttpConsumerInvoker.class
                .getDeclaredMethod("handleResponse", Request.class, CloseableHttpResponse.class);
        handleResponseMethod.setAccessible(true);
        try {
            return (Response) handleResponseMethod.invoke(invoker, request, httpResponse);
        } catch (java.lang.reflect.InvocationTargetException e) {
            // Unwrap and rethrow so that @Test(expected=...) can catch the original exception
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
