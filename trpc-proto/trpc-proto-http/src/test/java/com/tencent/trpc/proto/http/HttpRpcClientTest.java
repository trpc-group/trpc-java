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

package com.tencent.trpc.proto.http;

import static com.tencent.trpc.proto.http.common.HttpConstants.CONNECTION_REQUEST_TIMEOUT;
import static com.tencent.trpc.transport.http.common.Constants.HTTP_SCHEME;

import com.tencent.trpc.core.common.ConfigManager;
import com.tencent.trpc.core.common.config.BackendConfig;
import com.tencent.trpc.core.common.config.ConsumerConfig;
import com.tencent.trpc.core.common.config.GlobalConfig;
import com.tencent.trpc.core.common.config.ProviderConfig;
import com.tencent.trpc.core.common.config.ServerConfig;
import com.tencent.trpc.core.common.config.ServiceConfig;
import com.tencent.trpc.core.exception.TRpcException;
import com.tencent.trpc.core.logger.Logger;
import com.tencent.trpc.core.logger.LoggerFactory;
import com.tencent.trpc.core.rpc.RpcClientContext;
import com.tencent.trpc.core.rpc.RpcServerContext;
import com.tencent.trpc.core.utils.NetUtils;
import com.tencent.trpc.proto.http.common.RpcServerContextWithHttp;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHeaders;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import tests.service.GreeterJavaBeanService;
import tests.service.GreeterJavaBeanService.GenericResponseBean;
import tests.service.GreeterJavaBeanService.InnerMsg;
import tests.service.GreeterJavaBeanService.RequestBean;
import tests.service.GreeterJavaBeanService.ResponseBean;
import tests.service.GreeterJsonService;
import tests.service.GreeterParameterizedService;
import tests.service.GreeterParameterizedService.RequestParameterizedBean;
import tests.service.GreeterService;
import tests.service.HelloRequestProtocol.HelloRequest;
import tests.service.HelloRequestProtocol.HelloResponse;
import tests.service.impl1.GreeterJavaBeanServiceImpl;
import tests.service.impl1.GreeterJsonServiceImpl1;
import tests.service.impl1.GreeterParameterizedServiceImpl;
import tests.service.impl1.GreeterServiceImpl1;

public class HttpRpcClientTest {

    private static final Logger logger = LoggerFactory.getLogger(HttpRpcServerTest.class);

    private static final String CST_BASE_PATH1 = "/";
    private static final String CST_BASE_PATH2 = "/test-base-path";
    private static final String TEST_MESSAGE = "hello";
    private static final String TEST_INNER_MESSAGE = "tRPC-Java!";
    private static final String CONTAINER_KEY = "test-container";
    private static final String FULL_SET_KEY = "test-fullset";
    private static final Integer REQUEST_TIMEOUT = 1000;
    private static final Integer MAX_CONNECTIONS = 20480;
    private static final Integer CONNECTION_REQUEST_TIMEOUT_VALUE = 900;
    private static ServerConfig serverConfig;

    @BeforeClass
    public static void startHttpServer() {
        ConfigManager.stopTest();
        ConfigManager.startTest();
        GlobalConfig globalConfig = ConfigManager.getInstance().getGlobalConfig();
        globalConfig.setContainerName(CONTAINER_KEY);
        globalConfig.setFullSetName(FULL_SET_KEY);
        globalConfig.setEnableSet(true);

        ProviderConfig<GreeterService> gspc = new ProviderConfig<>();
        gspc.setServiceInterface(GreeterService.class);
        gspc.setRef(new GreeterServiceImpl1());
        // gspc.setProtocolIds(Collections.singletonList("http"));

        ProviderConfig<GreeterJsonService> gjspc = new ProviderConfig<>();
        gjspc.setServiceInterface(GreeterJsonService.class);
        gjspc.setRef(new GreeterJsonServiceImpl1());
        // gjspc.setProtocolIds(Collections.singletonList("http"));

        ProviderConfig<GreeterJavaBeanService> javaBeanService = new ProviderConfig<>();
        javaBeanService.setServiceInterface(GreeterJavaBeanService.class);
        javaBeanService.setRef(new GreeterJavaBeanServiceImpl());

        ProviderConfig<GreeterParameterizedService> parameterizedService = new ProviderConfig<>();
        parameterizedService.setServiceInterface(GreeterParameterizedService.class);
        parameterizedService.setRef(new GreeterParameterizedServiceImpl());

        HashMap<String, ServiceConfig> providers = new HashMap<>();

        ServiceConfig serviceConfig1 = getServiceConfig(gspc, "test.server1", NetUtils.LOCAL_HOST,
                18088, HTTP_SCHEME, "jetty");
        providers.put(serviceConfig1.getName(), serviceConfig1);
        ServiceConfig serviceConfig2 = getServiceConfig(gjspc, "test.server2", NetUtils.LOCAL_HOST,
                18088, HTTP_SCHEME, "jetty");
        providers.put(serviceConfig2.getName(), serviceConfig2);
        ServiceConfig serviceConfig3 = getServiceConfig(gspc, "test.server3", NetUtils.LOCAL_HOST,
                18089, HTTP_SCHEME, "jetty", CST_BASE_PATH1);
        providers.put(serviceConfig3.getName(), serviceConfig3);
        ServiceConfig serviceConfig4 = getServiceConfig(gjspc, "test.server4", NetUtils.LOCAL_HOST,
                18089, HTTP_SCHEME, "jetty", CST_BASE_PATH2);
        providers.put(serviceConfig4.getName(), serviceConfig4);
        ServiceConfig serviceConfig5 = getServiceConfig(javaBeanService, "test.server5",
                NetUtils.LOCAL_HOST,
                18088, HTTP_SCHEME, "jetty");
        providers.put(serviceConfig5.getName(), serviceConfig5);

        ServiceConfig serviceConfig6 = getServiceConfig(parameterizedService, "test.server6",
                NetUtils.LOCAL_HOST,
                18088, HTTP_SCHEME, "jetty");
        providers.put(serviceConfig6.getName(), serviceConfig6);

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

    private static RequestBean createJavaBeanRequest(String msg, String innerMsg) {
        RequestBean requestBean = new RequestBean();
        requestBean.setMessage(msg);
        InnerMsg innerMsg1 = new InnerMsg();
        innerMsg1.setMsg(innerMsg);
        requestBean.setInnerMsg(innerMsg1);
        return requestBean;
    }

    @Test
    public void testHttpRpcClient() {
        BackendConfig backendConfig = new BackendConfig();
        backendConfig.setName("serviceId");
        backendConfig.setRequestTimeout(REQUEST_TIMEOUT);
        backendConfig.setMaxConns(MAX_CONNECTIONS);
        backendConfig.getExtMap().put(CONNECTION_REQUEST_TIMEOUT, CONNECTION_REQUEST_TIMEOUT_VALUE);
        backendConfig.setNamingUrl("ip://127.0.0.1:18088");
        backendConfig.setKeepAlive(false);
        backendConfig.setConnsPerAddr(4);
        backendConfig.setProtocol("http");

        ConsumerConfig<GreeterService> consumerConfig = new ConsumerConfig<>();
        consumerConfig.setServiceInterface(GreeterService.class);
        consumerConfig.setBackendConfig(backendConfig);

        try {
            GreeterService proxy = consumerConfig.getProxy();

            for (int i = 0; i < 20; i++) {
                RpcClientContext context = new RpcClientContext();
                HelloResponse helloResponse = proxy.sayHello(context, createPbRequest(TEST_INNER_MESSAGE));
                Assert.assertNotNull(helloResponse);
                String rspMessage = helloResponse.getMessage();
                logger.info("http rpc client request result: {}", rspMessage);
                Assert.assertTrue(rspMessage.contains(TEST_INNER_MESSAGE));
            }
        } finally {
            backendConfig.stop();
        }
    }

    @Test
    public void testHttpRpcClientBasePath1() {
        BackendConfig backendConfig = new BackendConfig();
        backendConfig.setName("serviceId");
        backendConfig.setRequestTimeout(REQUEST_TIMEOUT);
        backendConfig.setMaxConns(MAX_CONNECTIONS);
        ConsumerConfig<GreeterService> consumerConfig = new ConsumerConfig<>();
        consumerConfig.setServiceInterface(GreeterService.class);
        consumerConfig.setBackendConfig(backendConfig);
        backendConfig.setNamingUrl("ip://127.0.0.1:18089");
        backendConfig.setKeepAlive(false);
        backendConfig.setConnsPerAddr(4);
        backendConfig.setProtocol("http");
        backendConfig.setBasePath(CST_BASE_PATH1);
        try {
            GreeterService proxy = consumerConfig.getProxy();

            for (int i = 0; i < 20; i++) {
                RpcClientContext context = new RpcClientContext();
                HelloResponse helloResponse = proxy.sayHello(context, createPbRequest(TEST_INNER_MESSAGE));
                Assert.assertNotNull(helloResponse);
                String rspMessage = helloResponse.getMessage();
                logger.info("http rpc client request result: {}", rspMessage);
                Assert.assertTrue(rspMessage.contains(TEST_INNER_MESSAGE));
            }
        } finally {
            backendConfig.stop();
        }
    }

    @Test
    public void testHttpRpcClientBasePath2() {
        BackendConfig backendConfig = new BackendConfig();
        backendConfig.setName("serviceId");
        backendConfig.setRequestTimeout(REQUEST_TIMEOUT);
        backendConfig.setMaxConns(MAX_CONNECTIONS);
        ConsumerConfig<GreeterJsonService> consumerConfig = new ConsumerConfig<>();
        consumerConfig.setServiceInterface(GreeterJsonService.class);
        consumerConfig.setBackendConfig(backendConfig);
        backendConfig.setNamingUrl("ip://127.0.0.1:18089");
        backendConfig.setKeepAlive(false);
        backendConfig.setConnsPerAddr(4);
        backendConfig.setProtocol("http");
        backendConfig.setBasePath(CST_BASE_PATH2);
        try {
            GreeterJsonService proxy = consumerConfig.getProxy();

            for (int i = 0; i < 20; i++) {
                Map<String, Object> obj = new HashMap<>();
                obj.put("message", TEST_INNER_MESSAGE);

                RpcClientContext context = new RpcClientContext();
                Map helloResponse = proxy.sayHelloJson(context, obj);
                Assert.assertNotNull(helloResponse);
                String rspMessage = (String) helloResponse.get("message");
                logger.info("http rpc client request result: {}", rspMessage);
                Assert.assertTrue(rspMessage.contains(TEST_INNER_MESSAGE));
            }
        } finally {
            backendConfig.stop();
        }
    }

    @Test
    public void testHttpRpcClientBasePathNotExist() {
        BackendConfig backendConfig = new BackendConfig();
        backendConfig.setName("serviceId");
        backendConfig.setRequestTimeout(REQUEST_TIMEOUT);
        backendConfig.setMaxConns(MAX_CONNECTIONS);
        ConsumerConfig<GreeterService> consumerConfig = new ConsumerConfig<>();
        consumerConfig.setServiceInterface(GreeterService.class);
        consumerConfig.setBackendConfig(backendConfig);
        backendConfig.setNamingUrl("ip://127.0.0.1:18089");
        backendConfig.setKeepAlive(false);
        backendConfig.setConnsPerAddr(4);
        backendConfig.setProtocol("http");
        backendConfig.setBasePath("not-exist-path");
        try {
            GreeterService proxy = consumerConfig.getProxy();

            RpcClientContext context = new RpcClientContext();
            proxy.sayHello(context, createPbRequest(TEST_INNER_MESSAGE));
            Assert.fail("no exception thrown");
        } catch (TRpcException e) {
            Assert.assertEquals(404, e.getBizCode());
        } finally {
            backendConfig.stop();
        }
    }

    @Test
    public void testHttpRpcClientWithParameterizedBean() {
        // 1)准备配置
        BackendConfig backendConfig = new BackendConfig();
        backendConfig.setName("serviceId");
        backendConfig.setRequestTimeout(REQUEST_TIMEOUT);
        backendConfig.setMaxConns(MAX_CONNECTIONS);
        backendConfig.setNamingUrl("ip://127.0.0.1:18088");
        backendConfig.setKeepAlive(false);
        backendConfig.setConnsPerAddr(4);
        backendConfig.setProtocol("http");
        ConsumerConfig<GreeterParameterizedService> consumerConfig = new ConsumerConfig<>();
        consumerConfig.setServiceInterface(GreeterParameterizedService.class);
        consumerConfig.setBackendConfig(backendConfig);
        try {
            // 2)获取代理
            GreeterParameterizedService proxy = consumerConfig.getProxy();

            for (int i = 0; i < 20; i++) {
                final String msg = "I am";
                final String innerMsg = " ParameterizedBean!";

                RpcClientContext context = new RpcClientContext();
                Map helloResponse = proxy
                        .sayHelloParameterized(context, RequestParameterizedBean.of("message", msg + innerMsg));
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
    public void testHttpRpcClientWithJavaBean() {
        BackendConfig backendConfig = new BackendConfig();
        backendConfig.setName("serviceId");
        backendConfig.setRequestTimeout(REQUEST_TIMEOUT);
        backendConfig.setMaxConns(MAX_CONNECTIONS);
        backendConfig.setNamingUrl("ip://127.0.0.1:18088");
        backendConfig.setKeepAlive(false);
        backendConfig.setConnsPerAddr(4);
        backendConfig.setProtocol("http");
        ConsumerConfig<GreeterJavaBeanService> consumerConfig = new ConsumerConfig<>();
        consumerConfig.setServiceInterface(GreeterJavaBeanService.class);
        consumerConfig.setBackendConfig(backendConfig);
        try {
            GreeterJavaBeanService proxy = consumerConfig.getProxy();

            for (int i = 0; i < 20; i++) {
                RpcClientContext context = new RpcClientContext();
                ResponseBean helloResponse = proxy
                        .sayHello(context, createJavaBeanRequest(TEST_MESSAGE, TEST_INNER_MESSAGE));
                Assert.assertNotNull(helloResponse);
                String rspMessage = helloResponse.getMessage();
                logger.info("http rpc client request result: {}", rspMessage);
                Assert.assertTrue(rspMessage.contains(TEST_MESSAGE));
                Assert.assertEquals(TEST_MESSAGE, helloResponse.getMessage());
                Assert.assertEquals(TEST_INNER_MESSAGE, helloResponse.getInnerMsg().getMsg());
            }
        } finally {
            backendConfig.stop();
        }
    }

    @Test
    public void testHttpRpcClientWithGenericJavaBean() {
        BackendConfig backendConfig = new BackendConfig();
        backendConfig.setName("serviceId");
        backendConfig.setRequestTimeout(REQUEST_TIMEOUT);
        backendConfig.setMaxConns(MAX_CONNECTIONS);
        backendConfig.setNamingUrl("ip://127.0.0.1:18088");
        backendConfig.setKeepAlive(false);
        backendConfig.setConnsPerAddr(4);
        backendConfig.setProtocol("http");
        ConsumerConfig<GreeterJavaBeanService> consumerConfig = new ConsumerConfig<>();
        consumerConfig.setServiceInterface(GreeterJavaBeanService.class);
        consumerConfig.setBackendConfig(backendConfig);
        try {
            GreeterJavaBeanService proxy = consumerConfig.getProxy();

            for (int i = 0; i < 20; i++) {
                RpcClientContext context = new RpcClientContext();
                GenericResponseBean<String> helloResponse = proxy
                        .sayHelloWithGeneric(context, createJavaBeanRequest(TEST_MESSAGE, TEST_INNER_MESSAGE));
                Assert.assertNotNull(helloResponse);
                String rspMessage = helloResponse.getMessage();
                logger.info("http rpc client request result: {}", rspMessage);
                Assert.assertTrue(rspMessage.contains(TEST_MESSAGE));
                Assert.assertEquals(TEST_MESSAGE, helloResponse.getMessage());
                Assert.assertEquals(TEST_INNER_MESSAGE, helloResponse.getInnerMsg().getMsg());
            }
        } finally {
            backendConfig.stop();
        }
    }

    @Test
    public void testHttpRpcClientWithBlankRsp() {
        BackendConfig backendConfig = new BackendConfig();
        backendConfig.setName("serviceId");
        backendConfig.setRequestTimeout(REQUEST_TIMEOUT);
        backendConfig.setMaxConns(MAX_CONNECTIONS);
        backendConfig.setNamingUrl("ip://127.0.0.1:18088");
        backendConfig.setKeepAlive(false);
        backendConfig.setConnsPerAddr(4);
        backendConfig.setProtocol("http");
        ConsumerConfig<GreeterService> consumerConfig = new ConsumerConfig<>();
        consumerConfig.setServiceInterface(GreeterService.class);
        consumerConfig.setBackendConfig(backendConfig);
        try {
            GreeterService proxy = consumerConfig.getProxy();

            for (int i = 0; i < 20; i++) {
                RpcClientContext context = new RpcClientContext();
                String helloResponse = proxy.sayBlankHello(context, createPbRequest(TEST_INNER_MESSAGE));
                Assert.assertNull(helloResponse);
                logger.info("http rpc client request result: {}", helloResponse);
                Assert.assertTrue(StringUtils.isEmpty(helloResponse));
            }
        } finally {
            backendConfig.stop();
        }
    }

    @Test
    public void testHttpRpcClientWithHeaders() {
        BackendConfig backendConfig = new BackendConfig();
        backendConfig.setName("serviceId");
        backendConfig.setRequestTimeout(REQUEST_TIMEOUT);
        backendConfig.setMaxConns(MAX_CONNECTIONS);
        backendConfig.getExtMap().put(CONNECTION_REQUEST_TIMEOUT, 900);
        backendConfig.setNamingUrl("ip://127.0.0.1:18088");
        backendConfig.setKeepAlive(false);
        backendConfig.setConnsPerAddr(4);
        backendConfig.setProtocol("http");

        ConsumerConfig<GreeterService> consumerConfig = new ConsumerConfig<>();
        consumerConfig.setServiceInterface(GreeterService.class);
        consumerConfig.setBackendConfig(backendConfig);

        try {
            GreeterService proxy = consumerConfig.getProxy();

            RpcServerContext context = new RpcServerContextWithHttp();
            context.getReqAttachMap().put(HttpHeaders.CONNECTION, "keep-alive");
            context.getReqAttachMap().put(HttpHeaders.CONTENT_LENGTH, "100");
            context.getReqAttachMap().put("Object", new Object());
            context.getReqAttachMap().put("key", "key");
            context.getReqAttachMap().put("key2", "key".getBytes(StandardCharsets.UTF_8));
            HelloResponse helloResponse = proxy.sayHello(context.newClientContext(),
                    createPbRequest(TEST_INNER_MESSAGE));
            Assert.assertNotNull(helloResponse);
            String rspMessage = helloResponse.getMessage();
            logger.info("http rpc client request result: {}", rspMessage);
            Assert.assertTrue(rspMessage.contains(TEST_INNER_MESSAGE));
        } finally {
            backendConfig.stop();
        }
    }

    @Test
    public void testHttpRpcClientWithHeaders2() {
        // 1)准备配置
        BackendConfig backendConfig = new BackendConfig();
        backendConfig.setName("serviceId");
        backendConfig.setRequestTimeout(REQUEST_TIMEOUT);
        backendConfig.setMaxConns(MAX_CONNECTIONS);
        backendConfig.getExtMap().put(CONNECTION_REQUEST_TIMEOUT, 900);
        backendConfig.setNamingUrl("ip://127.0.0.1:18088");
        backendConfig.setKeepAlive(false);
        backendConfig.setConnsPerAddr(4);
        backendConfig.setProtocol("http");

        ConsumerConfig<GreeterService> consumerConfig = new ConsumerConfig<>();
        consumerConfig.setServiceInterface(GreeterService.class);
        consumerConfig.setBackendConfig(backendConfig);

        try {
            // 2)获取代理
            GreeterService proxy = consumerConfig.getProxy();

            RpcClientContext context = new RpcClientContext();
            context.getReqAttachMap().put(HttpHeaders.CONNECTION, "keep-alive");
            context.getReqAttachMap().put(HttpHeaders.CONTENT_LENGTH, "100");
            context.getReqAttachMap().put("Object", new Object());
            context.getReqAttachMap().put("key", "key");
            context.getReqAttachMap().put("key2", "key".getBytes(StandardCharsets.UTF_8));
            HelloResponse helloResponse = proxy.sayHello(context, createPbRequest(TEST_INNER_MESSAGE));
            Assert.assertNotNull(helloResponse);
            String rspMessage = helloResponse.getMessage();
            logger.info("http rpc client request result: {}", rspMessage);
            Assert.assertTrue(rspMessage.contains(TEST_INNER_MESSAGE));
        } finally {
            backendConfig.stop();
        }
    }

    @Test
    public void testHttpRpcAttachmentWithJavaBean() {
        BackendConfig backendConfig = new BackendConfig();
        backendConfig.setName("serviceId");
        backendConfig.setRequestTimeout(REQUEST_TIMEOUT);
        backendConfig.setMaxConns(MAX_CONNECTIONS);
        backendConfig.setNamingUrl("ip://127.0.0.1:18088");
        backendConfig.setKeepAlive(false);
        backendConfig.setConnsPerAddr(4);
        backendConfig.setProtocol("http");
        ConsumerConfig<GreeterJavaBeanService> consumerConfig = new ConsumerConfig<>();
        consumerConfig.setServiceInterface(GreeterJavaBeanService.class);
        consumerConfig.setBackendConfig(backendConfig);
        try {
            GreeterJavaBeanService proxy = consumerConfig.getProxy();

            RpcClientContext context = new RpcClientContext();
            ResponseBean helloResponse = proxy
                    .assertAttachment(context, createJavaBeanRequest(TEST_MESSAGE, TEST_INNER_MESSAGE));
            Assert.assertNotNull(helloResponse);
            String rspMessage = helloResponse.getMessage();
            logger.info("http rpc client request result: {}", rspMessage);
            Assert.assertTrue(rspMessage.contains(TEST_MESSAGE));
            Assert.assertEquals(TEST_MESSAGE, helloResponse.getMessage());
            Assert.assertEquals(TEST_INNER_MESSAGE, helloResponse.getInnerMsg().getMsg());
        } finally {
            backendConfig.stop();
        }
    }
}
