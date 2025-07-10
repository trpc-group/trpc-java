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
import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.SSLContext;
import org.apache.hc.client5.http.async.methods.SimpleHttpRequest;
import org.apache.hc.client5.http.async.methods.SimpleHttpRequests;
import org.apache.hc.client5.http.async.methods.SimpleHttpResponse;
import org.apache.hc.client5.http.impl.async.CloseableHttpAsyncClient;
import org.apache.hc.client5.http.impl.async.HttpAsyncClients;
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManager;
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManagerBuilder;
import org.apache.hc.core5.concurrent.FutureCallback;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.http2.HttpVersionPolicy;
import org.apache.hc.core5.http2.ssl.ConscryptClientTlsStrategy;
import org.apache.hc.core5.ssl.SSLContexts;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import tests.service.GreeterJavaBeanService;
import tests.service.GreeterJsonService;
import tests.service.GreeterService;
import tests.service.HelloRequestProtocol.HelloRequest;
import tests.service.impl1.GreeterJavaBeanServiceImpl;
import tests.service.impl1.GreeterJsonServiceImpl1;
import tests.service.impl1.GreeterServiceImpl1;

public class HttpsRpcServerTest {

    private static final Logger logger = LoggerFactory.getLogger(HttpsRpcServerTest.class);

    private static ServerConfig serverConfig;

    private static CloseableHttpAsyncClient httpAsyncClient;

    @BeforeClass
    public static void startHttpServer() throws CertificateException, NoSuchAlgorithmException,
            KeyStoreException, IOException, KeyManagementException {
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
                NetUtils.LOCAL_HOST, 18094, HTTP_SCHEME, "jetty");
        providers.put(serviceConfig1.getName(), serviceConfig1);

        ServiceConfig serviceConfig2 = getServiceConfig(gjspc, "test.server2",
                NetUtils.LOCAL_HOST, 18094, HTTP_SCHEME, "jetty");
        providers.put(serviceConfig2.getName(), serviceConfig2);

        ServiceConfig serviceConfig3 = getServiceConfig(javaBeanService, "test.server3",
                NetUtils.LOCAL_HOST, 18094, HTTP_SCHEME, "jetty");
        providers.put(serviceConfig3.getName(), serviceConfig3);

        ServerConfig sc = new ServerConfig();
        sc.setServiceMap(providers);
        sc.setApp("http-test-app");
        sc.setLocalIp("127.0.0.1");
        sc.init();

        serverConfig = sc;

        String path = HttpsRpcServerTest.class.getClassLoader().getResource("keystore.jks")
                .getFile();
        String keyStorePath = new File(path).getAbsolutePath();
        String keyStorePass = "init234";
        final SSLContext sslContext = SSLContexts.custom()
                .loadTrustMaterial(new File(keyStorePath), keyStorePass.toCharArray())
                .build();
        final PoolingAsyncClientConnectionManager cm = PoolingAsyncClientConnectionManagerBuilder
                .create().useSystemProperties()
                .setTlsStrategy(new ConscryptClientTlsStrategy(sslContext))
                .build();
        httpAsyncClient = HttpAsyncClients.custom()
                .setVersionPolicy(HttpVersionPolicy.FORCE_HTTP_1).setConnectionManager(cm)
                .build();
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
        String path = HttpsRpcServerTest.class.getClassLoader().getResource("keystore.jks")
                .getFile();
        serviceConfig.getExtMap().put(KEYSTORE_PATH, new File(path).getAbsolutePath());
        serviceConfig.getExtMap().put(KEYSTORE_PASS, "init234");
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

    @Test
    public void testNotFoundService() throws Exception {
        SimpleHttpRequest simpleHttpRequest =
                SimpleHttpRequests
                        .get("https://localhost:18094/NotExistService/NotExistMethod");
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
        Assert.assertNotEquals(null, simpleHttpResponse);
        logger.error(simpleHttpResponse.getBodyText());
        Assert.assertEquals("HTTP", simpleHttpResponse.getVersion().getProtocol());
        Assert.assertEquals(1, simpleHttpResponse.getVersion().getMajor());
        logger.info("response code is {}", simpleHttpResponse.getCode());
        Assert.assertEquals(404, simpleHttpResponse.getCode());
        logger.info("http response is: {}", simpleHttpResponse.getBodyText());
        ErrorResponse errorResponse = JsonUtils.fromJson(simpleHttpResponse.getBodyText(), ErrorResponse.class);
        Assert.assertEquals("not found service", errorResponse.getMessage());

    }

    @Test
    public void testPostByJson() throws Exception {
        SimpleHttpRequest simpleHttpRequest =
                SimpleHttpRequests
                        .post("https://localhost:18094/tencent.trpc.http.GreeterService/sayHello");
        simpleHttpRequest.setHeader(HttpHeaders.CONTENT_TYPE, HttpConstants.CONTENT_TYPE_JSON);
        Map<String, String> body = new HashMap<>();
        body.put("message", "TRpc-Java!");
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
        Assert.assertNotEquals(null, simpleHttpResponse);
        logger.error(simpleHttpResponse.getBodyText());
        Assert.assertEquals("HTTP", simpleHttpResponse.getVersion().getProtocol());
        Assert.assertEquals(1, simpleHttpResponse.getVersion().getMajor());
        logger.info("response code is {}", simpleHttpResponse.getCode());
        Assert.assertEquals(200, simpleHttpResponse.getCode());
        logger.info("http response is: {}", simpleHttpResponse.getBodyText());
        Assert.assertEquals("{\"message\":\"Hello, TRpc-Java!\"}",
                simpleHttpResponse.getBodyText());
    }

    @Test
    public void testPostByPB() throws Exception {
        SimpleHttpRequest simpleHttpRequest =
                SimpleHttpRequests
                        .post("https://localhost:18094/tencent.trpc.http.GreeterService/sayHello");
        simpleHttpRequest.setHeader(HttpHeaders.CONTENT_TYPE, HttpConstants.CONTENT_TYPE_PROTOBUF);

        HelloRequest.Builder reqBuilder = HelloRequest.newBuilder();
        reqBuilder.setMessage("TRpc-Java!");

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
        Assert.assertNotEquals(null, simpleHttpResponse);
        logger.error(simpleHttpResponse.getBodyText());
        Assert.assertEquals("HTTP", simpleHttpResponse.getVersion().getProtocol());
        Assert.assertEquals(1, simpleHttpResponse.getVersion().getMajor());
        logger.info("response code is {}", simpleHttpResponse.getCode());
        Assert.assertEquals(200, simpleHttpResponse.getCode());
        logger.info("http response is: {}", simpleHttpResponse.getBodyText());
        Assert.assertEquals("{\"message\":\"Hello, TRpc-Java!\"}",
                simpleHttpResponse.getBodyText());
    }

    @Test
    public void testPostMapByJson() throws Exception {
        SimpleHttpRequest simpleHttpRequest =
                SimpleHttpRequests
                        .post("https://localhost:18094/tencent.trpc.http.GreeterJsonService/sayHelloJson");
        simpleHttpRequest.setHeader(HttpHeaders.CONTENT_TYPE, HttpConstants.CONTENT_TYPE_JSON);
        Map<String, String> body = new HashMap<>();
        body.put("message", "TRpc-Java!");
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
        Assert.assertNotEquals(null, simpleHttpResponse);
        logger.error(simpleHttpResponse.getBodyText());
        Assert.assertEquals("HTTP", simpleHttpResponse.getVersion().getProtocol());
        Assert.assertEquals(1, simpleHttpResponse.getVersion().getMajor());
        logger.info("response code is {}", simpleHttpResponse.getCode());
        Assert.assertEquals(200, simpleHttpResponse.getCode());
        logger.info("http response is: {}", simpleHttpResponse.getBodyText());
        Assert.assertEquals("{\"message\":\"Hi:TRpc-Java!\"}",
                simpleHttpResponse.getBodyText());

    }

    @Test
    public void testGet() throws Exception {
        String url =
                "https://localhost:18094/tencent.trpc.http.GreeterService/sayHello?message=TRpc-Java";
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
        Assert.assertNotEquals(null, simpleHttpResponse);
        logger.error(simpleHttpResponse.getBodyText());
        Assert.assertEquals("HTTP", simpleHttpResponse.getVersion().getProtocol());
        Assert.assertEquals(1, simpleHttpResponse.getVersion().getMajor());
        logger.info("response code is {}", simpleHttpResponse.getCode());
        Assert.assertEquals(200, simpleHttpResponse.getCode());
        logger.info("http response is: {}", simpleHttpResponse.getBodyText());
        Assert.assertEquals("{\"message\":\"Hello, TRpc-Java\"}",
                simpleHttpResponse.getBodyText());
    }

    @Test
    public void testConvertBeanWithGetMethod() throws Exception {
        String url =
                "https://localhost:18094/tencent.trpc.http.GreeterService/sayHelloNonPbType?"
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
        Assert.assertNotEquals(null, simpleHttpResponse);
        logger.error(simpleHttpResponse.getBodyText());
        Assert.assertEquals("HTTP", simpleHttpResponse.getVersion().getProtocol());
        Assert.assertEquals(1, simpleHttpResponse.getVersion().getMajor());
        logger.info("response code is {}", simpleHttpResponse.getCode());
        Assert.assertEquals(200, simpleHttpResponse.getCode());
        logger.info("http response is: {}", simpleHttpResponse.getBodyText());
        Assert.assertEquals("{\"message\":\"Hello,TRpc-Java\",\"status\":1,\"comments\":[\"first\",\"two\"]}",
                simpleHttpResponse.getBodyText());
    }

    @Test
    public void testGetWithJavaBean() throws Exception {
        String url =
                "https://localhost:18094/tencent.trpc.http.GreeterJavaBeanService/sayHello?message=TRpc-Java";
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
        Assert.assertNotEquals(null, simpleHttpResponse);
        logger.error(simpleHttpResponse.getBodyText());
        Assert.assertEquals("HTTP", simpleHttpResponse.getVersion().getProtocol());
        Assert.assertEquals(1, simpleHttpResponse.getVersion().getMajor());
        logger.info("response code is {}", simpleHttpResponse.getCode());
        Assert.assertEquals(200, simpleHttpResponse.getCode());
        logger.info("http response is: {}", simpleHttpResponse.getBodyText());
        Assert.assertEquals("{\"message\":\"TRpc-Java\"}", simpleHttpResponse.getBodyText());
    }

    @Test
    public void testGetMap() throws Exception {

        String url =
                "https://localhost:18094/tencent.trpc.http.GreeterService/sayHello?message=TRpc-Java";
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
        Assert.assertNotEquals(null, simpleHttpResponse);
        logger.error(simpleHttpResponse.getBodyText());
        Assert.assertEquals("HTTP", simpleHttpResponse.getVersion().getProtocol());
        Assert.assertEquals(1, simpleHttpResponse.getVersion().getMajor());
        logger.info("response code is {}", simpleHttpResponse.getCode());
        Assert.assertEquals(200, simpleHttpResponse.getCode());
        logger.info("http response is: {}", simpleHttpResponse.getBodyText());
        Assert.assertEquals("{\"message\":\"Hello, TRpc-Java\"}",
                simpleHttpResponse.getBodyText());
    }

    @Test
    public void testUnknownMethod() throws Exception {

        String url =
                "https://localhost:18094/tencent.trpc.http.GreeterService/sayHello?message="
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
        Assert.assertNotEquals(null, simpleHttpResponse);
        logger.error(simpleHttpResponse.getBodyText());
        Assert.assertEquals("HTTP", simpleHttpResponse.getVersion().getProtocol());
        Assert.assertEquals(1, simpleHttpResponse.getVersion().getMajor());
        logger.info("response code is {}", simpleHttpResponse.getCode());
        Assert.assertEquals(405, simpleHttpResponse.getCode());
        logger.info("http response is: {}", simpleHttpResponse.getBodyText());
        ErrorResponse errorResponse = JsonUtils.fromJson(simpleHttpResponse.getBodyText(), ErrorResponse.class);
        Assert.assertEquals("http method is not allow", errorResponse.getMessage());

    }
}
