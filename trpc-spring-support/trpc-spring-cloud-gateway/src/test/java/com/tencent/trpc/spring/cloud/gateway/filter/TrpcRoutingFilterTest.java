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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.tencent.trpc.spring.cloud.gateway.TrpcGatewayApplication;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

public class TrpcRoutingFilterTest {

    private final String requestBody = "{\"msg\":\"hello gateway!\",\"id\":\"\"}";

    ConfigurableApplicationContext application;

    @BeforeEach
    void setUp() {
        // Start the Spring container, start the gateway, and backend services
        application = new SpringApplicationBuilder().sources(TrpcGatewayApplication.class).run(new String[0]);
    }

    @AfterEach
    void tearDown() {
        // Stop the gateway and simulated backend services
        application.stop();
    }

    @Test
    void filter() {
        // Initiate an HTTP request and verify the normal TRPC forwarding scenario.
        trpcTest();

        // Initiate an HTTP request and verify the normal HTTP forwarding scenario.
        httpTest();
    }

    private void httpTest() {
        try {
            JSONObject response = gateway(getHttpRequest());
            assertEquals(response.toString(), requestBody);
        } catch (JSONException | IOException e) {
            assertNull(e);
        }
    }

    private void trpcTest() {
        try {
            JSONObject response = gateway(getTRPCRequest(requestBody));
            assertEquals(response.toString(), requestBody);
        } catch (JSONException | IOException e) {
            assertNull(e);
        }
    }

    private JSONObject gateway(Request httpRequest) throws JSONException, IOException {
        Response response = new OkHttpClient().newBuilder().readTimeout(2, TimeUnit.SECONDS).build()
                .newCall(httpRequest).execute();
        // Format is as follows: {"message":"","id":""}
        return new JSONObject(response.body().string());
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
