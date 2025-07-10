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

package com.tencent.trpc.spring.exception.support;

import com.tencent.trpc.spring.exception.annotation.TRpcExceptionHandler;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Extends {@link AbstractAnnotationExceptionHandlerResolver} and provides ability to
 * resolve exception handler by annotation {@link TRpcExceptionHandler}
 *
 * @see TRpcExceptionHandler
 * @see InvocableExceptionHandler
 */
public class AnnotationExceptionHandlerResolver extends
        AbstractAnnotationExceptionHandlerResolver<TRpcExceptionHandler> {

    public AnnotationExceptionHandlerResolver() {
    }

    /**
     * Construct {@link AnnotationExceptionHandlerResolver} that resolve exception-handling methods
     * from target bean
     *
     * @param bean target bean
     */
    public AnnotationExceptionHandlerResolver(Object bean) {
        super.detectExceptionHandlers(bean);
    }

    /**
     * Detect the exception types can be handled by provided method via
     * {@link TRpcExceptionHandler} annotation
     *
     * @param method exception-handling method (annotated with {@link TRpcExceptionHandler})
     * @param annotation {@link TRpcExceptionHandler} instance
     * @return exception types can be handled by provided method
     */
    @SuppressWarnings("unchecked")
    @Override
    protected Set<Class<? extends Throwable>> doDetectExceptionMappings(Method method,
                                                                        TRpcExceptionHandler annotation) {
        Set<Class<? extends Throwable>> result = new HashSet<>(Arrays.asList(annotation.value()));
        if (result.isEmpty()) {
            // If TRpcExceptionHandler.value() is empty, detect exception types from method signature
            for (Class<?> paramType : method.getParameterTypes()) {
                if (Throwable.class.isAssignableFrom(paramType)) {
                    result.add((Class<? extends Throwable>) paramType);
                }
            }
        }
        if (result.isEmpty()) {
            throw new IllegalStateException("No exception types mapped to " + method);
        }
        return result;
    }

}
