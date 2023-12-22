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

package com.tencent.trpc.proto.http.common;

import static com.tencent.trpc.proto.http.common.HttpConstants.HTTP_SCHEME;
import static com.tencent.trpc.transport.http.common.Constants.HTTP2_SCHEME;

import com.tencent.trpc.core.common.ConfigManager;
import com.tencent.trpc.core.common.config.BackendConfig;
import com.tencent.trpc.core.common.config.ConsumerConfig;
import com.tencent.trpc.core.common.config.ProviderConfig;
import com.tencent.trpc.core.common.config.ServerConfig;
import com.tencent.trpc.core.common.config.ServiceConfig;
import com.tencent.trpc.core.rpc.RpcClientContext;
import com.tencent.trpc.core.utils.NetUtils;
import java.util.HashMap;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import tests.service.GreeterService;
import tests.service.HelloRequestProtocol.HelloRequest;
import tests.service.HelloRequestProtocol.HelloResponse;
import tests.service.impl1.GreeterServiceImpl2;

public class HttpTransparentInfoTest {

    private static final String TEST_MESSAGE = "tRPC-Java!";

    private static ServerConfig serverConfig;

    private static int TRPC_JAVA_TEST_HTTP2_PORT;

    private static int TRPC_JAVA_TEST_HTTP_PORT;

    private static final String TRANSPARENT_TRANSMISSION_KEY = "name";
    private static final String TRANSPARENT_TRANSMISSION_VALUE = "zhangsan";

    private static final String TRANSPARENT_TRANSMISSION_RES_KEY = "code";

    private static final String TRANSPARENT_TRANSMISSION_RES_VALUE = "2012";

    @BeforeClass
    public static void startHttpServer() {
        ConfigManager.stopTest();
        ConfigManager.startTest();

        ProviderConfig<GreeterService> gspc = new ProviderConfig<>();
        gspc.setServiceInterface(GreeterService.class);
        gspc.setRef(new GreeterServiceImpl2());
        HashMap<String, ServiceConfig> providers = new HashMap<>();

        TRPC_JAVA_TEST_HTTP2_PORT = NetUtils.getAvailablePort();
        ServiceConfig serviceConfig1 = getServiceConfig(gspc, "trpc.java.test.http2",
                NetUtils.LOCAL_HOST, TRPC_JAVA_TEST_HTTP2_PORT, HTTP2_SCHEME, "jetty");
        providers.put(serviceConfig1.getName(), serviceConfig1);

        TRPC_JAVA_TEST_HTTP_PORT = NetUtils.getAvailablePort();
        ServiceConfig serviceConfig2 = getServiceConfig(gspc, "trpc.java.test.http",
                NetUtils.LOCAL_HOST, TRPC_JAVA_TEST_HTTP_PORT, HTTP_SCHEME, "jetty");
        providers.put(serviceConfig2.getName(), serviceConfig2);

        ServerConfig sc = new ServerConfig();
        sc.setServiceMap(providers);
        sc.setApp("http-test-app");
        sc.setLocalIp("127.0.0.1");
        sc.init();

        serverConfig = sc;
    }

    private static ServiceConfig getServiceConfig(ProviderConfig gspc, String name,
            String ip, int port, String protocol, String transport) {
        ServiceConfig serviceConfig = new ServiceConfig();
        serviceConfig.setName(name);
        serviceConfig.getProviderConfigs().add(gspc);
        serviceConfig.setIp(ip);
        serviceConfig.setPort(port);
        serviceConfig.setProtocol(protocol);
        serviceConfig.setTransporter(transport);
        return serviceConfig;
    }

    @AfterClass
    public static void stopHttpServer() {
        ConfigManager.stopTest();
        if (serverConfig != null) {
            serverConfig.stop();
            serverConfig = null;
        }
    }

    private static HelloRequest createPbRequest(String msg) {
        HelloRequest.Builder builder = HelloRequest.newBuilder();
        builder.setMessage(msg);
        return builder.build();
    }

    @Test
    public void testHttp2RpcClient() {
        BackendConfig backendConfig = new BackendConfig();
        ConsumerConfig<GreeterService> consumerConfig = new ConsumerConfig<>();
        backendConfig.setName("serviceId");
        backendConfig.setRequestTimeout(10000);
        consumerConfig.setServiceInterface(GreeterService.class);
        consumerConfig.setBackendConfig(backendConfig);
        backendConfig.setNamingUrl("ip://127.0.0.1:" + TRPC_JAVA_TEST_HTTP2_PORT);
        backendConfig.setKeepAlive(false);
        backendConfig.setConnsPerAddr(4);
        backendConfig.setProtocol(HTTP2_SCHEME);
        try {
            GreeterService proxy = consumerConfig.getProxy();

            RpcClientContext context = new RpcClientContext();
            // client tran info to server
            context.getReqAttachMap().put(TRANSPARENT_TRANSMISSION_KEY, TRANSPARENT_TRANSMISSION_VALUE);

            HelloResponse helloResponse = proxy.sayHello(context, createPbRequest(TEST_MESSAGE));
            // get server tran info
            String code = (String) context.getRspAttachMap().get(TRANSPARENT_TRANSMISSION_RES_KEY);
            Assert.assertEquals(code, TRANSPARENT_TRANSMISSION_RES_VALUE);
            Assert.assertNotNull(helloResponse);
            String rspMessage = helloResponse.getMessage();
            Assert.assertEquals(rspMessage, "hello " + TRANSPARENT_TRANSMISSION_VALUE);
        } finally {
            backendConfig.stop();
        }
    }

    @Test
    public void testHttpRpcClient() {
        BackendConfig backendConfig = new BackendConfig();
        ConsumerConfig<GreeterService> consumerConfig = new ConsumerConfig<>();
        backendConfig.setName("serviceId");
        backendConfig.setRequestTimeout(10000);
        consumerConfig.setServiceInterface(GreeterService.class);
        consumerConfig.setBackendConfig(backendConfig);
        backendConfig.setNamingUrl("ip://127.0.0.1:" + TRPC_JAVA_TEST_HTTP_PORT);
        backendConfig.setKeepAlive(false);
        backendConfig.setConnsPerAddr(4);
        backendConfig.setProtocol(HTTP_SCHEME);
        try {
            GreeterService proxy = consumerConfig.getProxy();

            RpcClientContext context = new RpcClientContext();
            context.getReqAttachMap().put(TRANSPARENT_TRANSMISSION_KEY, TRANSPARENT_TRANSMISSION_VALUE);

            HelloResponse helloResponse = proxy.sayHello(context, createPbRequest(TEST_MESSAGE));
            String code = (String) context.getRspAttachMap().get(TRANSPARENT_TRANSMISSION_RES_KEY);
            Assert.assertEquals(code, TRANSPARENT_TRANSMISSION_RES_VALUE);
            Assert.assertNotNull(helloResponse);
            String rspMessage = helloResponse.getMessage();
            Assert.assertEquals(rspMessage, "hello " + TRANSPARENT_TRANSMISSION_VALUE);
        } finally {
            backendConfig.stop();
        }
    }
}
