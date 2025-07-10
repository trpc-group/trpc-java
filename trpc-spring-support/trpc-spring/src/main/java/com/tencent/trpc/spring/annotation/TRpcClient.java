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

package com.tencent.trpc.spring.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that Spring container should inject a tRPC client implementation instance
 * to the annotated field (Typically a tRPC service interface).
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@Documented
public @interface TRpcClient {

    /**
     * target tRPC service id. Should be identical to the {@code client.service[i].name} property in trpc.yaml:
     * <pre>
     * client:
     *   service:
     *     - name: demo-service   <--
     * </pre>
     */
    String id();

}
