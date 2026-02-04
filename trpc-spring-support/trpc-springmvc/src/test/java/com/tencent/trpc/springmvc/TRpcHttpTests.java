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

package com.tencent.trpc.springmvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tencent.trpc.core.utils.Charsets;
import com.tencent.trpc.springmvc.proto.ProtoJsonHttpMessageConverter;
import com.tencent.trpc.proto.http.util.StreamUtils;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.client.RestTemplate;
import tests.TrpcSpringmvcDemoApplication;
import tests.proto.HelloRequestProtocol;

@SpringBootTest(classes = TrpcSpringmvcDemoApplication.class,
        webEnvironment = WebEnvironment.DEFINED_PORT)
public class TRpcHttpTests {

    private static final Logger logger = LoggerFactory.getLogger(TRpcHttpTests.class);

    @Test
    public void testNotFoundService() throws Exception {
        URL url = new URL(
                "http://localhost:12347/NotExistService/NotExistMethod");
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
            Assertions.assertEquals(404, responseCode);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    @Test
    public void testNotSpecifiedRouter() throws Exception {
        URL url = new URL("http://localhost:12347");
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
            Assertions.assertEquals(404, responseCode);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    @Test
    public void testNotSpecifiedEnoughRouter() throws Exception {
        URL url = new URL("http://localhost:12347/TestApp/TestServer/Greeter");
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
            Assertions.assertEquals(404, responseCode);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    @Test
    public void testPostByJson() throws Exception {
        testPostByJson("trpc", "trpc.TestApp.TestServer.Greeter", 200);
        testPostByJson("trpc", "trpc.TestApp.TestServer.Greeter2", 200);
        testPostByJson("trpc", "trpc.TestApp.TestServer.Greeter2", 200);
    }

    private void testPostByJson(String baseUrl, String service, int expectStatus) throws Exception {
        URL url = new URL(
                String.format("http://localhost:12347/%s/%s/sayHello", baseUrl,
                        service));
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
            connection.setRequestProperty("Content-Type", TRpcHttpConstants.CONTENT_TYPE_JSON);

            out = connection.getOutputStream();
            String message = Base64.getEncoder()
                    .encodeToString("你好TRpc-Java!".getBytes(Charsets.UTF_8));
            System.out.println(message);
            out.write(("{\"message\":\"" + message + "\"}").getBytes(StandardCharsets.UTF_8));
            out.close();

            int responseCode = connection.getResponseCode();
            logger.info("response code is {}", responseCode);

            Assertions.assertEquals(expectStatus, responseCode);
            if (expectStatus == 404) {
                return;
            }

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

    public void testPostByJsonWithRest() throws Exception {
        URL url = new URL(
                "http://localhost:12347/test-base-url/TestApp/TestServer/Greeter/sayHello");
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
            connection.setRequestProperty("Content-Type", TRpcHttpConstants.CONTENT_TYPE_JSON);

            out = connection.getOutputStream();
            String message = Base64.getEncoder()
                    .encodeToString("你好TRpc-Java!".getBytes(Charsets.UTF_8));
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
    public void testPostByPB() throws Exception {
        URL url = new URL(
                "http://localhost:12347/trpc.TestApp.TestServer.Greeter2/sayHello");
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
            connection.setRequestProperty("Content-Type", TRpcHttpConstants.CONTENT_TYPE_PROTOBUF);

            HelloRequestProtocol.HelloRequest.Builder reqBuilder = HelloRequestProtocol.HelloRequest
                    .newBuilder();
            reqBuilder.setMessage("你好TRpc-Java!");

            out = connection.getOutputStream();
            out.write(reqBuilder.build().toByteArray());
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
    public void testPostMapByJson() throws Exception {
        URL url = new URL(
                "http://localhost:12347/test-base-url/trpc.TestApp.TestServer.GreeterJson/sayHelloJson");
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
            connection.setRequestProperty("Content-Type", TRpcHttpConstants.CONTENT_TYPE_JSON);

            out = connection.getOutputStream();
            String message = Base64.getEncoder()
                    .encodeToString("你好TRpc-Java!".getBytes(Charsets.UTF_8));
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
    public void testGet() throws Exception {
        String strUrl =
                "http://localhost:12347/trpc.TestApp.TestServer.Greeter2/sayHello?message=";
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
    public void testGetWithDefaultBasePath() throws Exception {
        String strUrl =
                "http://localhost:12347/trpc/trpc.TestApp.TestServer.Greeter2/sayHello?message=";
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
    public void testGetWithJavaBean() throws Exception {
        String strUrl =
                "http://localhost:12347/test-base-url/trpc.TestApp.TestServer.GreeterJavaBeanService/sayHello?message=";
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
                "http://localhost:12347/test-base-url/trpc.TestApp.TestServer.GreeterJson/sayHelloJson?message=";
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
    public void testUnknownMethod() throws Exception {
        String strUrl =
                "http://localhost:12347/trpc.TestApp.TestServer.Greeter2/sayHello?message=";
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

            Assertions.assertEquals(404, responseCode);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    @Test
    public void testGetWithTrpcHeader() throws Exception {
        String strUrl =
                "http://localhost:12347/test-base-url2/trpc.TestApp.TestServer.Greeter3/sayHello?message=";

        String base64Param = Base64.getEncoder()
                .encodeToString("你好TRpc-Java".getBytes(Charsets.UTF_8));
        URL url = new URL(strUrl + URLEncoder.encode(base64Param, "UTF-8"));
        HttpURLConnection connection = null;

        try {
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(200);
            connection.setReadTimeout(1000);
            connection.setDoOutput(false);
            connection.setDoInput(true);

            connection.setRequestProperty(TRpcHttpConstants.HTTP_HEADER_TRPC_REQUEST_ID,
                    Integer.toString(Integer.MAX_VALUE));
            // 此处时间过短，会先收到请求503的超时
            connection.setRequestProperty(TRpcHttpConstants.HTTP_HEADER_TRPC_TIMEOUT,
                    Integer.toString(200));
            connection.setRequestProperty(TRpcHttpConstants.HTTP_HEADER_TRPC_CALLER,
                    "trpc.http.test.caller");
            connection.setRequestProperty(TRpcHttpConstants.HTTP_HEADER_TRPC_CALLEE,
                    "trpc.TestApp.TestServer.Greeter3.sayHello");
            connection.setRequestProperty(TRpcHttpConstants.HTTP_HEADER_TRPC_MESSAGE_TYPE,
                    Integer.toString(1));

            ObjectMapper objectMapper = new ObjectMapper();
            String transJson = objectMapper
                    .writeValueAsString(Collections.singletonMap("message", base64Param));
            connection.setRequestProperty(TRpcHttpConstants.HTTP_HEADER_TRPC_TRANS_INFO, transJson);

            int responseCode = connection.getResponseCode();
            logger.info("response code is {}", responseCode);

            Assertions.assertEquals(408, responseCode);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    @Test
    public void testControllerPostPbByJson() throws Exception {
        RestTemplate restTemplate = new RestTemplate();
        Map<String, String> body = new HashMap<>();
        body.put("message", "testControllerPostPbByJson");

        String responseStr = restTemplate
                .postForObject("http://localhost:12347/test", body, String.class);
        Assertions.assertEquals("{\"message\":\"testControllerPostPbByJsontest\"}", responseStr);

        List<HttpMessageConverter<?>> messageConverters = restTemplate.getMessageConverters();
        messageConverters.add(0, new ProtoJsonHttpMessageConverter<>());
        restTemplate.setMessageConverters(messageConverters);

        HelloRequestProtocol.HelloResponse response = restTemplate
                .postForObject("http://localhost:12347/test", body, HelloRequestProtocol.HelloResponse.class);
        Assertions.assertEquals("testControllerPostPbByJsontest", response.getMessage());

        URL url = new URL("http://localhost:12347/test");
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
            connection.setRequestProperty("Content-Type", TRpcHttpConstants.CONTENT_TYPE_JSON);

            out = connection.getOutputStream();
            out.write(("{\"message\":\"你好TRpc-Java!\"}").getBytes(StandardCharsets.UTF_8));
            out.close();

            int responseCode = connection.getResponseCode();
            Assertions.assertEquals(200, responseCode);

            in = connection.getInputStream();
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            StreamUtils.copy(in, bos);

            Assertions.assertEquals("{\"message\":\"你好TRpc-Java!test\"}",
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

}
