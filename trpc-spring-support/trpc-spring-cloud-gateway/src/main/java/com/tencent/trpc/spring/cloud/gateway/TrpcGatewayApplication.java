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

package com.tencent.trpc.spring.cloud.gateway;

import com.tencent.trpc.spring.cloud.gateway.autoconfigure.EnableTrpcRouting;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

@EnableTrpcRouting
@SpringBootApplication
public class TrpcGatewayApplication {

    public static void main(String[] args) {
        new SpringApplicationBuilder().sources(TrpcGatewayApplication.class).run(args);
    }

}