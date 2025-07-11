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

import com.tencent.trpc.spring.exception.annotation.EnableTRpcHandleException;
import com.tencent.trpc.spring.exception.api.HandleExceptionConfigurer;
import com.tencent.trpc.spring.exception.annotation.TRpcExceptionHandlerRegistry;
import com.tencent.trpc.spring.exception.api.ExceptionHandlerResolver;
import com.tencent.trpc.spring.exception.api.ExceptionResultTransformer;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportAware;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.annotation.Order;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.CollectionUtils;
import org.springframework.util.function.SingletonSupplier;

/**
 * Default {@link Configuration} class for tRPC exception-handling mechanism
 *
 */
@Configuration
public class HandleExceptionConfiguration implements ImportAware {

    /**
     * Default priority for {@link ExceptionHandlerResolver} and {@link ExceptionResultTransformer}
     */
    public static final int DEFAULT_ORDER = Ordered.HIGHEST_PRECEDENCE + 10;

    protected AnnotationAttributes annotationAttributes;

    private Supplier<ExceptionHandlerResolver> customizedResolverSupplier;

    private Supplier<ExceptionResultTransformer> customizedTransformSupplier;

    /**
     * Build {@link HandleExceptionAdvisingPostProcessor} with default
     * {@link ExceptionHandlerResolver} and {@link ExceptionResultTransformer}
     */
    @Bean
    public HandleExceptionAdvisingPostProcessor handleExceptionAdvisingPostProcessor(
            ObjectProvider<ExceptionHandlerResolver> resolverProvider,
            ObjectProvider<ExceptionResultTransformer> transformProvider) {
        Supplier<ExceptionHandlerResolver> exceptionHandlerResolverSupplier = new SingletonSupplier<>(
                customizedResolverSupplier, () -> resolverProvider.orderedStream().findFirst().orElse(null));
        Supplier<ExceptionResultTransformer> exceptionResultTransformSupplier = new SingletonSupplier<>(
                customizedTransformSupplier, () -> transformProvider.orderedStream().findFirst().orElse(null));
        HandleExceptionAdvisingPostProcessor postProcessor = new HandleExceptionAdvisingPostProcessor(
                exceptionHandlerResolverSupplier, exceptionResultTransformSupplier);
        if (annotationAttributes != null) {
            postProcessor.setProxyTargetClass(annotationAttributes.getBoolean("proxyTargetClass"));
            postProcessor.setExposeProxy(annotationAttributes.getBoolean("exposeProxy"));
        }
        return postProcessor;
    }

    /**
     * Build default {@link ExceptionHandlerResolver}
     *
     * @see DefaultExceptionHandlerResolver
     */
    @Bean
    @Order(DEFAULT_ORDER)
    public ExceptionHandlerResolver defaultExceptionHandlerResolver(
            @Autowired(required = false) @TRpcExceptionHandlerRegistry List<Object> exceptionHandlers) {
        DefaultExceptionHandlerResolver defaultExceptionHandlerResolver = new DefaultExceptionHandlerResolver();
        if (!CollectionUtils.isEmpty(exceptionHandlers)) {
            exceptionHandlers.forEach(defaultExceptionHandlerResolver::detectExceptionHandlers);
        }
        return defaultExceptionHandlerResolver;
    }

    /**
     * Build default {@link ExceptionResultTransformer}
     *
     * @see DefaultExceptionResultTransformer
     */
    @Bean
    @Order(DEFAULT_ORDER)
    public ExceptionResultTransformer defaultExceptionResultTransform() {
        return new DefaultExceptionResultTransformer();
    }

    @Override
    public void setImportMetadata(AnnotationMetadata importMetadata) {
        Class<?> annotationType = EnableTRpcHandleException.class;
        this.annotationAttributes = AnnotationAttributes
                .fromMap(importMetadata.getAnnotationAttributes(annotationType.getName(), false));
        if (this.annotationAttributes == null) {
            throw new IllegalArgumentException("@" + annotationType.getSimpleName()
                    + " is not present on importing class " + importMetadata.getClassName());
        }
    }

    @Autowired(required = false)
    void setConfigurers(Collection<HandleExceptionConfigurer> configurers) {
        if (CollectionUtils.isEmpty(configurers)) {
            return;
        }
        if (configurers.size() > 1) {
            throw new IllegalStateException("Only one HandleExceptionConfigurer may exist");
        }
        HandleExceptionConfigurer configurer = configurers.iterator().next();
        this.customizedResolverSupplier = configurer::getCustomizedHandlerResolver;
        this.customizedTransformSupplier = configurer::getCustomizedResultTransform;
    }

}
