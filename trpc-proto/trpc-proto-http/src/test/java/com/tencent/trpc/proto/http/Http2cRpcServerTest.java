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

import static com.tencent.trpc.transport.http.common.Constants.HTTP2_SCHEME;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tencent.trpc.core.common.ConfigManager;
import com.tencent.trpc.core.common.config.ProviderConfig;
import com.tencent.trpc.core.common.config.ServerConfig;
import com.tencent.trpc.core.common.config.ServiceConfig;
import com.tencent.trpc.core.logger.Logger;
import com.tencent.trpc.core.logger.LoggerFactory;
import com.tencent.trpc.core.utils.Charsets;
import com.tencent.trpc.core.utils.JsonUtils;
import com.tencent.trpc.core.utils.NetUtils;
import com.tencent.trpc.proto.http.common.ErrorResponse;
import com.tencent.trpc.proto.http.common.HttpConstants;
import java.net.URLEncoder;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.apache.hc.client5.http.async.methods.SimpleHttpRequest;
import org.apache.hc.client5.http.async.methods.SimpleHttpRequests;
import org.apache.hc.client5.http.async.methods.SimpleHttpResponse;
import org.apache.hc.client5.http.impl.async.CloseableHttpAsyncClient;
import org.apache.hc.client5.http.impl.async.HttpAsyncClients;
import org.apache.hc.core5.concurrent.FutureCallback;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpHeaders;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import tests.service.GreeterJavaBeanService;
import tests.service.GreeterJsonService;
import tests.service.GreeterService;
import tests.service.HelloRequestProtocol.HelloRequest;
import tests.service.impl1.GreeterJavaBeanServiceImpl;
import tests.service.impl1.GreeterJsonServiceImpl1;
import tests.service.impl1.GreeterServiceImpl1;

public class Http2cRpcServerTest {

    private static final Logger logger = LoggerFactory.getLogger(Http2cRpcServerTest.class);

    private static final String TEST_MESSAGE = "tRPC-Java!";
    private static final String TEST_MESSAGE_BODY = "{\"message\":\"Hello, tRPC-Java!\"}";
    private static final String TEST_MESSAGE_BODY_BASE64 = "{\"message\":\"Hello, dFJQQy1KYXZhIQ==\"}";

    private static ServerConfig serverConfig;

    private static CloseableHttpAsyncClient httpAsyncClient;

    @BeforeAll
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

        ProviderConfig<GreeterJavaBeanService> javaBeanService = new ProviderConfig<>();
        javaBeanService.setServiceInterface(GreeterJavaBeanService.class);
        javaBeanService.setRef(new GreeterJavaBeanServiceImpl());

        HashMap<String, ServiceConfig> providers = new HashMap<>();

        ServiceConfig serviceConfig1 = getServiceConfig(gspc, "test.server1",
                NetUtils.LOCAL_HOST, 18083, HTTP2_SCHEME, "jetty");

        providers.put(serviceConfig1.getName(), serviceConfig1);

        ServiceConfig serviceConfig2 = getServiceConfig(gjspc, "test.server2",
                NetUtils.LOCAL_HOST, 18083, HTTP2_SCHEME, "jetty");
        providers.put(serviceConfig2.getName(), serviceConfig2);

        ServiceConfig serviceConfig3 = getServiceConfig(javaBeanService, "test.server3",
                NetUtils.LOCAL_HOST, 18083, HTTP2_SCHEME, "jetty");
        providers.put(serviceConfig3.getName(), serviceConfig3);

        ServerConfig sc = new ServerConfig();
        sc.setServiceMap(providers);
        sc.setApp("http-test-app");
        sc.setLocalIp("127.0.0.1");
        sc.init();

        serverConfig = sc;

        httpAsyncClient = HttpAsyncClients.customHttp2().build();
        httpAsyncClient.start();
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

    @AfterAll
    public static void stopHttpServer() {
        ConfigManager.stopTest();
        if (serverConfig != null) {
            serverConfig.stop();
            serverConfig = null;
        }
    }

    @Test
    public void testNotFoundService() throws Exception {
        SimpleHttpRequest simpleHttpRequest =
                SimpleHttpRequests
                        .get("http://localhost:18083/NotExistService/NotExistMethod");
        Future<SimpleHttpResponse> httpResponseFuture = httpAsyncClient.execute(simpleHttpRequest,
                new FutureCallback<SimpleHttpResponse>() {
                    @Override
                    public void completed(SimpleHttpResponse result) {
                        logger.debug(result.getBodyText());
                    }

                    @Override
                    public void failed(Exception ex) {
                        logger.error("failed, ex: ", ex);
                    }

                    @Override
                    public void cancelled() {
                        logger.error("cancelled");
                    }
                });
        SimpleHttpResponse simpleHttpResponse = httpResponseFuture.get(2000, TimeUnit.MILLISECONDS);
        Assertions.assertNotEquals(null, simpleHttpResponse);
        logger.error(simpleHttpResponse.getBodyText());
        Assertions.assertEquals("HTTP", simpleHttpResponse.getVersion().getProtocol());
        Assertions.assertEquals(2, simpleHttpResponse.getVersion().getMajor());
        logger.info("response code is {}", simpleHttpResponse.getCode());
        Assertions.assertEquals(404, simpleHttpResponse.getCode());
        logger.info("http response is: {}", simpleHttpResponse.getBodyText());
        ErrorResponse errorResponse = JsonUtils.fromJson(simpleHttpResponse.getBodyText(), ErrorResponse.class);
        Assertions.assertEquals("not found service", errorResponse.getMessage());

    }

    @Test
    public void testPostByJson() throws Exception {
        SimpleHttpRequest simpleHttpRequest =
                SimpleHttpRequests
                        .post("http://localhost:18083/tencent.trpc.http.GreeterService/sayHello");
        simpleHttpRequest.setHeader(HttpHeaders.CONTENT_TYPE, HttpConstants.CONTENT_TYPE_JSON);
        Map<String, byte[]> body = new HashMap<>();
        body.put("message", TEST_MESSAGE.getBytes(Charsets.UTF_8));
        ObjectMapper objectMapper = new ObjectMapper();
        simpleHttpRequest
                .setBody(objectMapper.writeValueAsString(body), ContentType.APPLICATION_JSON);
        Future<SimpleHttpResponse> httpResponseFuture = httpAsyncClient.execute(simpleHttpRequest,
                new FutureCallback<SimpleHttpResponse>() {
                    @Override
                    public void completed(SimpleHttpResponse result) {
                        logger.debug(result.getBodyText());
                    }

                    @Override
                    public void failed(Exception ex) {
                        logger.error("failed, ex: ", ex);
                    }

                    @Override
                    public void cancelled() {
                        logger.error("cancelled");
                    }
                });
        SimpleHttpResponse simpleHttpResponse = httpResponseFuture.get(2000, TimeUnit.MILLISECONDS);
        Assertions.assertNotEquals(null, simpleHttpResponse);
        logger.error(simpleHttpResponse.getBodyText());
        Assertions.assertEquals("HTTP", simpleHttpResponse.getVersion().getProtocol());
        Assertions.assertEquals(2, simpleHttpResponse.getVersion().getMajor());
        logger.info("response code is {}", simpleHttpResponse.getCode());
        Assertions.assertEquals(200, simpleHttpResponse.getCode());
        logger.info("http response is: {}", simpleHttpResponse.getBodyText());
        Assertions.assertEquals(TEST_MESSAGE_BODY_BASE64, simpleHttpResponse.getBodyText());
    }

    @Test
    public void testPostByPB() throws Exception {
        SimpleHttpRequest simpleHttpRequest =
                SimpleHttpRequests
                        .post("http://localhost:18083/tencent.trpc.http.GreeterService/sayHello");
        simpleHttpRequest.setHeader(HttpHeaders.CONTENT_TYPE, HttpConstants.CONTENT_TYPE_PROTOBUF);

        HelloRequest.Builder reqBuilder = HelloRequest.newBuilder();
        reqBuilder.setMessage(TEST_MESSAGE);

        simpleHttpRequest.setBody(reqBuilder.build().toByteArray(), ContentType.DEFAULT_BINARY);
        Future<SimpleHttpResponse> httpResponseFuture = httpAsyncClient.execute(simpleHttpRequest,
                new FutureCallback<SimpleHttpResponse>() {
                    @Override
                    public void completed(SimpleHttpResponse result) {
                        logger.debug(result.getBodyText());
                    }

                    @Override
                    public void failed(Exception ex) {
                        logger.error("failed, ex: ", ex);
                    }

                    @Override
                    public void cancelled() {
                        logger.error("cancelled");
                    }
                });
        SimpleHttpResponse simpleHttpResponse = httpResponseFuture.get(2000, TimeUnit.MILLISECONDS);
        Assertions.assertNotEquals(null, simpleHttpResponse);
        logger.error(simpleHttpResponse.getBodyText());
        Assertions.assertEquals("HTTP", simpleHttpResponse.getVersion().getProtocol());
        Assertions.assertEquals(2, simpleHttpResponse.getVersion().getMajor());
        logger.info("response code is {}", simpleHttpResponse.getCode());
        Assertions.assertEquals(200, simpleHttpResponse.getCode());
        logger.info("http response is: {}", simpleHttpResponse.getBodyText());
        Assertions.assertEquals(TEST_MESSAGE_BODY, simpleHttpResponse.getBodyText());
    }

    @Test
    public void testPostMapByJson() throws Exception {
        SimpleHttpRequest simpleHttpRequest =
                SimpleHttpRequests
                        .post("http://localhost:18083/tencent.trpc.http.GreeterJsonService/sayHelloJson");
        simpleHttpRequest.setHeader(HttpHeaders.CONTENT_TYPE, HttpConstants.CONTENT_TYPE_JSON);
        Map<String, byte[]> body = new HashMap<>();
        body.put("message", TEST_MESSAGE.getBytes(Charsets.UTF_8));
        ObjectMapper objectMapper = new ObjectMapper();
        simpleHttpRequest
                .setBody(objectMapper.writeValueAsString(body), ContentType.APPLICATION_JSON);
        Future<SimpleHttpResponse> httpResponseFuture = httpAsyncClient.execute(simpleHttpRequest,
                new FutureCallback<SimpleHttpResponse>() {
                    @Override
                    public void completed(SimpleHttpResponse result) {
                        logger.debug(result.getBodyText());
                    }

                    @Override
                    public void failed(Exception ex) {
                        logger.error("failed, ex: ", ex);
                    }

                    @Override
                    public void cancelled() {
                        logger.error("cancelled");
                    }
                });
        SimpleHttpResponse simpleHttpResponse = httpResponseFuture.get(2000, TimeUnit.MILLISECONDS);
        Assertions.assertNotEquals(null, simpleHttpResponse);
        logger.error(simpleHttpResponse.getBodyText());
        Assertions.assertEquals("HTTP", simpleHttpResponse.getVersion().getProtocol());
        Assertions.assertEquals(2, simpleHttpResponse.getVersion().getMajor());
        logger.info("response code is {}", simpleHttpResponse.getCode());
        Assertions.assertEquals(200, simpleHttpResponse.getCode());
        logger.info("http response is: {}", simpleHttpResponse.getBodyText());
        Assertions.assertEquals("{\"message\":\"Hi:dFJQQy1KYXZhIQ==\"}",
                simpleHttpResponse.getBodyText());
    }

    @Test
    public void testGet() throws Exception {
        String url =
                "http://localhost:18083/tencent.trpc.http.GreeterService/sayHello?message="
                        + URLEncoder.encode(
                        Base64.getEncoder().encodeToString("你好TRpc-Java".getBytes(Charsets.UTF_8)),
                        "UTF-8");
        SimpleHttpRequest simpleHttpRequest = SimpleHttpRequests.get(url);
        Future<SimpleHttpResponse> httpResponseFuture = httpAsyncClient.execute(simpleHttpRequest,
                new FutureCallback<SimpleHttpResponse>() {
                    @Override
                    public void completed(SimpleHttpResponse result) {
                        logger.debug(result.getBodyText());
                    }

                    @Override
                    public void failed(Exception ex) {
                        logger.error("failed, ex: ", ex);
                    }

                    @Override
                    public void cancelled() {
                        logger.error("cancelled");
                    }
                });
        SimpleHttpResponse simpleHttpResponse = httpResponseFuture.get(2000, TimeUnit.MILLISECONDS);
        Assertions.assertNotEquals(null, simpleHttpResponse);
        logger.error(simpleHttpResponse.getBodyText());
        Assertions.assertEquals("HTTP", simpleHttpResponse.getVersion().getProtocol());
        Assertions.assertEquals(2, simpleHttpResponse.getVersion().getMajor());
        logger.info("response code is {}", simpleHttpResponse.getCode());
        Assertions.assertEquals(200, simpleHttpResponse.getCode());
        logger.info("http response is: {}", simpleHttpResponse.getBodyText());
        Assertions.assertEquals("{\"message\":\"Hello, 5L2g5aW9VFJwYy1KYXZh\"}",
                simpleHttpResponse.getBodyText());
    }

    @Test
    public void testConvertBeanWithGetMethod() throws Exception {
        String url =
                "http://localhost:18083/tencent.trpc.http.GreeterService/sayHelloNonPbType?"
                        + "message=TRpc-Java&status=1&comments=first&comments=two";
        SimpleHttpRequest simpleHttpRequest = SimpleHttpRequests.get(url);
        Future<SimpleHttpResponse> httpResponseFuture = httpAsyncClient.execute(simpleHttpRequest,
                new FutureCallback<SimpleHttpResponse>() {
                    @Override
                    public void completed(SimpleHttpResponse result) {
                        logger.debug(result.getBodyText());
                    }

                    @Override
                    public void failed(Exception ex) {
                        logger.error("failed, ex: ", ex);
                    }

                    @Override
                    public void cancelled() {
                        logger.error("cancelled");
                    }
                });
        SimpleHttpResponse simpleHttpResponse = httpResponseFuture.get(2000, TimeUnit.MILLISECONDS);
        Assertions.assertNotEquals(null, simpleHttpResponse);
        logger.error(simpleHttpResponse.getBodyText());
        Assertions.assertEquals("HTTP", simpleHttpResponse.getVersion().getProtocol());
        Assertions.assertEquals(2, simpleHttpResponse.getVersion().getMajor());
        logger.info("response code is {}", simpleHttpResponse.getCode());
        Assertions.assertEquals(200, simpleHttpResponse.getCode());
        logger.info("http response is: {}", simpleHttpResponse.getBodyText());
        Assertions.assertEquals("{\"message\":\"Hello,TRpc-Java\",\"status\":1,\"comments\":[\"first\",\"two\"]}",
                simpleHttpResponse.getBodyText());
    }

    @Test
    public void testGetWithJavaBean() throws Exception {
        String url =
                "http://localhost:18083/tencent.trpc.http.GreeterJavaBeanService/sayHello?message="
                        + URLEncoder.encode(
                        Base64.getEncoder().encodeToString("你好TRpc-Java".getBytes(Charsets.UTF_8)),
                        "UTF-8");
        SimpleHttpRequest simpleHttpRequest = SimpleHttpRequests.get(url);
        Future<SimpleHttpResponse> httpResponseFuture = httpAsyncClient.execute(simpleHttpRequest,
                new FutureCallback<SimpleHttpResponse>() {
                    @Override
                    public void completed(SimpleHttpResponse result) {
                        logger.debug(result.getBodyText());
                    }

                    @Override
                    public void failed(Exception ex) {
                        logger.error("failed, ex: ", ex);
                    }

                    @Override
                    public void cancelled() {
                        logger.error("cancelled");
                    }
                });
        SimpleHttpResponse simpleHttpResponse = httpResponseFuture.get(2000, TimeUnit.MILLISECONDS);
        Assertions.assertNotEquals(null, simpleHttpResponse);
        logger.error(simpleHttpResponse.getBodyText());
        Assertions.assertEquals("HTTP", simpleHttpResponse.getVersion().getProtocol());
        Assertions.assertEquals(2, simpleHttpResponse.getVersion().getMajor());
        logger.info("response code is {}", simpleHttpResponse.getCode());
        Assertions.assertEquals(200, simpleHttpResponse.getCode());
        logger.info("http response is: {}", simpleHttpResponse.getBodyText());
        Assertions.assertEquals("{\"message\":\"5L2g5aW9VFJwYy1KYXZh\"}",
                simpleHttpResponse.getBodyText());
    }

    @Test
    public void testGetMap() throws Exception {

        String url =
                "http://localhost:18083/tencent.trpc.http.GreeterService/sayHello?message="
                        + URLEncoder.encode(
                        Base64.getEncoder().encodeToString("你好TRpc-Java".getBytes(Charsets.UTF_8)),
                        "UTF-8");
        SimpleHttpRequest simpleHttpRequest = SimpleHttpRequests.get(url);
        Future<SimpleHttpResponse> httpResponseFuture = httpAsyncClient.execute(simpleHttpRequest,
                new FutureCallback<SimpleHttpResponse>() {
                    @Override
                    public void completed(SimpleHttpResponse result) {
                        logger.debug(result.getBodyText());
                    }

                    @Override
                    public void failed(Exception ex) {
                        logger.error("failed, ex: ", ex);
                    }

                    @Override
                    public void cancelled() {
                        logger.error("cancelled");
                    }
                });
        SimpleHttpResponse simpleHttpResponse = httpResponseFuture.get(2000, TimeUnit.MILLISECONDS);
        Assertions.assertNotEquals(null, simpleHttpResponse);
        logger.error(simpleHttpResponse.getBodyText());
        Assertions.assertEquals("HTTP", simpleHttpResponse.getVersion().getProtocol());
        Assertions.assertEquals(2, simpleHttpResponse.getVersion().getMajor());
        logger.info("response code is {}", simpleHttpResponse.getCode());
        Assertions.assertEquals(200, simpleHttpResponse.getCode());
        logger.info("http response is: {}", simpleHttpResponse.getBodyText());
        Assertions.assertEquals("{\"message\":\"Hello, 5L2g5aW9VFJwYy1KYXZh\"}",
                simpleHttpResponse.getBodyText());
    }

    @Test
    public void testUnknownMethod() throws Exception {

        String url =
                "http://localhost:18083/tencent.trpc.http.GreeterService/sayHello?message="
                        + URLEncoder.encode(
                        Base64.getEncoder().encodeToString("你好TRpc-Java".getBytes(Charsets.UTF_8)),
                        "UTF-8");
        SimpleHttpRequest simpleHttpRequest = SimpleHttpRequests.put(url);
        Future<SimpleHttpResponse> httpResponseFuture = httpAsyncClient.execute(simpleHttpRequest,
                new FutureCallback<SimpleHttpResponse>() {
                    @Override
                    public void completed(SimpleHttpResponse result) {
                        logger.debug(result.getBodyText());
                    }

                    @Override
                    public void failed(Exception ex) {
                        logger.error("failed, ex: ", ex);
                    }

                    @Override
                    public void cancelled() {
                        logger.error("cancelled");
                    }
                });
        SimpleHttpResponse simpleHttpResponse = httpResponseFuture.get(2000, TimeUnit.MILLISECONDS);
        Assertions.assertNotEquals(null, simpleHttpResponse);
        logger.error(simpleHttpResponse.getBodyText());
        Assertions.assertEquals("HTTP", simpleHttpResponse.getVersion().getProtocol());
        Assertions.assertEquals(2, simpleHttpResponse.getVersion().getMajor());
        logger.info("response code is {}", simpleHttpResponse.getCode());
        Assertions.assertEquals(405, simpleHttpResponse.getCode());
        logger.info("http response is: {}", simpleHttpResponse.getBodyText());
        ErrorResponse errorResponse = JsonUtils.fromJson(simpleHttpResponse.getBodyText(), ErrorResponse.class);
        Assertions.assertEquals("http method is not allow", errorResponse.getMessage());

    }
}
