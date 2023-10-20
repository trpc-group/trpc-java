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

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Objects;
import java.util.Set;
import org.springframework.core.GenericTypeResolver;
import org.springframework.core.MethodIntrospector;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils.MethodFilter;

/**
 * Extends {@link AbstractInvocableExceptionHandlerResolver} and provides ability to
 * find exception-handling methods via annotation
 *
 * @param <T> annotation type
 */
public abstract class AbstractAnnotationExceptionHandlerResolver<T extends Annotation> extends
        AbstractInvocableExceptionHandlerResolver {

    protected final Class<T> annotationType;
    protected final MethodFilter methodFilter;

    @SuppressWarnings("unchecked")
    public AbstractAnnotationExceptionHandlerResolver() {
        this.annotationType = (Class<T>) Objects.requireNonNull(GenericTypeResolver
                .resolveTypeArgument(getClass(), AbstractAnnotationExceptionHandlerResolver.class));
        this.methodFilter = method -> AnnotatedElementUtils.hasAnnotation(method, this.annotationType);
    }

    /**
     * Get methods annotated with annotation {@link T}
     *
     * @param bean target bean
     * @param type bean type
     * @return methods annotated with annotation {@link T}
     */
    @Override
    protected Set<Method> findExceptionHandlerMethods(Object bean, Class<?> type) {
        return MethodIntrospector.selectMethods(type, methodFilter);
    }

    /**
     * Detect the exception types can be handled by provided exception-handling method via annotation
     *
     * @param method exception-handling method descriptor
     * @return exception types can be handled
     */
    @Override
    protected Set<Class<? extends Throwable>> detectExceptionMappings(Method method) {
        T ann = AnnotatedElementUtils.findMergedAnnotation(method, annotationType);
        Assert.notNull(ann, "Annotation '" + annotationType + "' is not present on method '" + method + "'");
        return doDetectExceptionMappings(method, ann);
    }

    /**
     * Detect the exception types can be handled by provided exception-handling method via annotation {@link T}
     *
     * @param method exception-handling method
     * @param annotation target annotation on method
     * @return exception types can be handled
     */
    protected abstract Set<Class<? extends Throwable>> doDetectExceptionMappings(Method method, T annotation);

}
