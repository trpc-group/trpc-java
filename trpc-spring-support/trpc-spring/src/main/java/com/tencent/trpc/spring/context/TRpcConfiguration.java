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

import com.tencent.trpc.spring.context.configuration.TRpcConfigManagerCustomizer;
import com.tencent.trpc.spring.context.configuration.TRpcConfigManagerInitializer;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

/**
 * Spring configuration class for tRPC
 */
@Configuration(proxyBeanMethods = false)
public class TRpcConfiguration {

    public static final String CONFIG_MANAGER_INITIALIZER_BEAN_NAME = "tRpcConfigManagerInitializer";

    /**
     * Build {@link TRpcLifecycleManager}
     *
     * @return TRpcLifecycleManager
     */
    @Bean
    public TRpcLifecycleManager tRpcLifecycleManager() {
        return new TRpcLifecycleManager();
    }

    /**
     * Build {@link TRpcConfigManagerInitializer}.
     * <p>Users could implement {@link TRpcConfigManagerCustomizer} and
     * register it as bean to customize {@link com.tencent.trpc.core.common.ConfigManager}</p>
     *
     * @param customizerProvider implementations of {@link TRpcConfigManagerCustomizer}, if any
     * @return TRpcConfigManagerInitializer
     */
    @Bean(CONFIG_MANAGER_INITIALIZER_BEAN_NAME)
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public TRpcConfigManagerInitializer tRpcConfigManagerInitializer(
            ObjectProvider<TRpcConfigManagerCustomizer> customizerProvider) {
        return new TRpcConfigManagerInitializer(customizerProvider);
    }

    /**
     * Build {@link TRpcClientAnnotationBeanPostProcessor}
     *
     * @return TRpcClientAnnotationBeanPostProcessor
     */
    @Bean
    public TRpcClientAnnotationBeanPostProcessor tRpcClientAnnotationBeanPostProcessor(
            ObjectProvider<TRpcConfigManagerInitializer> tRpcConfigManagerInitializer) {
        return new InitializingTRpcClientAnnotationBeanPostProcessor(tRpcConfigManagerInitializer);
    }

    /**
     * Build {@link TRpcConfigRegistryPostProcessor}
     *
     * @return TRpcConfigRegistryPostProcessor
     */
    @Bean
    public TRpcConfigRegistryPostProcessor tRpcConfigRegistryPostProcessor() {
        return new TRpcConfigRegistryPostProcessor();
    }

}
