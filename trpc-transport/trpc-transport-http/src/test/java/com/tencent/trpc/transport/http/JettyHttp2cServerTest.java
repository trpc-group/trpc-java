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

package com.tencent.trpc.transport.http;

import static com.tencent.trpc.transport.http.common.Constants.HTTP2_SCHEME;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tencent.trpc.core.common.config.ProtocolConfig;
import com.tencent.trpc.core.extension.ExtensionLoader;
import com.tencent.trpc.core.logger.Logger;
import com.tencent.trpc.core.logger.LoggerFactory;
import com.tencent.trpc.transport.http.common.ServletManager;
import com.tencent.trpc.transport.http.spi.HttpServerFactory;
import java.io.IOException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.apache.hc.client5.http.async.methods.SimpleHttpRequest;
import org.apache.hc.client5.http.async.methods.SimpleHttpRequests;
import org.apache.hc.client5.http.async.methods.SimpleHttpResponse;
import org.apache.hc.client5.http.impl.async.CloseableHttpAsyncClient;
import org.apache.hc.client5.http.impl.async.HttpAsyncClients;
import org.apache.hc.core5.concurrent.FutureCallback;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class JettyHttp2cServerTest {

    private static final Logger logger = LoggerFactory.getLogger(JettyHttp2cServerTest.class);

    private static HttpServer httpServer;

    private static CloseableHttpAsyncClient httpAsyncClient;

    private static CloseableHttpClient httpClient;

    @BeforeAll
    public static void beforeClass() throws InterruptedException {
        ProtocolConfig protocolConfig = ProtocolConfig.newInstance();
        protocolConfig.setIp("localhost");
        protocolConfig.setPort(18081);
        protocolConfig.setProtocol(HTTP2_SCHEME);
        protocolConfig.setTransporter("jetty");
        protocolConfig.setIoThreads(30);
        protocolConfig.setDefault();
        HttpServerFactory jettyServerFactory = ExtensionLoader
                .getExtensionLoader(HttpServerFactory.class)
                .getExtension(protocolConfig.getTransporter());
        HttpServer httpServer = jettyServerFactory.create(protocolConfig, (request, response) -> {
            String pathInfo = request.getPathInfo();
            if ("/".equals(pathInfo)) {
                response.setStatus(HttpStatus.OK_200);
                response.flushBuffer();
            } else {
                response.setStatus(HttpStatus.NOT_FOUND_404);
                response.flushBuffer();
            }
        });

        httpServer.open();
        assertTrue(httpServer.isBound());
        httpServer.toString();
        httpServer.getLocalAddress();
        ((AbstractHttpServer) httpServer).setConfig(httpServer.getConfig());
        ((AbstractHttpServer) httpServer)
                .setExecutor(((AbstractHttpServer) httpServer).getExecutor());
        new ServletManager().getServletContext(0);
        JettyHttp2cServerTest.httpServer = httpServer;

        httpAsyncClient = HttpAsyncClients.customHttp2().build();
        httpAsyncClient.start();
        Thread.sleep(1000);

        httpClient = HttpClients.custom().build();
    }

    @AfterAll
    public static void afterClass() {
        if (httpAsyncClient != null) {
            try {
                httpAsyncClient.close();
            } catch (IOException e) {
                logger.warn("close httpAsyncClient error: {}", e);
            }
        }
        if (httpServer != null) {
            httpServer.close();
            httpServer = null;
        }
    }

    @Test
    public void testNormalRequestWithHttp2() throws Exception {
        SimpleHttpRequest simpleHttpRequest = SimpleHttpRequests.get("http://localhost:18081/");
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
        SimpleHttpResponse simpleHttpResponse = httpResponseFuture.get(10000, TimeUnit.MILLISECONDS);
        Assertions.assertNotEquals(null, simpleHttpResponse);
        logger.error(simpleHttpResponse.getBodyText());
        Assertions.assertEquals("HTTP", simpleHttpResponse.getVersion().getProtocol());
        Assertions.assertEquals(2, simpleHttpResponse.getVersion().getMajor());
        logger.info("response code is {}", simpleHttpResponse.getCode());
        Assertions.assertEquals(200, simpleHttpResponse.getCode());
        logger.info("http response is: {}", simpleHttpResponse.getBodyText());
        Assertions.assertEquals("", simpleHttpResponse.getBodyText());
    }

    @Test
    public void testNotExistRequestWithHttp2() throws Exception {
        SimpleHttpRequest simpleHttpRequest = SimpleHttpRequests
                .get("http://localhost:18081/not_exist_path");
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
        SimpleHttpResponse simpleHttpResponse = httpResponseFuture.get(10000, TimeUnit.MILLISECONDS);
        Assertions.assertNotEquals(null, simpleHttpResponse);
        logger.error(simpleHttpResponse.getBodyText());
        Assertions.assertEquals("HTTP", simpleHttpResponse.getVersion().getProtocol());
        Assertions.assertEquals(2, simpleHttpResponse.getVersion().getMajor());
        logger.info("response code is {}", simpleHttpResponse.getCode());
        Assertions.assertEquals(404, simpleHttpResponse.getCode());
        logger.info("http response is: {}", simpleHttpResponse.getBodyText());
        Assertions.assertEquals("", simpleHttpResponse.getBodyText());
    }

    @Test
    public void testNormalRequestWithHttp() throws Exception {

        HttpGet httpGet = new HttpGet("http://localhost:18081/");
        CloseableHttpResponse httpResponse = httpClient.execute(httpGet);

        int responseCode = httpResponse.getStatusLine().getStatusCode();
        logger.info("response code is {}", responseCode);
        Assertions.assertEquals(responseCode, 200);

        Assertions.assertEquals(httpResponse.getProtocolVersion().getProtocol(), "HTTP");
        Assertions.assertEquals(httpResponse.getProtocolVersion().getMajor(), 1);

    }

    @Test
    public void testNotExistRequestWithHttp1() throws Exception {

        HttpGet httpGet = new HttpGet("http://localhost:18081/not_exist_path");
        CloseableHttpResponse httpResponse = httpClient.execute(httpGet);

        int responseCode = httpResponse.getStatusLine().getStatusCode();
        logger.info("response code is {}", responseCode);
        Assertions.assertEquals(responseCode, 404);

        Assertions.assertEquals(httpResponse.getProtocolVersion().getProtocol(), "HTTP");
        Assertions.assertEquals(httpResponse.getProtocolVersion().getMajor(), 1);

    }

}
