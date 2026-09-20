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

package com.tencent.trpc.springmvc;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import com.tencent.trpc.core.rpc.common.RpcMethodInfoAndInvoker;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;

/**
 * Test {@link TRpcServiceHandlerMapping#getHandlerInternal}, which logs the request path before routing. The log
 * message must not leave any unresolved placeholder, otherwise the log is polluted and the values are lost.
 */
public class TRpcServiceHandlerMappingTest {

    private static final String REQUEST_PATH = "/trpc.test.Greeter/sayHello";

    private TRpcServiceHandlerMapping handlerMapping;

    @Before
    public void before() {
        handlerMapping = new TRpcServiceHandlerMapping();
    }

    /**
     * A GET request goes through the routing, no route is registered so null is returned. The point of this case is
     * that the debug log of the request path is executed without any exception.
     */
    @Test
    public void testGetHandlerInternalWithGet() {
        MockHttpServletRequest request = new MockHttpServletRequest(
                TRpcHttpConstants.HTTP_METHOD_GET, REQUEST_PATH);
        request.setRequestURI(REQUEST_PATH);
        assertNull(handlerMapping.getHandlerInternal(request));
    }

    /**
     * A POST request also goes through the routing.
     */
    @Test
    public void testGetHandlerInternalWithPost() {
        MockHttpServletRequest request = new MockHttpServletRequest(
                TRpcHttpConstants.HTTP_METHOD_POST, REQUEST_PATH);
        request.setRequestURI(REQUEST_PATH);
        assertNull(handlerMapping.getHandlerInternal(request));
    }

    /**
     * A request path containing a percent sign is a valid path(a percent encoded path), and it must not break the
     * logging of the request path.
     */
    @Test
    public void testGetHandlerInternalWithPercentEncodedPath() {
        String path = "/trpc.test.Greeter/sayHello%2Fabc";
        MockHttpServletRequest request = new MockHttpServletRequest(
                TRpcHttpConstants.HTTP_METHOD_GET, path);
        request.setRequestURI(path);
        assertNull(handlerMapping.getHandlerInternal(request));
    }

    /**
     * Only GET and POST are supported, the other methods are rejected before the routing.
     */
    @Test
    public void testGetHandlerInternalWithUnsupportedMethod() {
        MockHttpServletRequest request = new MockHttpServletRequest("DELETE", REQUEST_PATH);
        request.setRequestURI(REQUEST_PATH);
        assertNull(handlerMapping.getHandlerInternal(request));
    }

    /**
     * When the path is not registered, the service and the method can also be taken from the request parameters.
     */
    @Test
    public void testGetHandlerInternalWithServiceAndMethodParameter() {
        MockHttpServletRequest request = new MockHttpServletRequest(
                TRpcHttpConstants.HTTP_METHOD_POST, REQUEST_PATH);
        request.setRequestURI(REQUEST_PATH);
        request.setParameter(TRpcHttpConstants.TRPC_PARAM_SERVICE, "trpc.test.Greeter");
        request.setParameter(TRpcHttpConstants.TRPC_PARAM_METHOD, "sayHello");
        RpcMethodInfoAndInvoker route = handlerMapping.getHandlerInternal(request);
        // nothing is registered, so no route is found, but both routing branches have been executed
        assertNull(route);
    }

    @Test
    public void testHandlerMappingOrder() {
        assertNotNull(handlerMapping);
        // the order is set in the constructor so that the tRPC mapping takes precedence
        org.junit.Assert.assertEquals(org.springframework.core.Ordered.HIGHEST_PRECEDENCE + 50000,
                handlerMapping.getOrder());
    }
}
