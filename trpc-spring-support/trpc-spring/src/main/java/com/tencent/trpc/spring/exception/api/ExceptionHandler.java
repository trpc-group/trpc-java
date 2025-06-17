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
import jakarta.annotation.Nullable;

/**
 * Defines the method to handle exceptions thrown by tRPC methods.
 */
@FunctionalInterface
public interface ExceptionHandler {

    /**
     * Handle exception thrown by a tRPC method.
     *
     * @param t the thrown exception
     * @param targetMethod descriptor of the method that throw the exception
     * @param arguments method invocation arguments
     * @return handler result
     * @throws Throwable if the handler decides to re-throw the exception
     */
    @Nullable
    Object handle(Throwable t, @Nullable Method targetMethod, @Nullable Object[] arguments) throws Throwable;

}
