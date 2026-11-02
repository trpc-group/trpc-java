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
import jakarta.servlet.AsyncContext;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
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
        protocolConfig.setIp("127.0.0.1");
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
            } else if ("/echo".equals(pathInfo)) {
                response.getWriter().write(request.getMethod() + ":" + pathInfo + ":"
                        + request.getParameter("id") + ":" + request.getReader().readLine());
            } else if ("/async".equals(pathInfo)) {
                AsyncContext asyncContext = request.startAsync();
                asyncContext.start(() -> {
                    try {
                        asyncContext.getResponse().getWriter().write("async response");
                    } catch (IOException e) {
                        throw new IllegalStateException(e);
                    } finally {
                        asyncContext.complete();
                    }
                });
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

    @Test
    public void testPostRequestBody() throws Exception {
        HttpURLConnection connection = (HttpURLConnection)
                new URL("http://127.0.0.1:18080/echo?id=42").openConnection();
        try {
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "text/plain; charset=UTF-8");
            connection.setConnectTimeout(1000);
            connection.setReadTimeout(5000);
            connection.setDoOutput(true);
            try (var output = connection.getOutputStream()) {
                output.write("request body".getBytes(StandardCharsets.UTF_8));
            }
            Assertions.assertEquals(200, connection.getResponseCode());
            try (var input = connection.getInputStream()) {
                Assertions.assertEquals("POST:/echo:42:request body",
                        new String(input.readAllBytes(), StandardCharsets.UTF_8));
            }
        } finally {
            connection.disconnect();
        }
    }

    @Test
    public void testAsyncRequest() throws Exception {
        HttpURLConnection connection = (HttpURLConnection)
                new URL("http://127.0.0.1:18080/async").openConnection();
        try {
            connection.setConnectTimeout(1000);
            connection.setReadTimeout(5000);
            Assertions.assertEquals(200, connection.getResponseCode());
            try (var input = connection.getInputStream()) {
                Assertions.assertEquals("async response",
                        new String(input.readAllBytes(), StandardCharsets.UTF_8));
            }
        } finally {
            connection.disconnect();
        }
    }

}
