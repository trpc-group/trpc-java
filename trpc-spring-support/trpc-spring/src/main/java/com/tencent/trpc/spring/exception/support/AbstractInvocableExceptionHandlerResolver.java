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

import com.tencent.trpc.spring.exception.api.ExceptionHandler;
import com.tencent.trpc.spring.exception.api.ExceptionHandlerResolver;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.springframework.core.ExceptionDepthComparator;
import org.springframework.util.ClassUtils;

/**
 * Abstract implementation of {@link ExceptionHandlerResolver} that resolve exception
 * to {@link InvocableExceptionHandler}.
 *
 * @see InvocableExceptionHandler
 */
public abstract class AbstractInvocableExceptionHandlerResolver implements ExceptionHandlerResolver {

    /**
     * Unmapped exception types cache, store exception types that do not bind to any handlers
     */
    private final Set<Class<? extends Throwable>> unmappedExceptionTypes = new HashSet<>();

    /**
     * {@link InvocableExceptionHandler} mapping, map exact exception type to {@link InvocableExceptionHandler}
     */
    private final Map<Class<? extends Throwable>, InvocableExceptionHandler> mappedHandlers = new ConcurrentHashMap<>();

    /**
     * {@link InvocableExceptionHandler} cache, differs from {@link #mappedHandlers}
     * which map exact exception type to {@link InvocableExceptionHandler}, this cache
     * also map exception subclass to {@link InvocableExceptionHandler}.
     */
    private final Map<Class<? extends Throwable>, InvocableExceptionHandler> cacheHandlers = new ConcurrentHashMap<>();

    private boolean allowOverrideExceptionHandler = false;

    /**
     * {@inheritDoc}
     *
     * @param t {@link Throwable} thrown by a tRPC method
     * @param target tRPC service class instance
     * @param targetMethod tRPC method descriptor
     * @return {@link ExceptionHandler} to handle this specific {@link Throwable}
     */
    @Override
    public ExceptionHandler resolveExceptionHandler(Throwable t, Object target, Method targetMethod) {
        return doResolveExceptionHandler(t);
    }

    /**
     * Set allowOverrideExceptionHandler, if true, allow bind multiple handlers
     * to same exception type. But only one of them takes effect.
     * <p>Default to false</p>
     *
     * @param allowOverrideExceptionHandler true or false
     */
    public void setAllowOverrideExceptionHandler(boolean allowOverrideExceptionHandler) {
        this.allowOverrideExceptionHandler = allowOverrideExceptionHandler;
    }

    /**
     * Detect exception-handling methods in target bean and loads them into cache.
     *
     * @param bean target bean
     */
    public void detectExceptionHandlers(Object bean) {
        if (bean == null) {
            return;
        }
        Class<?> beanClass = ClassUtils.getUserClass(bean);
        for (Method method : findExceptionHandlerMethods(bean, beanClass)) {
            for (Class<? extends Throwable> exceptionType : detectExceptionMappings(method)) {
                addExceptionMappingHandler(exceptionType, bean, method);
            }
        }
        // clear caches
        synchronized (unmappedExceptionTypes) {
            unmappedExceptionTypes.clear();
        }
        cacheHandlers.clear();
    }

    /**
     * Find exception-handling methods in target bean
     *
     * @param bean target bean
     * @param type bean type
     * @return exception-handling methods in target bean
     */
    protected abstract Set<Method> findExceptionHandlerMethods(Object bean, Class<?> type);

    /**
     * Detect the exception types can be handled by provided exception-handling method.
     *
     * @param method exception-handling method descriptor
     * @return exception types can be handled
     */
    protected abstract Set<Class<? extends Throwable>> detectExceptionMappings(Method method);

    private ExceptionHandler doResolveExceptionHandler(Throwable exception) {
        if (exception == null) {
            return null;
        }
        return resolveHandlerByExceptionType(exception.getClass());
    }

    private ExceptionHandler resolveHandlerByExceptionType(Class<? extends Throwable> exceptionType) {
        if (exceptionType == null) {
            return null;
        }
        if (unmappedExceptionTypes.contains(exceptionType)) {
            return null;
        }
        InvocableExceptionHandler handler = cacheHandlers.get(exceptionType);
        if (handler == null) {
            handler = getMappedHandler(exceptionType);
            if (handler != null) {
                // if matched, put into cache
                this.cacheHandlers.put(exceptionType, handler);
            } else {
                // if nothing matches
                synchronized (unmappedExceptionTypes) {
                    if (!cacheHandlers.containsKey(exceptionType) && !unmappedExceptionTypes
                            .contains(exceptionType)) {
                        handler = getMappedHandler(exceptionType);
                        if (handler != null) {
                            cacheHandlers.put(exceptionType, handler);
                        } else {
                            unmappedExceptionTypes.add(exceptionType);
                        }
                    }
                }
            }
        }
        return handler;
    }

    private InvocableExceptionHandler getMappedHandler(Class<? extends Throwable> exceptionType) {
        List<Class<? extends Throwable>> matches = mappedHandlers.keySet().stream()
                .filter(mappedException -> mappedException.isAssignableFrom(exceptionType))
                .collect(Collectors.toList());
        if (!matches.isEmpty()) {
            // if multiple exception types are assignable, take the nearest one
            matches.sort(new ExceptionDepthComparator(exceptionType));
            return mappedHandlers.get(matches.get(0));
        }
        return null;
    }

    private void addExceptionMappingHandler(Class<? extends Throwable> exceptionType, Object bean, Method method) {
        InvocableExceptionHandler handler = new InvocableExceptionHandler(bean, method);
        InvocableExceptionHandler oldHandler = mappedHandlers.put(exceptionType, handler);
        if (oldHandler != null && !oldHandler.equals(handler) && !allowOverrideExceptionHandler) {
            throw new IllegalStateException(
                    "Ambiguous @TRpcExceptionHandler handler mapped for [" + exceptionType + "]: handler '" + handler
                            + "' and oldHandler '" + oldHandler + "'");
        }
    }

}
