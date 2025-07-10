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

package com.tencent.trpc.spring.aop;

import com.tencent.trpc.spring.context.support.BeanFactoryAwareSupport;
import com.tencent.trpc.spring.exception.api.ExceptionHandler;
import com.tencent.trpc.spring.exception.api.ExceptionHandlerResolver;
import com.tencent.trpc.spring.exception.api.ExceptionResultTransformer;
import java.lang.reflect.Method;
import java.util.function.Supplier;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.core.Ordered;
import org.springframework.util.ClassUtils;
import org.springframework.util.function.SingletonSupplier;

/**
 * Abstract base class of tRPC exception handling interceptors
 */
public abstract class AbstractExceptionHandlingMethodInterceptor extends BeanFactoryAwareSupport
        implements MethodInterceptor, Ordered {

    private final Supplier<ExceptionHandlerResolver> defaultHandlerResolverSupplier;
    private final Supplier<ExceptionResultTransformer> defaultTransformSupplier;


    public AbstractExceptionHandlingMethodInterceptor(
            @Nullable Supplier<ExceptionHandlerResolver> defaultHandlerResolverSupplier,
            @Nullable Supplier<ExceptionResultTransformer> defaultTransformSupplier) {
        this.defaultHandlerResolverSupplier = new SingletonSupplier<>(
                defaultHandlerResolverSupplier, () -> getBean(ExceptionHandlerResolver.class));
        this.defaultTransformSupplier = new SingletonSupplier<>(defaultTransformSupplier,
                () -> (result, targetType) -> result);
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    @Nullable
    @Override
    public Object invoke(@Nonnull MethodInvocation invocation) throws Throwable {
        Object result;
        try {
            result = invocation.proceed();
        } catch (Throwable t) {
            result = handleException(t, invocation.getThis(), invocation.getMethod(), invocation.getArguments());
        }
        return result;
    }

    /**
     * Determine if the {@link Throwable} should be handled
     *
     * @param t exception threw by method
     * @param method method
     * @param args method invoking arguments
     * @return true or false
     */
    protected abstract boolean shouldHandleException(Throwable t, Method method, Object[] args);

    /**
     * Get the {@link ExceptionHandler} for handling exceptions thrown by specific {@link Method}.
     *
     *
     * @param method method to handle
     * @return {@link ExceptionHandler} implementation
     */
    protected abstract ExceptionHandler determineExceptionHandler(Method method);

    /**
     * Get the {@link ExceptionResultTransformer} for transforming exception handler result.
     *
     * @param method method to handle
     * @return {@link ExceptionResultTransformer} implementation
     */
    protected abstract ExceptionResultTransformer determineExceptionResultTransform(Method method);

    private Object handleException(Throwable t, Object target, Method method, Object... args) throws Throwable {
        Class<?> targetType = target == null ? null : ClassUtils.getUserClass(target);
        Method specificMethod = ClassUtils.getMostSpecificMethod(method, targetType);

        if (shouldHandleException(t, specificMethod, args)) {
            Object handleResult = doHandleException(t, target, specificMethod, args);
            return transformResult(handleResult, specificMethod);
        } else {
            throw t;
        }
    }

    private Object doHandleException(Throwable t, Object target, Method method, Object... args) throws Throwable {
        ExceptionHandler exceptionHandler = determineExceptionHandler(method);
        if (exceptionHandler == null) {
            ExceptionHandlerResolver exceptionHandlerResolver = defaultHandlerResolverSupplier.get();
            if (exceptionHandlerResolver != null) {
                exceptionHandler = exceptionHandlerResolver.resolveExceptionHandler(t, target, method);
            }
        }
        if (exceptionHandler == null) {
            // re-throw the exception if no handlers found
            throw t;
        } else {
            return exceptionHandler.handle(t, method, args);
        }
    }

    private Object transformResult(Object handleResult, Method method) {
        final Class<?> targetType = method.getReturnType();
        if (void.class.isAssignableFrom(method.getReturnType())) {
            return null;
        }
        ExceptionResultTransformer resultTransform = determineExceptionResultTransform(method);
        if (resultTransform == null) {
            resultTransform = defaultTransformSupplier.get();
        }
        if (resultTransform == null) {
            return handleResult;
        }
        return resultTransform.transform(handleResult, targetType);
    }

}
