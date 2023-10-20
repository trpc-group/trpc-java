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

package com.tencent.trpc.spring.exception.support;

import com.tencent.trpc.core.rpc.anno.TRpcMethod;
import com.tencent.trpc.spring.aop.AbstractExceptionHandlingMethodInterceptor;
import com.tencent.trpc.spring.exception.annotation.TRpcHandleException;
import com.tencent.trpc.spring.exception.api.ExceptionHandler;
import com.tencent.trpc.spring.exception.api.ExceptionHandlerResolver;
import com.tencent.trpc.spring.exception.api.ExceptionResultTransformer;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import org.aopalliance.intercept.MethodInterceptor;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.util.StringUtils;

/**
 * Implements {@link MethodInterceptor} to intercept tRPC method to wire exception handler.
 * Handles exception thrown by methods annotated with {@link TRpcMethod}.
 *
 * @see AbstractExceptionHandlingMethodInterceptor
 */
public class HandleExceptionInterceptor extends AbstractExceptionHandlingMethodInterceptor {

    private final Map<Method, TRpcHandleException> annotationCache = new ConcurrentHashMap<>();

    private final Map<Method, Set<Class<? extends Throwable>>> excludeExceptions = new ConcurrentHashMap<>();

    private final Map<String, ExceptionResultTransformer> transformCache = new ConcurrentHashMap<>();

    private final Map<String, ExceptionHandler> handlerCache = new ConcurrentHashMap<>();

    /**
     * Construct a {@link HandleExceptionInterceptor}
     *
     * @param defaultHandlerResolverSupplier {@link Supplier} for {@link ExceptionHandlerResolver}
     * @param defaultTransformSupplier {@link Supplier} for {@link ExceptionResultTransformer}
     */
    public HandleExceptionInterceptor(
            @Nullable Supplier<ExceptionHandlerResolver> defaultHandlerResolverSupplier,
            @Nullable Supplier<ExceptionResultTransformer> defaultTransformSupplier) {
        super(defaultHandlerResolverSupplier, defaultTransformSupplier);
    }

    @Override
    protected ExceptionHandler determineExceptionHandler(Method method) {
        ExceptionHandler exceptionHandler = null;
        TRpcHandleException handleException = findHandleExceptionAnnotation(method);
        String handlerName = handleException.handler();
        if (StringUtils.hasText(handlerName)) {
            exceptionHandler = handlerCache
                    .computeIfAbsent(handlerName, name -> getQualifierBean(name, ExceptionHandler.class));
        }
        return exceptionHandler;
    }

    @Override
    protected ExceptionResultTransformer determineExceptionResultTransform(Method method) {
        ExceptionResultTransformer resultTransform = null;
        TRpcHandleException handleException = findHandleExceptionAnnotation(method);
        String transformName = handleException.transform();
        if (StringUtils.hasText(transformName)) {
            resultTransform = transformCache
                    .computeIfAbsent(transformName, name -> getQualifierBean(name, ExceptionResultTransformer.class));
        }
        return resultTransform;
    }

    @Override
    protected boolean shouldHandleException(Throwable t, Method method, Object[] args) {
        return shouldHandleException(t.getClass(), method);
    }

    /**
     * Determine if the exception should be handled. Will exclude the exception types
     * defined in {@link TRpcHandleException#exclude()}
     */
    private boolean shouldHandleException(Class<? extends Throwable> exceptionType, Method method) {
        Set<Class<? extends Throwable>> excludeExceptionsSet = excludeExceptions.get(method);
        if (excludeExceptionsSet == null) {
            TRpcHandleException handleException = findHandleExceptionAnnotation(method);
            Class<? extends Throwable>[] excludeExceptionTypes = handleException.exclude();
            excludeExceptionsSet = new HashSet<>();
            if (excludeExceptionTypes.length > 0) {
                excludeExceptionsSet.addAll(Arrays.asList(excludeExceptionTypes));
            }
            excludeExceptions.put(method, excludeExceptionsSet);
        }
        return excludeExceptionsSet.isEmpty() || !excludeExceptionsSet.contains(exceptionType);
    }

    /**
     * Find and return the {@link TRpcHandleException} annotation on target method or class
     */
    private TRpcHandleException findHandleExceptionAnnotation(Method method) {
        if (method == null) {
            throw new NullPointerException();
        }
        if (annotationCache.containsKey(method)) {
            return annotationCache.get(method);
        }
        TRpcHandleException annotation = AnnotatedElementUtils.findMergedAnnotation(method, TRpcHandleException.class);
        if (annotation == null) {
            annotation = AnnotatedElementUtils
                    .findMergedAnnotation(method.getDeclaringClass(), TRpcHandleException.class);
        }
        if (annotation == null) {
            throw new IllegalStateException("No @HandleTRpcException specified in method '" + method + "'");
        }
        annotationCache.put(method, annotation);
        return annotation;
    }

}
