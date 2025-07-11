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
import com.tencent.trpc.spring.exception.annotation.TRpcExceptionHandlerRegistry;
import com.tencent.trpc.spring.exception.api.ExceptionHandler;
import com.tencent.trpc.spring.exception.api.ExceptionHandlerResolver;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.ClassUtils;

/**
 * Default implementation of tRPC {@link ExceptionHandlerResolver}.
 * <p>Resolve ExceptionHandler by scan methods annotated with {@link TRpcExceptionHandler}, in order of:</p>
 * <ol>
 *     <li>scan annotated methods in target bean</li>
 *     <li>scan annotated methods in {@link Configuration}
 *     class annotated with {@link TRpcExceptionHandlerRegistry}</li>
 * </ol>
 *
 * @see TRpcExceptionHandler
 * @see TRpcExceptionHandlerRegistry
 * @see AnnotationExceptionHandlerResolver
 */
public class DefaultExceptionHandlerResolver extends AnnotationExceptionHandlerResolver {

    private final Map<Class<?>, ExceptionHandlerResolver> specificResolvers = new ConcurrentHashMap<>(8);

    @Override
    public ExceptionHandler resolveExceptionHandler(Throwable e, Object target, Method targetMethod) {
        ExceptionHandler exceptionHandler = null;
        // scan annotated methods in target bean
        ExceptionHandlerResolver specificResolver = getSpecificExceptionHandlerResolver(target);
        if (specificResolver != null) {
            exceptionHandler = specificResolver.resolveExceptionHandler(e, target, targetMethod);
        }
        // if no annotated methods found in target bean, fallback to scan TRpcExceptionHandlerRegistry annotated class
        if (exceptionHandler == null) {
            exceptionHandler = super.resolveExceptionHandler(e, target, targetMethod);
        }
        return exceptionHandler;
    }

    private ExceptionHandlerResolver getSpecificExceptionHandlerResolver(Object bean) {
        if (bean == null) {
            return null;
        }
        Class<?> targetType = ClassUtils.getUserClass(bean);
        return specificResolvers.computeIfAbsent(targetType, type -> new AnnotationExceptionHandlerResolver(bean));
    }

}
