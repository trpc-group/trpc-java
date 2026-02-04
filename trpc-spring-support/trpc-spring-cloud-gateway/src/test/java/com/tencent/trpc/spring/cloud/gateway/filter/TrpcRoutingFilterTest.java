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
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

public class TrpcRoutingFilterTest {

    private static final String REQUEST_BODY = "{\"msg\":\"hello gateway!\",\"id\":\"\"}";
    private static ConfigurableApplicationContext application;
    private static OkHttpClient httpClient;

    @BeforeAll
    static void setUp() throws InterruptedException {
        application = SpringApplication.run(TestSpringApplication.class);
        Thread.sleep(3000);

        httpClient = new OkHttpClient().newBuilder()
                .readTimeout(10, TimeUnit.SECONDS)
                .connectTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .build();
    }

    @AfterAll
    static void tearDown() {
        if (application != null) {
            application.close();
        }
    }

    @Test
    void httpTest() throws JSONException, IOException {
        JSONObject response = gateway(getHttpRequest());
        assertNotNull(response);
        assertEquals(REQUEST_BODY, response.toString());
    }

    @Test
    void trpcTest() throws JSONException, IOException {
        JSONObject response = gateway(getTRPCRequest(REQUEST_BODY));
        assertNotNull(response);
        assertEquals(REQUEST_BODY, response.toString());
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
