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

import static com.tencent.trpc.transport.http.common.Constants.HTTP_SCHEME;
import static com.tencent.trpc.transport.http.common.Constants.KEYSTORE_PASS;
import static com.tencent.trpc.transport.http.common.Constants.KEYSTORE_PATH;
import static org.junit.Assert.assertTrue;

import com.tencent.trpc.core.common.config.ProtocolConfig;
import com.tencent.trpc.core.extension.ExtensionLoader;
import com.tencent.trpc.core.logger.Logger;
import com.tencent.trpc.core.logger.LoggerFactory;
import com.tencent.trpc.transport.http.common.ServletManager;
import com.tencent.trpc.transport.http.spi.HttpServerFactory;
import java.io.File;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
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
import org.apache.hc.core5.http2.HttpVersionPolicy;
import org.apache.hc.core5.http2.ssl.ConscryptClientTlsStrategy;
import org.apache.hc.core5.ssl.SSLContexts;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class JettyHttpsServerTest {

    private static final Logger logger = LoggerFactory.getLogger(JettyHttpsServerTest.class);

    private static HttpServer httpServer;

    private static CloseableHttpAsyncClient httpAsyncClient;

    @BeforeClass
    public static void beforeClass() throws CertificateException, NoSuchAlgorithmException,
            KeyStoreException, IOException, KeyManagementException {
        ProtocolConfig protocolConfig = ProtocolConfig.newInstance();
        protocolConfig.setIp("localhost");
        protocolConfig.setPort(18083);
        protocolConfig.setProtocol(HTTP_SCHEME);
        protocolConfig.setTransporter("jetty");
        String path = JettyHttpsServerTest.class.getClassLoader().getResource("keystore.jks")
                .getFile();
        protocolConfig.getExtMap().put(KEYSTORE_PATH, new File(path).getAbsolutePath());
        protocolConfig.getExtMap().put(KEYSTORE_PASS, "init234");
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
        JettyHttpsServerTest.httpServer = httpServer;

        String keyStorePath = String.valueOf(path);
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

    @AfterClass
    public static void afterClass() {
        if (httpServer != null) {
            httpServer.open();
            httpServer.close();
            httpServer = null;
        }
        if (httpAsyncClient != null) {
            try {
                httpAsyncClient.close();
            } catch (IOException e) {
                logger.warn("close httpAsyncClient error: {}", e);
            }
        }
    }

    @Test
    public void testNormalRequestWithHttps() throws Exception {
        SimpleHttpRequest simpleHttpRequest = SimpleHttpRequests.get("https://localhost:18083/");
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
        Assert.assertEquals("", simpleHttpResponse.getBodyText());
    }

    @Test
    public void testNotExistRequestWithHttps() throws Exception {
        SimpleHttpRequest simpleHttpRequest = SimpleHttpRequests
                .get("https://localhost:18083/not_exist_path");
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
        Assert.assertEquals("", simpleHttpResponse.getBodyText());
    }

}
