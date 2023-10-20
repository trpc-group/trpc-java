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

package com.tencent.trpc.spring.context;

import com.tencent.trpc.spring.context.configuration.TRpcConfigManagerInitializer;
import java.util.Objects;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.InjectionMetadata.InjectedElement;
import org.springframework.core.annotation.AnnotationAttributes;

/**
 * Extends {@link TRpcClientAnnotationBeanPostProcessor} to ensure
 * {@link TRpcConfigManagerInitializer} got executed before injecting tRPC client instances.
 */
public class InitializingTRpcClientAnnotationBeanPostProcessor extends TRpcClientAnnotationBeanPostProcessor {

    private final ObjectProvider<TRpcConfigManagerInitializer> initializer;

    public InitializingTRpcClientAnnotationBeanPostProcessor(
            ObjectProvider<TRpcConfigManagerInitializer> initializer) {
        super();
        this.initializer = initializer;
    }

    @Override
    protected Object doGetInjectedBean(AnnotationAttributes attributes, Object bean, String beanName,
            Class<?> injectedType, InjectedElement injectedElement) {
        Objects.requireNonNull(initializer.getIfAvailable()).initialize();
        return super.doGetInjectedBean(attributes, bean, beanName, injectedType, injectedElement);
    }
}
