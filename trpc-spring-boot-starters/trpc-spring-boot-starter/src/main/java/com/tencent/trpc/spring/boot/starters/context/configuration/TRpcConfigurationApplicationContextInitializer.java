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

package com.tencent.trpc.spring.boot.starters.context.configuration;

import com.tencent.trpc.container.config.ApplicationConfigParser;
import com.tencent.trpc.core.common.ConfigManager;
import com.tencent.trpc.core.common.Constants;
import com.tencent.trpc.core.common.TRpcSystemProperties;
import com.tencent.trpc.core.extension.ExtensionLoader;
import com.tencent.trpc.spring.boot.starters.context.configuration.bind.BindResultDescription;
import com.tencent.trpc.spring.boot.starters.context.configuration.bind.LoggingTRpcConfigurationBindResultReporter;
import com.tencent.trpc.spring.boot.starters.context.configuration.bind.TRpcConfigurationBindResult;
import com.tencent.trpc.spring.boot.starters.context.configuration.bind.TRpcConfigurationBindResultReporter;
import com.tencent.trpc.spring.boot.starters.context.configuration.bind.TRpcConfigurationBinder;
import javax.annotation.Nonnull;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;

/**
 * Initializes {@link ConfigManager} configuration
 */
public class TRpcConfigurationApplicationContextInitializer implements
        ApplicationContextInitializer<ConfigurableApplicationContext>,
        Ordered {

    /**
     * TRpcConfigurationApplicationContextInitializer start order is between
     * TRpcConfigurationEnvironmentPostProcessor and Configuration Center
     */
    public static final int ORDER = Ordered.LOWEST_PRECEDENCE;

    @Override
    public void initialize(@Nonnull ConfigurableApplicationContext applicationContext) {
        // won't call ConfigManager.setDefault(), let TRpcConfigManagerInitializer to setDefault()
        TRpcConfigurationBindResult result = bindFrom(applicationContext.getEnvironment());
        getConfigParser().doParse(result.toMap());
        exportBean("tRpcConfigurationProperties", result.getData(), applicationContext);
    }

    private void exportBean(String name, Object bean, ConfigurableApplicationContext applicationContext) {
        applicationContext.getBeanFactory().registerSingleton(name, bean);
        applicationContext.getBeanFactory().registerResolvableDependency(bean.getClass(), bean);
    }

    /**
     * Binds the final configuration mapping of TRpc from the ConfigurableEnvironment
     *
     * @param environment Spring startup environment
     * @return TRpc configuration mapping
     */
    private TRpcConfigurationBindResult bindFrom(ConfigurableEnvironment environment) {
        TRpcConfigurationBinder binder = new TRpcConfigurationBinder(environment);
        TRpcConfigurationBindResult result = binder.bind();
        report(binder);
        return result;
    }

    /**
     * Prints TRpc configuration mapping
     *
     * @param binder TRpc configuration binder
     */
    private void report(TRpcConfigurationBinder binder) {
        BindResultDescription description = binder.getResultDescription();
        getReporter().report(description);
    }

    /**
     * Gets the TRpc configuration binding result printer
     *
     * @return TRpcConfigurationBindResultReporter
     */
    private TRpcConfigurationBindResultReporter getReporter() {
        return new LoggingTRpcConfigurationBindResultReporter();
    }

    /**
     * Gets the TRpc configuration parser
     *
     * @return ApplicationConfigParser
     */
    private ApplicationConfigParser getConfigParser() {
        String configParserName = TRpcSystemProperties.getProperties(TRpcSystemProperties.CONFIG_TYPE,
                Constants.DEFAULT_CONFIG_TYPE);
        return ExtensionLoader.getExtensionLoader(ApplicationConfigParser.class).getExtension(configParserName);
    }

    @Override
    public int getOrder() {
        return ORDER;
    }
}