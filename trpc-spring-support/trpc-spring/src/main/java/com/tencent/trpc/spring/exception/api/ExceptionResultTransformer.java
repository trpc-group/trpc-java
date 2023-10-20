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

package com.tencent.trpc.spring.exception.api;

import java.lang.reflect.Method;

/**
 * Transformer interface that transform the result of {@link ExceptionHandler#handle(Throwable, Method, Object[])}
 * to the expected type, which will be returned by the invoked method.
 */
@FunctionalInterface
public interface ExceptionResultTransformer {

    /**
     * Transform the object returned by {@link ExceptionHandler} to a instance of target type
     *
     * @param result transform source object
     * @param targetType type of the transform result
     * @return transformed Object
     */
    Object transform(Object result, Class<?> targetType);

}
