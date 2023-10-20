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
import javax.annotation.Nullable;

/**
 * Resolver interface that resolve the exception thrown by a tRPC service invocation to
 * a related {@link ExceptionHandler}.
 */
public interface ExceptionHandlerResolver {

    /**
     * Resolve the exception thrown by a tRPC service invocation to a related {@link ExceptionHandler}.
     *
     * @param t {@link Throwable} thrown by a tRPC method
     * @param target tRPC service class instance
     * @param targetMethod tRPC method descriptor
     * @return {@link ExceptionHandler} to handle this specific {@link Throwable}
     */
    @Nullable
    ExceptionHandler resolveExceptionHandler(Throwable t, @Nullable Object target, @Nullable Method targetMethod);

}
