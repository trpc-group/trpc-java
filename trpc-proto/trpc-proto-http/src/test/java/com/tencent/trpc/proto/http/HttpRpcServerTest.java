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

import static com.tencent.trpc.transport.http.common.Constants.HTTP_SCHEME;

import com.tencent.trpc.core.common.ConfigManager;
import com.tencent.trpc.core.common.config.ProviderConfig;
import com.tencent.trpc.core.common.config.ServerConfig;
import com.tencent.trpc.core.common.config.ServiceConfig;
import com.tencent.trpc.core.logger.Logger;
import com.tencent.trpc.core.logger.LoggerFactory;
import com.tencent.trpc.core.utils.Charsets;
import com.tencent.trpc.core.utils.JsonUtils;
import com.tencent.trpc.core.utils.NetUtils;
import com.tencent.trpc.proto.http.common.HttpConstants;
import com.tencent.trpc.proto.http.util.StreamUtils;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import tests.service.GreeterJavaBeanService;
import tests.service.GreeterJsonService;
import tests.service.GreeterParameterizedService;
import tests.service.GreeterService;
import tests.service.HelloRequestProtocol.HelloRequest;
import tests.service.TestBeanConvertWithGetMethodRsp;
import tests.service.impl1.GreeterJavaBeanServiceImpl;
import tests.service.impl1.GreeterJsonServiceImpl1;
import tests.service.impl1.GreeterParameterizedServiceImpl;
import tests.service.impl1.GreeterServiceImpl1;

public class HttpRpcServerTest {

    private static final Logger logger = LoggerFactory.getLogger(HttpRpcServerTest.class);

    private static ServerConfig serverConfig;

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

        ProviderConfig<GreeterParameterizedService> parameterizedService = new ProviderConfig<>();
        parameterizedService.setServiceInterface(GreeterParameterizedService.class);
        parameterizedService.setRef(new GreeterParameterizedServiceImpl());

        HashMap<String, ServiceConfig> providers = new HashMap<>();
        ServiceConfig serviceConfig1 = getServiceConfig(gspc, "test.server1", NetUtils.LOCAL_HOST,
                18090, HTTP_SCHEME, "jetty");
        providers.put(serviceConfig1.getName(), serviceConfig1);
        ServiceConfig serviceConfig2 = getServiceConfig(gjspc, "test.server2", NetUtils.LOCAL_HOST,
                18090, HTTP_SCHEME, "jetty");
        providers.put(serviceConfig2.getName(), serviceConfig2);
        ServiceConfig serviceConfig3 = getServiceConfig(javaBeanService, "test.server3",
                NetUtils.LOCAL_HOST,
                18090, HTTP_SCHEME, "jetty");
        providers.put(serviceConfig3.getName(), serviceConfig3);

        ServiceConfig serviceConfig4 = getServiceConfig(parameterizedService, "test.server4",
                NetUtils.LOCAL_HOST,
                18090, HTTP_SCHEME, "jetty");
        providers.put(serviceConfig4.getName(), serviceConfig4);

        ServerConfig sc = new ServerConfig();
        sc.setServiceMap(providers);
        sc.setApp("http-test-app");
        sc.setLocalIp("127.0.0.1");
        sc.init();

        serverConfig = sc;
    }

    private static ServiceConfig getServiceConfig(ProviderConfig<?> gspc, String name,
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
        URL url = new URL(
                "http://localhost:18090/NotExistService/NotExistMethod");
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
    public void testPostByJson() throws Exception {
        URL url = new URL(
                "http://localhost:18090/tencent.trpc.http.GreeterService/sayHello");
        HttpURLConnection connection = null;
        OutputStream out = null;
        InputStream in = null;

        try {
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(200);
            connection.setReadTimeout(1000);
            connection.setDoOutput(true);
            connection.setDoInput(true);
            connection.setRequestProperty("Content-Type", HttpConstants.CONTENT_TYPE_JSON);

            out = connection.getOutputStream();
            String message = Base64.getEncoder()
                    .encodeToString("TRpc-Java!".getBytes(Charsets.UTF_8));
            System.out.println(message);
            out.write(("{\"message\":\"" + message + "\"}").getBytes(StandardCharsets.UTF_8));
            out.close();

            int responseCode = connection.getResponseCode();
            logger.info("response code is {}", responseCode);

            Assertions.assertEquals(200, responseCode);

            in = connection.getInputStream();

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            StreamUtils.copy(in, bos);

            logger.info("http response is: {}",
                    new String(bos.toByteArray(), StandardCharsets.UTF_8));
        } finally {
            if (out != null) {
                out.close();
            }

            if (in != null) {
                in.close();
            }

            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    @Test
    public void testPostByPBWithoutContentType() throws Exception {
        URL url = new URL(
                "http://localhost:18090/tencent.trpc.http.GreeterService/sayHello");
        HttpURLConnection connection = null;

        try {
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(200);
            connection.setReadTimeout(1000);
            connection.setDoOutput(false);
            connection.setDoInput(true);

            HelloRequest.Builder reqBuilder = HelloRequest.newBuilder();
            reqBuilder.setMessage("TRpc-Java!");

            int responseCode = connection.getResponseCode();
            logger.info("response code is {}", responseCode);

            Assertions.assertEquals(405, responseCode);

        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    @Test
    public void testPostByPB() throws Exception {
        URL url = new URL(
                "http://localhost:18090/tencent.trpc.http.GreeterService/sayHello");
        HttpURLConnection connection = null;
        OutputStream out = null;
        InputStream in = null;

        try {
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(200);
            connection.setReadTimeout(1000);
            connection.setDoOutput(true);
            connection.setDoInput(true);
            connection.setRequestProperty("Content-Type", HttpConstants.CONTENT_TYPE_PROTOBUF);

            HelloRequest.Builder reqBuilder = HelloRequest.newBuilder();
            reqBuilder.setMessage("TRpc-Java!");

            out = connection.getOutputStream();
            out.write(reqBuilder.build().toByteArray());
            out.close();

            int responseCode = connection.getResponseCode();
            logger.info("response code is {}", responseCode);

            Assertions.assertEquals(responseCode, 200);

            in = connection.getInputStream();

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            StreamUtils.copy(in, bos);

            logger.info("http response is: {}",
                    new String(bos.toByteArray(), StandardCharsets.UTF_8));
        } finally {
            if (out != null) {
                out.close();
            }

            if (in != null) {
                in.close();
            }

            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    @Test
    public void testPostMapByJson() throws Exception {
        URL url = new URL(
                "http://localhost:18090/tencent.trpc.http.GreeterJsonService/sayHelloJson");
        HttpURLConnection connection = null;
        OutputStream out = null;
        InputStream in = null;

        try {
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(200);
            connection.setReadTimeout(1000);
            connection.setDoOutput(true);
            connection.setDoInput(true);
            connection.setRequestProperty("Content-Type", HttpConstants.CONTENT_TYPE_JSON);

            out = connection.getOutputStream();
            String message = Base64.getEncoder()
                    .encodeToString("TRpc-Java!".getBytes(Charsets.UTF_8));
            System.out.println(message);
            out.write(("{\"message\":\"" + message + "\"}").getBytes(StandardCharsets.UTF_8));
            out.close();

            int responseCode = connection.getResponseCode();
            logger.info("response code is {}", responseCode);

            Assertions.assertEquals(responseCode, 200);

            in = connection.getInputStream();

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            StreamUtils.copy(in, bos);

            logger.info("http response is: {}",
                    new String(bos.toByteArray(), StandardCharsets.UTF_8));
        } finally {
            if (out != null) {
                out.close();
            }

            if (in != null) {
                in.close();
            }

            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    @Test
    public void testGet() throws Exception {
        String strUrl =
                "http://localhost:18090/tencent.trpc.http.GreeterService/sayHello?message=";
        URL url = new URL(strUrl + URLEncoder.encode(
                Base64.getEncoder().encodeToString("你好TRpc-Java".getBytes(Charsets.UTF_8)),
                "UTF-8"));
        HttpURLConnection connection = null;
        InputStream in = null;

        try {
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(200);
            connection.setReadTimeout(1000);
            connection.setDoOutput(false);
            connection.setDoInput(true);

            int responseCode = connection.getResponseCode();
            logger.info("response code is {}", responseCode);

            Assertions.assertEquals(200, responseCode);

            in = connection.getInputStream();

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            StreamUtils.copy(in, bos);

            logger.info("http response is: {}",
                    new String(bos.toByteArray(), StandardCharsets.UTF_8));
        } finally {
            if (in != null) {
                in.close();
            }

            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    @Test
    public void testConvertBeanWithGetMethod() throws Exception {
        String strUrl =
                "http://localhost:18090/tencent.trpc.http"
                        + ".GreeterService/sayHelloNonPbType?message=TRpc-Java&status=1&comments=first&comments=two";
        URL url = new URL(strUrl);
        HttpURLConnection connection = null;
        InputStream in = null;

        try {
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(200);
            connection.setReadTimeout(1000);
            connection.setDoOutput(false);
            connection.setDoInput(true);

            int responseCode = connection.getResponseCode();
            logger.info("response code is {}", responseCode);

            Assertions.assertEquals(200, responseCode);

            in = connection.getInputStream();

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            StreamUtils.copy(in, bos);

            String messageContent = new String(bos.toByteArray(), StandardCharsets.UTF_8);
            TestBeanConvertWithGetMethodRsp response = JsonUtils.fromJson(messageContent,
                    TestBeanConvertWithGetMethodRsp.class);
            Assertions.assertEquals(1, response.getStatus());
            Assertions.assertEquals("Hello,TRpc-Java", response.getMessage());
            Assertions.assertArrayEquals(new String[]{"first", "two"}, response.getComments());

            logger.info("http response is: {}",
                    new String(bos.toByteArray(), StandardCharsets.UTF_8));
        } finally {
            if (in != null) {
                in.close();
            }

            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    @Test
    public void testGetWithDefaultBasePath() throws Exception {
        String strUrl =
                "http://localhost:18090/tencent.trpc.http.GreeterService/sayHello?message=";
        URL url = new URL(strUrl + URLEncoder.encode(
                Base64.getEncoder().encodeToString("你好TRpc-Java".getBytes(Charsets.UTF_8)),
                "UTF-8"));
        HttpURLConnection connection = null;
        InputStream in = null;

        try {
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(200);
            connection.setReadTimeout(1000);
            connection.setDoOutput(false);
            connection.setDoInput(true);

            int responseCode = connection.getResponseCode();
            logger.info("response code is {}", responseCode);

            Assertions.assertEquals(200, responseCode);

            in = connection.getInputStream();

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            StreamUtils.copy(in, bos);

            logger.info("http response is: {}",
                    new String(bos.toByteArray(), StandardCharsets.UTF_8));
        } finally {
            if (in != null) {
                in.close();
            }

            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    @Test
    public void testGetMap() throws Exception {
        String strUrl =
                "http://localhost:18090/tencent.trpc.http.GreeterService/sayHello?message=";
        URL url = new URL(strUrl + URLEncoder.encode(
                Base64.getEncoder().encodeToString("你好TRpc-Java".getBytes(Charsets.UTF_8)),
                "UTF-8"));
        HttpURLConnection connection = null;
        InputStream in = null;

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

            in = connection.getInputStream();

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            StreamUtils.copy(in, bos);

            logger.info("http response is: {}",
                    new String(bos.toByteArray(), StandardCharsets.UTF_8));
        } finally {
            if (in != null) {
                in.close();
            }

            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    @Test
    public void testUnknownMethod() throws Exception {
        String strUrl =
                "http://localhost:18090/tencent.trpc.http.GreeterService/sayHello?message=";
        URL url = new URL(strUrl + URLEncoder.encode(
                Base64.getEncoder().encodeToString("你好TRpc-Java".getBytes(Charsets.UTF_8)),
                "UTF-8"));
        HttpURLConnection connection = null;

        try {
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("PUT");
            connection.setConnectTimeout(200);
            connection.setReadTimeout(1000);
            connection.setDoOutput(false);
            connection.setDoInput(true);

            int responseCode = connection.getResponseCode();
            logger.info("response code is {}", responseCode);

            Assertions.assertEquals(405, responseCode);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    @Test
    public void testGetWithJavaBean() throws Exception {
        String strUrl =
                "http://localhost:18090/tencent.trpc.http.GreeterJavaBeanService/sayHello?message=";
        URL url = new URL(strUrl + "hello");
        HttpURLConnection connection = null;
        InputStream in = null;

        try {
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(200);
            connection.setReadTimeout(1000);
            connection.setDoOutput(false);
            connection.setDoInput(true);

            int responseCode = connection.getResponseCode();
            logger.info("response code is {}", responseCode);

            Assertions.assertEquals(200, responseCode);

            in = connection.getInputStream();

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            StreamUtils.copy(in, bos);

            logger.error("http response is: {}",
                    new String(bos.toByteArray(), StandardCharsets.UTF_8));
        } finally {
            if (in != null) {
                in.close();
            }

            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    @Test
    public void testGetWithParameterizedBean() throws Exception {
        String strUrl =
                "http://localhost:18090/tencent.trpc.http.GreeterParameterizedService/sayHelloParameterized?message=";
        URL url = new URL(strUrl + "hello");
        HttpURLConnection connection = null;
        InputStream in = null;

        try {
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(200);
            connection.setReadTimeout(1000);
            connection.setDoOutput(false);
            connection.setDoInput(true);

            int responseCode = connection.getResponseCode();
            logger.info("response code is {}", responseCode);

            Assertions.assertEquals(200, responseCode);

            in = connection.getInputStream();

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            StreamUtils.copy(in, bos);

            logger.error("http response is: {}",
                    new String(bos.toByteArray(), StandardCharsets.UTF_8));
        } finally {
            if (in != null) {
                in.close();
            }

            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    @Test
    public void testPostWithJavaBean() throws Exception {
        String strUrl =
                "http://localhost:18090/tencent.trpc.http.GreeterJavaBeanService/sayHello";
        URL url = new URL(strUrl);
        HttpURLConnection connection = null;
        OutputStream out = null;
        InputStream in = null;

        try {
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(200);
            connection.setReadTimeout(1000);
            connection.setDoOutput(true);
            connection.setDoInput(true);
            connection.setRequestProperty("Content-Type", HttpConstants.CONTENT_TYPE_JSON);

            out = connection.getOutputStream();
            Map<String, Object> data = new HashMap<>();
            Map<String, Object> innerData = new HashMap<>();
            innerData.put("msg", "trpc");
            data.put("message", "hello");
            data.put("innerMsg", innerData);

            String msg = JsonUtils.toJson(data);
            System.out.println(msg);
            out.write(msg.getBytes(StandardCharsets.UTF_8));
            out.close();

            int responseCode = connection.getResponseCode();
            logger.info("response code is {}", responseCode);

            Assertions.assertEquals(200, responseCode);

            in = connection.getInputStream();

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            StreamUtils.copy(in, bos);

            String rsp = new String(bos.toByteArray(), StandardCharsets.UTF_8);
            logger.error("http response is: {}", rsp);

            Map<String, Object> rspData = JsonUtils.fromJson(rsp, Map.class);

            Assertions.assertEquals("hello", rspData.get("message"));
            Assertions.assertEquals("trpc", ((Map) rspData.get("innerMsg")).get("msg"));
        } finally {
            if (in != null) {
                in.close();
            }

            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    @Test
    public void testPostWithJavaBeanAndCharset() throws Exception {
        String strUrl =
                "http://localhost:18090/tencent.trpc.http.GreeterJavaBeanService/sayHello";
        URL url = new URL(strUrl);
        HttpURLConnection connection = null;
        OutputStream out = null;
        InputStream in = null;

        try {
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(200);
            connection.setReadTimeout(1000);
            connection.setDoOutput(true);
            connection.setDoInput(true);
            connection.setRequestProperty("Content-Type",
                    HttpConstants.CONTENT_TYPE_JSON_WITH_CHARSET);

            out = connection.getOutputStream();
            Map<String, Object> data = new HashMap<>();
            Map<String, Object> innerData = new HashMap<>();
            innerData.put("msg", "trpc");
            data.put("message", "hello");
            data.put("innerMsg", innerData);

            String msg = JsonUtils.toJson(data);
            System.out.println(msg);
            out.write(msg.getBytes(StandardCharsets.UTF_8));
            out.close();

            int responseCode = connection.getResponseCode();
            logger.info("response code is {}", responseCode);

            Assertions.assertEquals(200, responseCode);

            in = connection.getInputStream();

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            StreamUtils.copy(in, bos);

            String rsp = new String(bos.toByteArray(), StandardCharsets.UTF_8);
            logger.error("http response is: {}", rsp);

            Map<String, Object> rspData = JsonUtils.fromJson(rsp, Map.class);

            Assertions.assertEquals("hello", rspData.get("message"));
            Assertions.assertEquals("trpc", ((Map) rspData.get("innerMsg")).get("msg"));
        } finally {
            if (in != null) {
                in.close();
            }

            if (connection != null) {
                connection.disconnect();
            }
        }
    }
}
