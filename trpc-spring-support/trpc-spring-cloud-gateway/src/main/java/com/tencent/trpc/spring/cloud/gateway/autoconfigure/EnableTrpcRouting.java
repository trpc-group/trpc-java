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

package com.tencent.trpc.spring.cloud.gateway.autoconfigure;

import com.tencent.trpc.spring.cloud.gateway.TrpcGatewayApplication;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Import;

/**
 * When using, you can add annotations to the {@link SpringApplicationBuilder} startup.
 * Example code: {@link TrpcGatewayApplication}
 * The application will automatically load the `Bean` objects in {@link TrpcGatewayConfig} at startup.
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@Import({TrpcGatewayConfig.class})
public @interface EnableTrpcRouting {

}
