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

import static com.tencent.trpc.transport.http.common.Constants.HTTP_SCHEME;
import static com.tencent.trpc.transport.http.common.Constants.KEYSTORE_PASS;
import static com.tencent.trpc.transport.http.common.Constants.KEYSTORE_PATH;

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
import java.io.File;
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

public class HttpsRpcClientTest {

    private static final Logger logger = LoggerFactory.getLogger(HttpRpcServerTest.class);

    private static final String CST_BASE_PATH1 = "/";
    private static final String CST_BASE_PATH2 = "/test-base-path";

    private static ServerConfig serverConfig;

    private static Map<String, Object> extMap = new HashMap<>();

    @BeforeClass
    public static void startHttpServer() {

        String path = Http2RpcServerTest.class.getClassLoader().getResource("keystore.jks")
                .getFile();
        extMap.put(KEYSTORE_PATH, new File(path).getAbsolutePath());
        extMap.put(KEYSTORE_PASS, "init234");

        ConfigManager.stopTest();
        ConfigManager.startTest();

        ProviderConfig<GreeterService> gspc = new ProviderConfig<>();
        gspc.setServiceInterface(GreeterService.class);
        gspc.setRef(new GreeterServiceImpl1());

        ProviderConfig<GreeterJsonService> gjspc = new ProviderConfig<>();
        gjspc.setServiceInterface(GreeterJsonService.class);
        gjspc.setRef(new GreeterJsonServiceImpl1());

        HashMap<String, ServiceConfig> providers = new HashMap<>();

        ServiceConfig serviceConfig1 = getServiceConfig(gspc, "test.server1",
                NetUtils.LOCAL_HOST, 18091, HTTP_SCHEME, "jetty");
        providers.put(serviceConfig1.getName(), serviceConfig1);

        ServiceConfig serviceConfig2 = getServiceConfig(gjspc, "test.server2",
                NetUtils.LOCAL_HOST, 18091, HTTP_SCHEME, "jetty");
        providers.put(serviceConfig2.getName(), serviceConfig2);

        ServiceConfig serviceConfig3 = getServiceConfig(gspc, "test.server3",
                NetUtils.LOCAL_HOST, 18092, HTTP_SCHEME,
                "jetty", CST_BASE_PATH1);
        providers.put(serviceConfig3.getName(), serviceConfig3);

        ServiceConfig serviceConfig4 = getServiceConfig(gjspc, "test.server4",
                NetUtils.LOCAL_HOST, 18092, HTTP_SCHEME,
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
        serviceConfig.getExtMap().putAll(extMap);
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
        serviceConfig.getExtMap().putAll(extMap);
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
        BackendConfig backendConfig = new BackendConfig();
        ConsumerConfig<GreeterService> consumerConfig = new ConsumerConfig<>();
        backendConfig.setName("serviceId");
        backendConfig.setRequestTimeout(10000);
        consumerConfig.setServiceInterface(GreeterService.class);
        consumerConfig.setBackendConfig(backendConfig);
        backendConfig.setNamingUrl("ip://127.0.0.1:18091");
        backendConfig.setKeepAlive(false);
        backendConfig.setConnsPerAddr(4);
        backendConfig.setProtocol(HTTP_SCHEME);
        backendConfig.getExtMap().putAll(extMap);
        try {
            GreeterService proxy = consumerConfig.getProxy();

            for (int i = 0; i < 20; i++) {
                final String msg = "TRpc-Java!";

                RpcClientContext context = new RpcClientContext();
                HelloResponse helloResponse = proxy.sayHello(context, createPbRequest(msg));
                Assert.assertNotNull(helloResponse);
                String rspMessage = helloResponse.getMessage();
                logger.info("http rpc client request result: {}", rspMessage);
                Assert.assertTrue(rspMessage.contains(msg));
            }
        } finally {
            backendConfig.stop();
        }
    }

    @Test
    public void testHttpRpcClientWithOneWay() {
        BackendConfig backendConfig = new BackendConfig();
        ConsumerConfig<GreeterService> consumerConfig = new ConsumerConfig<>();
        backendConfig.setName("serviceId");
        backendConfig.setRequestTimeout(10000);
        consumerConfig.setServiceInterface(GreeterService.class);
        consumerConfig.setBackendConfig(backendConfig);
        backendConfig.setNamingUrl("ip://127.0.0.1:18091");
        backendConfig.setKeepAlive(false);
        backendConfig.setConnsPerAddr(4);
        backendConfig.setProtocol(HTTP_SCHEME);
        backendConfig.getExtMap().putAll(extMap);
        try {
            GreeterService proxy = consumerConfig.getProxy();

            for (int i = 0; i < 20; i++) {
                final String msg = "TRpc-Java!";

                RpcClientContext context = new RpcClientContext();
                context.setOneWay(true);
                HelloResponse helloResponse = proxy.sayHello(context, createPbRequest(msg));
                Assert.assertNull(helloResponse);
            }
        } finally {
            backendConfig.stop();
        }
    }

    @Test
    public void testHttpRpcClientBasePath1() {
        BackendConfig backendConfig = new BackendConfig();
        ConsumerConfig<GreeterService> consumerConfig = new ConsumerConfig<>();
        backendConfig.setName("serviceId");
        backendConfig.setRequestTimeout(10000);
        consumerConfig.setServiceInterface(GreeterService.class);
        consumerConfig.setBackendConfig(backendConfig);
        backendConfig.setNamingUrl("ip://127.0.0.1:18092");
        backendConfig.setKeepAlive(false);
        backendConfig.setConnsPerAddr(4);
        backendConfig.setProtocol(HTTP_SCHEME);
        backendConfig.setBasePath(CST_BASE_PATH1);
        backendConfig.getExtMap().putAll(extMap);
        try {
            GreeterService proxy = consumerConfig.getProxy();

            for (int i = 0; i < 20; i++) {
                final String msg = "TRpc-Java!";

                RpcClientContext context = new RpcClientContext();
                HelloResponse helloResponse = proxy.sayHello(context, createPbRequest(msg));
                Assert.assertNotNull(helloResponse);
                String rspMessage = helloResponse.getMessage();
                logger.info("http rpc client request result: {}", rspMessage);
                Assert.assertTrue(rspMessage.contains(msg));
            }
        } finally {
            backendConfig.stop();
        }
    }

    @Test
    public void testHttpRpcClientBasePath2() {
        BackendConfig backendConfig = new BackendConfig();
        ConsumerConfig<GreeterJsonService> consumerConfig = new ConsumerConfig<>();
        backendConfig.setName("serviceId");
        backendConfig.setRequestTimeout(10000);
        consumerConfig.setServiceInterface(GreeterJsonService.class);
        consumerConfig.setBackendConfig(backendConfig);
        backendConfig.setNamingUrl("ip://127.0.0.1:18092");
        backendConfig.setKeepAlive(false);
        backendConfig.setConnsPerAddr(4);
        backendConfig.setProtocol(HTTP_SCHEME);
        backendConfig.setBasePath(CST_BASE_PATH2);
        backendConfig.getExtMap().putAll(extMap);
        try {
            GreeterJsonService proxy = consumerConfig.getProxy();

            for (int i = 0; i < 20; i++) {
                final String msg = "TRpc-Java!";

                Map<String, Object> obj = new HashMap();
                obj.put("message", msg);

                RpcClientContext context = new RpcClientContext();
                Map helloResponse = proxy.sayHelloJson(context, obj);
                Assert.assertNotNull(helloResponse);
                String rspMessage = (String) helloResponse.get("message");
                logger.info("http rpc client request result: {}", rspMessage);
                Assert.assertTrue(rspMessage.contains(msg));
            }
        } finally {
            backendConfig.stop();
        }
    }

    @Test
    public void testHttpRpcClientBasePathNotExist() {
        BackendConfig backendConfig = new BackendConfig();
        ConsumerConfig<GreeterService> consumerConfig = new ConsumerConfig<>();
        backendConfig.setName("serviceId");
        backendConfig.setRequestTimeout(10000);
        consumerConfig.setServiceInterface(GreeterService.class);
        consumerConfig.setBackendConfig(backendConfig);
        backendConfig.setNamingUrl("ip://127.0.0.1:18092");
        backendConfig.setKeepAlive(false);
        backendConfig.setConnsPerAddr(4);
        backendConfig.setProtocol(HTTP_SCHEME);
        backendConfig.setBasePath("not-exist-path");
        backendConfig.getExtMap().putAll(extMap);
        try {
            GreeterService proxy = consumerConfig.getProxy();

            final String msg = "TRpc-Java!";
            RpcClientContext context = new RpcClientContext();
            proxy.sayHello(context, createPbRequest(msg));
            Assert.fail("no exception thrown");
        } catch (TRpcException e) {
            Assert.assertEquals(404, e.getBizCode());
        } finally {
            backendConfig.stop();
        }
    }

    @Test
    public void testHttpRpcClientWithFailed() {
        BackendConfig backendConfig = new BackendConfig();
        ConsumerConfig<GreeterService> consumerConfig = new ConsumerConfig<>();
        backendConfig.setName("serviceId");
        backendConfig.setRequestTimeout(10000);
        consumerConfig.setServiceInterface(GreeterService.class);
        consumerConfig.setBackendConfig(backendConfig);
        backendConfig.setNamingUrl("ip://127.0.0.1:18093");
        backendConfig.setKeepAlive(false);
        backendConfig.setConnsPerAddr(4);
        backendConfig.setProtocol(HTTP_SCHEME);
        backendConfig.setBasePath("not-exist-path");
        backendConfig.getExtMap().putAll(extMap);
        try {
            GreeterService proxy = consumerConfig.getProxy();

            final String msg = "TRpc-Java!";
            RpcClientContext context = new RpcClientContext();
            proxy.sayHello(context, createPbRequest(msg));
            Assert.fail("no exception thrown");
        } catch (TRpcException e) {
            Assert.assertEquals(0, e.getBizCode());
        } finally {
            backendConfig.stop();
        }
    }

}
