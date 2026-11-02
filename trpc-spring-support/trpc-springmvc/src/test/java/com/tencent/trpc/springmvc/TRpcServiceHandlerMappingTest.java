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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.tencent.trpc.core.common.ConfigManager;
import com.tencent.trpc.core.common.config.ProviderConfig;
import com.tencent.trpc.core.common.config.ServiceConfig;
import com.tencent.trpc.core.rpc.RpcContext;
import com.tencent.trpc.core.rpc.anno.TRpcMethod;
import com.tencent.trpc.core.rpc.anno.TRpcService;
import com.tencent.trpc.core.rpc.common.RpcMethodInfoAndInvoker;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

public class TRpcServiceHandlerMappingTest {

    private static final String RPC_SERVICE_NAME = "trpc.HandlerMappingTest.Server.Greeter";

    private static final String RPC_METHOD_NAME = "hello";

    private static final String TRPC_SERVICE = "TRpcServiceHandlerMappingTest.trpc";

    private static final String REST_SERVICE = "TRpcServiceHandlerMappingTest.rest";

    private static final String BROKEN_REST_SERVICE = "TRpcServiceHandlerMappingTest.broken";

    private Map<String, ServiceConfig> serviceMap;

    private TRpcServiceHandlerMapping handlerMapping;

    @BeforeEach
    public void setUp() {
        serviceMap = ConfigManager.getInstance().getServerConfig().getServiceMap();

        // non-rest service is ignored
        ServiceConfig trpcService = new ServiceConfig();
        trpcService.setName(TRPC_SERVICE);
        trpcService.setProtocol("trpc");
        serviceMap.put(TRPC_SERVICE, trpcService);

        ServiceConfig restService = new ServiceConfig();
        restService.setName(REST_SERVICE);
        restService.setProtocol("rest");
        ProviderConfig<GreeterService> providerConfig = new ProviderConfig<>();
        providerConfig.setServiceInterface(GreeterService.class);
        providerConfig.setRef(new GreeterServiceImpl());
        providerConfig.setDisableDefaultFilter(Boolean.FALSE);
        providerConfig.setServiceConfig(restService);
        restService.addProviderConfig(providerConfig);
        serviceMap.put(REST_SERVICE, restService);

        // provider without service interface, export fails and is ignored
        ServiceConfig brokenRestService = new ServiceConfig();
        brokenRestService.setName(BROKEN_REST_SERVICE);
        brokenRestService.setProtocol("rest");
        brokenRestService.addProviderConfig(new ProviderConfig<>());
        serviceMap.put(BROKEN_REST_SERVICE, brokenRestService);

        handlerMapping = new TRpcServiceHandlerMapping();
        handlerMapping.exportTRpcServices();
    }

    @AfterEach
    public void tearDown() {
        serviceMap.remove(TRPC_SERVICE);
        serviceMap.remove(REST_SERVICE);
        serviceMap.remove(BROKEN_REST_SERVICE);
    }

    @Test
    public void testExportOnlyOnce() {
        // the second export is ignored, otherwise duplicate router key exception is logged
        handlerMapping.exportTRpcServices();
        assertNotNull(handlerMapping.getHandlerInternal(
                new MockHttpServletRequest("POST", "/" + RPC_SERVICE_NAME + "/" + RPC_METHOD_NAME)));
    }

    @Test
    public void testRouteByPath() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST",
                "/" + RPC_SERVICE_NAME + "/" + RPC_METHOD_NAME);
        RpcMethodInfoAndInvoker route = handlerMapping.getHandlerInternal(request);
        assertNotNull(route);
        assertEquals(RPC_SERVICE_NAME, request.getAttribute(TRpcHttpConstants.REQUEST_ATTRIBUTE_TRPC_SERVICE));
        assertEquals(RPC_METHOD_NAME, request.getAttribute(TRpcHttpConstants.REQUEST_ATTRIBUTE_TRPC_METHOD));
    }

    @Test
    public void testRouteByParameter() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/not/exist");
        request.setParameter(TRpcHttpConstants.TRPC_PARAM_SERVICE, RPC_SERVICE_NAME);
        request.setParameter(TRpcHttpConstants.TRPC_PARAM_METHOD, RPC_METHOD_NAME);
        assertNotNull(handlerMapping.getHandlerInternal(request));
    }

    @Test
    public void testRouteNotFound() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/not/exist");
        assertNull(handlerMapping.getHandlerInternal(request));
        assertNull(request.getAttribute(TRpcHttpConstants.REQUEST_ATTRIBUTE_TRPC_SERVICE));
    }

    @Test
    public void testUnsupportedHttpMethod() {
        MockHttpServletRequest request = new MockHttpServletRequest("PUT",
                "/" + RPC_SERVICE_NAME + "/" + RPC_METHOD_NAME);
        assertNull(handlerMapping.getHandlerInternal(request));
    }

    @TRpcService(name = RPC_SERVICE_NAME)
    public interface GreeterService {

        @TRpcMethod(name = RPC_METHOD_NAME)
        String hello(RpcContext context, String request);
    }

    public static class GreeterServiceImpl implements GreeterService {

        @Override
        public String hello(RpcContext context, String request) {
            return request;
        }
    }
}
