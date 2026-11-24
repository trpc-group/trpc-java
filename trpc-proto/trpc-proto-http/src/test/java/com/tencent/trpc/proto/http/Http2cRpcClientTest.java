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

package com.tencent.trpc.proto.http;

import static com.tencent.trpc.transport.http.common.Constants.HTTP2_SCHEME;

import com.tencent.trpc.core.common.ConfigManager;
import com.tencent.trpc.core.common.config.BackendConfig;
import com.tencent.trpc.core.common.config.ConsumerConfig;
import com.tencent.trpc.core.common.config.ProviderConfig;
import com.tencent.trpc.core.common.config.ServerConfig;
import com.tencent.trpc.core.common.config.ServiceConfig;
import com.tencent.trpc.core.exception.TRpcException;
import com.tencent.trpc.core.logger.Logger;
import com.tencent.trpc.core.logger.LoggerFactory;
import com.tencent.trpc.core.rpc.RpcClientContext;
import com.tencent.trpc.core.utils.NetUtils;
import java.util.HashMap;
import java.util.Map;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import tests.service.GreeterJsonService;
import tests.service.GreeterService;
import tests.service.HelloRequestProtocol.HelloRequest;
import tests.service.HelloRequestProtocol.HelloResponse;
import tests.service.impl1.GreeterJsonServiceImpl1;
import tests.service.impl1.GreeterServiceImpl1;

public class Http2cRpcClientTest {

    private static final Logger logger = LoggerFactory.getLogger(HttpRpcServerTest.class);

    private static final String CST_BASE_PATH1 = "/";
    private static final String CST_BASE_PATH2 = "/test-base-path";
    private static final String TEST_MESSAGE = "tRPC-Java!";

    private static ServerConfig serverConfig;

    @BeforeClass
    public static void startHttpServer() {
        ConfigManager.stopTest();
        ConfigManager.startTest();

        ProviderConfig<GreeterService> gspc = new ProviderConfig<>();
        gspc.setServiceInterface(GreeterService.class);
        gspc.setRef(new GreeterServiceImpl1());
        // gspc.setProtocolIds(Collections.singletonList("http"));

        ProviderConfig<GreeterJsonService> gjspc = new ProviderConfig<>();
        gjspc.setServiceInterface(GreeterJsonService.class);
        gjspc.setRef(new GreeterJsonServiceImpl1());
        // gjspc.setProtocolIds(Collections.singletonList("http"));

        HashMap<String, ServiceConfig> providers = new HashMap<>();

        ServiceConfig serviceConfig1 = getServiceConfig(gspc, "test.server1",
                NetUtils.LOCAL_HOST, 18080, HTTP2_SCHEME, "jetty");
        providers.put(serviceConfig1.getName(), serviceConfig1);

        ServiceConfig serviceConfig2 = getServiceConfig(gjspc, "test.server2",
                NetUtils.LOCAL_HOST, 18080, HTTP2_SCHEME, "jetty");
        providers.put(serviceConfig2.getName(), serviceConfig2);

        ServiceConfig serviceConfig3 = getServiceConfig(gspc, "test.server3",
                NetUtils.LOCAL_HOST, 18081, HTTP2_SCHEME,
                "jetty", CST_BASE_PATH1);
        providers.put(serviceConfig3.getName(), serviceConfig3);

        ServiceConfig serviceConfig4 = getServiceConfig(gjspc, "test.server4",
                NetUtils.LOCAL_HOST, 18081, HTTP2_SCHEME,
                "jetty", CST_BASE_PATH2);
        providers.put(serviceConfig4.getName(), serviceConfig4);

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

    private static ServiceConfig getServiceConfig(ProviderConfig gspc, String name,
            String ip, int port, String protocol, String transport, String basePath) {
        ServiceConfig serviceConfig = new ServiceConfig();
        serviceConfig.setName(name);
        serviceConfig.getProviderConfigs().add(gspc);
        serviceConfig.setIp(ip);
        serviceConfig.setPort(port);
        serviceConfig.setProtocol(protocol);
        serviceConfig.setTransporter(transport);
        serviceConfig.setBasePath(basePath);
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
    public void testHttpRpcClient() {
        // 1)准备配置
        BackendConfig backendConfig = new BackendConfig();
        ConsumerConfig<GreeterService> consumerConfig = new ConsumerConfig<>();
        backendConfig.setName("serviceId");
        backendConfig.setRequestTimeout(10000);
        consumerConfig.setServiceInterface(GreeterService.class);
        consumerConfig.setBackendConfig(backendConfig);
        backendConfig.setNamingUrl("ip://127.0.0.1:18080");
        backendConfig.setKeepAlive(false);
        backendConfig.setConnsPerAddr(4);
        backendConfig.setProtocol(HTTP2_SCHEME);
        try {
            // 2)获取代理
            GreeterService proxy = consumerConfig.getProxy();

            for (int i = 0; i < 20; i++) {
                RpcClientContext context = new RpcClientContext();
                HelloResponse helloResponse = proxy.sayHello(context, createPbRequest(TEST_MESSAGE));
                Assert.assertNotNull(helloResponse);
                String rspMessage = helloResponse.getMessage();
                logger.info("http rpc client request result: {}", rspMessage);
                Assert.assertTrue(rspMessage.contains(TEST_MESSAGE));
            }
        } finally {
            backendConfig.stop();
        }
    }

    @Test
    public void testHttpRpcClientWithOneWay() {
        // 1)准备配置
        BackendConfig backendConfig = new BackendConfig();
        ConsumerConfig<GreeterService> consumerConfig = new ConsumerConfig<>();
        backendConfig.setName("serviceId");
        backendConfig.setRequestTimeout(10000);
        consumerConfig.setServiceInterface(GreeterService.class);
        consumerConfig.setBackendConfig(backendConfig);
        backendConfig.setNamingUrl("ip://127.0.0.1:18080");
        backendConfig.setKeepAlive(false);
        backendConfig.setConnsPerAddr(4);
        backendConfig.setProtocol(HTTP2_SCHEME);
        try {
            // 2)获取代理
            GreeterService proxy = consumerConfig.getProxy();

            for (int i = 0; i < 20; i++) {
                RpcClientContext context = new RpcClientContext();
                context.setOneWay(true);
                HelloResponse helloResponse = proxy.sayHello(context, createPbRequest(TEST_MESSAGE));
                Assert.assertNull(helloResponse);
            }
        } finally {
            backendConfig.stop();
        }
    }

    @Test
    public void testHttpRpcClientBasePath1() {
        // 1)准备配置
        BackendConfig backendConfig = new BackendConfig();
        ConsumerConfig<GreeterService> consumerConfig = new ConsumerConfig<>();
        backendConfig.setName("serviceId");
        backendConfig.setRequestTimeout(10000);
        consumerConfig.setServiceInterface(GreeterService.class);
        consumerConfig.setBackendConfig(backendConfig);
        backendConfig.setNamingUrl("ip://127.0.0.1:18081");
        backendConfig.setKeepAlive(false);
        backendConfig.setConnsPerAddr(4);
        backendConfig.setProtocol(HTTP2_SCHEME);
        backendConfig.setBasePath(CST_BASE_PATH1);
        try {
            // 2)获取代理
            GreeterService proxy = consumerConfig.getProxy();

            for (int i = 0; i < 20; i++) {
                RpcClientContext context = new RpcClientContext();
                HelloResponse helloResponse = proxy.sayHello(context, createPbRequest(TEST_MESSAGE));
                Assert.assertNotNull(helloResponse);
                String rspMessage = helloResponse.getMessage();
                logger.info("http rpc client request result: {}", rspMessage);
                Assert.assertTrue(rspMessage.contains(TEST_MESSAGE));
            }
        } finally {
            backendConfig.stop();
        }
    }

    @Test
    public void testHttpRpcClientBasePath2() {
        // 1)准备配置
        BackendConfig backendConfig = new BackendConfig();
        ConsumerConfig<GreeterJsonService> consumerConfig = new ConsumerConfig<>();
        backendConfig.setName("serviceId");
        backendConfig.setRequestTimeout(10000);
        consumerConfig.setServiceInterface(GreeterJsonService.class);
        consumerConfig.setBackendConfig(backendConfig);
        backendConfig.setNamingUrl("ip://127.0.0.1:18081");
        backendConfig.setKeepAlive(false);
        backendConfig.setConnsPerAddr(4);
        backendConfig.setProtocol(HTTP2_SCHEME);
        backendConfig.setBasePath(CST_BASE_PATH2);
        try {
            // 2)获取代理
            GreeterJsonService proxy = consumerConfig.getProxy();

            for (int i = 0; i < 20; i++) {
                Map<String, Object> obj = new HashMap<>();
                obj.put("message", TEST_MESSAGE);

                RpcClientContext context = new RpcClientContext();
                Map helloResponse = proxy.sayHelloJson(context, obj);
                Assert.assertNotNull(helloResponse);
                String rspMessage = (String) helloResponse.get("message");
                logger.info("http rpc client request result: {}", rspMessage);
                Assert.assertTrue(rspMessage.contains(TEST_MESSAGE));
            }
        } finally {
            backendConfig.stop();
        }
    }

    @Test
    public void testHttpRpcClientBasePathNotExist() {
        // 1)准备配置
        BackendConfig backendConfig = new BackendConfig();
        ConsumerConfig<GreeterService> consumerConfig = new ConsumerConfig<>();
        backendConfig.setName("serviceId");
        backendConfig.setRequestTimeout(10000);
        consumerConfig.setServiceInterface(GreeterService.class);
        consumerConfig.setBackendConfig(backendConfig);
        backendConfig.setNamingUrl("ip://127.0.0.1:18081");
        backendConfig.setKeepAlive(false);
        backendConfig.setConnsPerAddr(4);
        backendConfig.setProtocol(HTTP2_SCHEME);
        backendConfig.setBasePath("not-exist-path");
        try {
            // 2)获取代理
            GreeterService proxy = consumerConfig.getProxy();

            RpcClientContext context = new RpcClientContext();
            proxy.sayHello(context, createPbRequest(TEST_MESSAGE));
            Assert.fail("no exception thrown");
        } catch (TRpcException e) {
            Assert.assertEquals(404, e.getBizCode());
        } finally {
            backendConfig.stop();
        }
    }

    @Test
    public void testHttpRpcClientWithFailed() {
        // 1)准备配置
        BackendConfig backendConfig = new BackendConfig();
        ConsumerConfig<GreeterService> consumerConfig = new ConsumerConfig<>();
        backendConfig.setName("serviceId");
        backendConfig.setRequestTimeout(10000);
        consumerConfig.setServiceInterface(GreeterService.class);
        consumerConfig.setBackendConfig(backendConfig);
        backendConfig.setNamingUrl("ip://127.0.0.1:18082");
        backendConfig.setKeepAlive(false);
        backendConfig.setConnsPerAddr(4);
        backendConfig.setProtocol(HTTP2_SCHEME);
        backendConfig.setBasePath("not-exist-path");
        try {
            // 2)获取代理
            GreeterService proxy = consumerConfig.getProxy();

            RpcClientContext context = new RpcClientContext();
            proxy.sayHello(context, createPbRequest(TEST_MESSAGE));
            Assert.fail("no exception thrown");
        } catch (TRpcException e) {
            Assert.assertEquals(0, e.getBizCode());
        } finally {
            backendConfig.stop();
        }
    }

}
