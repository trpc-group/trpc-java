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
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.powermock.api.mockito.PowerMockito.doReturn;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

import com.tencent.trpc.core.rpc.RpcInvocation;
import com.tencent.trpc.core.rpc.common.RpcMethodInfo;
import com.tencent.trpc.core.rpc.def.DefRequest;
import com.tencent.trpc.proto.http.common.ErrorResponse;
import com.tencent.trpc.proto.http.common.HttpConstants;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
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
    public void handleError_shouldCallHttpErrorReply_whenRespondedIsFalse() throws Exception {
        // Arrange
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        DefRequest rpcRequest = mock(DefRequest.class);
        AtomicBoolean responded = new AtomicBoolean(false);
        CompletableFuture<Void> completionFuture = new CompletableFuture<>();
        Throwable throwable = new RuntimeException("test error");

        AbstractHttpExecutor abstractHttpExecutor = mock(AbstractHttpExecutor.class);

        // Mock getOriginalRequest method
        Map<String, Object> attachments = new HashMap<>();
        attachments.put(HttpConstants.TRPC_ATTACH_SERVLET_REQUEST, request);
        when(rpcRequest.getAttachments()).thenReturn(attachments);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        // Mock httpErrorReply method
        PowerMockito.doNothing().when(abstractHttpExecutor, "httpErrorReply", any(HttpServletRequest.class),
                any(HttpServletResponse.class), any(ErrorResponse.class));

        // Call real method
        when(abstractHttpExecutor, "handleError", throwable, rpcRequest, response, responded, completionFuture)
                .thenCallRealMethod();

        // Act
        Whitebox.invokeMethod(abstractHttpExecutor, "handleError", throwable, rpcRequest, response,
                responded, completionFuture);

        // Assert
        assertTrue("responded should be set to true", responded.get());
        assertTrue("completionFuture should be completed exceptionally", completionFuture.isCompletedExceptionally());

        // Verify httpErrorReply was called
        PowerMockito.verifyPrivate(abstractHttpExecutor, times(1))
                .invoke("httpErrorReply", eq(request), eq(response), any(ErrorResponse.class));
    }

    @Test
    public void handleError_shouldNotCallHttpErrorReply_whenRespondedIsTrue() throws Exception {
        // Arrange
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        DefRequest rpcRequest = mock(DefRequest.class);
        AtomicBoolean responded = new AtomicBoolean(true); // Already responded
        CompletableFuture<Void> completionFuture = new CompletableFuture<>();
        Throwable throwable = new RuntimeException("test error");

        AbstractHttpExecutor abstractHttpExecutor = mock(AbstractHttpExecutor.class);

        // Mock httpErrorReply method
        PowerMockito.doNothing().when(abstractHttpExecutor, "httpErrorReply", any(HttpServletRequest.class),
                any(HttpServletResponse.class), any(ErrorResponse.class));

        // Call real method
        when(abstractHttpExecutor, "handleError", throwable, rpcRequest, response, responded, completionFuture)
                .thenCallRealMethod();

        // Act
        Whitebox.invokeMethod(abstractHttpExecutor, "handleError", throwable, rpcRequest, response,
                responded, completionFuture);

        // Assert
        assertTrue("responded should remain true", responded.get());
        assertTrue("completionFuture should be completed exceptionally", completionFuture.isCompletedExceptionally());

        // Verify httpErrorReply was NOT called
        PowerMockito.verifyPrivate(abstractHttpExecutor, never())
                .invoke("httpErrorReply", any(HttpServletRequest.class), any(HttpServletResponse.class),
                        any(ErrorResponse.class));
    }

    @Test
    public void handleError_shouldCompleteExceptionally_evenWhenHttpErrorReplyThrows() throws Exception {
        // Arrange
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        DefRequest rpcRequest = mock(DefRequest.class);
        AtomicBoolean responded = new AtomicBoolean(false);
        CompletableFuture<Void> completionFuture = new CompletableFuture<>();
        Throwable throwable = new RuntimeException("test error");

        AbstractHttpExecutor abstractHttpExecutor = mock(AbstractHttpExecutor.class);

        // Mock getOriginalRequest method
        Map<String, Object> attachments = new HashMap<>();
        attachments.put(HttpConstants.TRPC_ATTACH_SERVLET_REQUEST, request);
        when(rpcRequest.getAttachments()).thenReturn(attachments);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        // Mock httpErrorReply method to throw exception
        PowerMockito.doThrow(new IOException("response error")).when(abstractHttpExecutor, "httpErrorReply",
                any(HttpServletRequest.class), any(HttpServletResponse.class), any(ErrorResponse.class));

        // Call real method
        when(abstractHttpExecutor, "handleError", throwable, rpcRequest, response, responded, completionFuture)
                .thenCallRealMethod();

        Whitebox.invokeMethod(abstractHttpExecutor, "handleError", throwable, rpcRequest, response,
                responded, completionFuture);

        assertTrue("responded should be set to true", responded.get());
        assertTrue("completionFuture should be completed exceptionally", completionFuture.isCompletedExceptionally());

        try {
            completionFuture.get();
            fail("Should have thrown exception");
        } catch (ExecutionException e) {
            assertEquals("Should complete with original throwable", throwable, e.getCause());
        }
    }

}