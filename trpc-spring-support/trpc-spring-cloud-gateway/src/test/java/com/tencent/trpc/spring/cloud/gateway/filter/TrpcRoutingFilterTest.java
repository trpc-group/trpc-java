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

package com.tencent.trpc.spring.cloud.gateway.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.tencent.trpc.spring.cloud.gateway.TrpcGatewayApplication;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

public class TrpcRoutingFilterTest {

    private final String requestBody = "{\"msg\":\"hello gateway!\",\"id\":\"\"}";

    private ConfigurableApplicationContext application;
    private OkHttpClient httpClient;

    @BeforeEach
    void setUp() throws InterruptedException {
        application = new SpringApplicationBuilder().sources(TrpcGatewayApplication.class).run(new String[0]);
        TimeUnit.SECONDS.sleep(5);

        httpClient = new OkHttpClient().newBuilder()
                .readTimeout(10, TimeUnit.SECONDS)
                .connectTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .build();
    }

    @AfterEach
    void tearDown() {
        if (application != null) {
            application.close();
        }
    }

    @Test
    void filter() {
        trpcTest();
        httpTest();
    }

    private void httpTest() {
        try {
            JSONObject response = gateway(getHttpRequest());
            assertNotNull(response);
            assertEquals(requestBody, response.toString());
        } catch (JSONException | IOException e) {
            throw new AssertionError("httpTest failed", e);
        }
    }

    private void trpcTest() {
        try {
            JSONObject response = gateway(getTRPCRequest(requestBody));
            assertNotNull(response);
            assertEquals(requestBody, response.toString());
        } catch (JSONException | IOException e) {
            throw new AssertionError("trpcTest failed", e);
        }
    }

    private JSONObject gateway(Request httpRequest) throws JSONException, IOException {
        try (Response response = httpClient.newCall(httpRequest).execute()) {
            ResponseBody body = response.body();
            assertNotNull(body);
            return new JSONObject(body.string());
        }
    }

    private Request getHttpRequest() {
        String url = "http://127.0.0.1:8080/cgi-bin/test?service=trpc.test.demo.Hello&method="
                + "SayHello&msg=hello gateway!";
        return new Request.Builder().url(url).build();
    }

    private Request getTRPCRequest(String payload) {
        String url = "http://127.0.0.1:8080/trpc/hello";
        RequestBody body = RequestBody.create(payload, MediaType.get("application/json"));
        return new Request.Builder().url(url).post(body).build();
    }

}
