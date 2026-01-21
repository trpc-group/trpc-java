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

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tencent.trpc.core.common.config.ProtocolConfig;
import com.tencent.trpc.core.extension.ExtensionLoader;
import com.tencent.trpc.core.logger.Logger;
import com.tencent.trpc.core.logger.LoggerFactory;
import com.tencent.trpc.transport.http.common.ServletManager;
import com.tencent.trpc.transport.http.spi.HttpServerFactory;
import java.net.HttpURLConnection;
import java.net.URL;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class JettyServerTest {

    private static final Logger logger = LoggerFactory.getLogger(JettyServerTest.class);

    private static HttpServer httpServer;

    @BeforeAll
    public static void beforeClass() {
        ProtocolConfig protocolConfig = ProtocolConfig.newInstance();
        protocolConfig.setIp("localhost");
        protocolConfig.setPort(18080);
        protocolConfig.setProtocol("http");
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
            } else {
                response.sendError(HttpStatus.NOT_FOUND_404);
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
        JettyServerTest.httpServer = httpServer;
    }

    @AfterAll
    public static void afterClass() {
        if (httpServer != null) {
            httpServer.open();
            httpServer.close();
            httpServer = null;
        }
    }

    @Test
    public void testNormalRequest() throws Exception {
        URL url = new URL("http://localhost:18080/");
        HttpURLConnection connection = null;

        try {
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(200);
            connection.setReadTimeout(1000);
            connection.setDoOutput(false);
            connection.setDoInput(true);

            int responseCode = connection.getResponseCode();
            logger.info("response code is {}", responseCode);

            Assertions.assertEquals(responseCode, 200);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    @Test
    public void testNotExistRequest() throws Exception {
        URL url = new URL("http://localhost:18080/not_exist_path");
        HttpURLConnection connection = null;

        try {
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(200);
            connection.setReadTimeout(1000);
            connection.setDoOutput(false);
            connection.setDoInput(true);

            int responseCode = connection.getResponseCode();
            logger.info("response code is {}", responseCode);

            Assertions.assertEquals(responseCode, 404);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

}
