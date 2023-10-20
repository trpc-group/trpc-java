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

import com.tencent.trpc.core.common.ConfigManager;
import com.tencent.trpc.core.common.config.BackendConfig;
import com.tencent.trpc.core.common.config.ClientConfig;
import com.tencent.trpc.core.common.config.ServerConfig;
import com.tencent.trpc.core.common.config.ServiceConfig;
import com.tencent.trpc.core.filter.FilterManager;
import com.tencent.trpc.core.filter.spi.Filter;
import com.tencent.trpc.core.logger.Logger;
import com.tencent.trpc.core.logger.LoggerFactory;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.lang.NonNull;
import org.springframework.util.Assert;

/**
 * Extends {@link BeanDefinitionRegistryPostProcessor} to register
 * tRPC {@link ConfigManager} and tRPC clients as beans.
 */
public class TRpcConfigRegistryPostProcessor implements BeanDefinitionRegistryPostProcessor, Ordered, InitializingBean {

    public static final String CONFIG_MANAGE_BEAN_NAME = "TRpcConfigManager";

    public static final int DEFAULT_ORDER = Ordered.LOWEST_PRECEDENCE - 10;

    private static final Logger logger = LoggerFactory.getLogger(TRpcConfigRegistryPostProcessor.class);

    private final ConfigManager configManager;

    public TRpcConfigRegistryPostProcessor() {
        this(ConfigManager.getInstance());
    }

    public TRpcConfigRegistryPostProcessor(ConfigManager configManager) {
        this.configManager = configManager;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    @Override
    public void afterPropertiesSet() {
        Assert.notNull(configManager, "configManager must not be null");
    }

    @Override
    public int getOrder() {
        return TRpcConfigRegistryPostProcessor.DEFAULT_ORDER;
    }

    @Override
    public void postProcessBeanDefinitionRegistry(@NonNull BeanDefinitionRegistry registry) throws BeansException {
        // register TRpc configManager
        registerTRpcConfigManagerBeanDefinition(registry);
        // register TRpc client
        registerTRpcClientBeanDefinition(registry);
        // register TRpc filter
        registerTRpcFilterBeanDefinition(registry);
    }

    @Override
    public void postProcessBeanFactory(@NonNull ConfigurableListableBeanFactory beanFactory) throws BeansException {
    }

    /**
     * Register tRPC {@link ConfigManager} as bean.
     *
     * @param registry bean definition registry
     */
    private void registerTRpcConfigManagerBeanDefinition(BeanDefinitionRegistry registry) {
        String beanName = TRpcConfigRegistryPostProcessor.CONFIG_MANAGE_BEAN_NAME;
        BeanDefinition beanDefinition = BeanDefinitionBuilder
                .genericBeanDefinition(ConfigManager.class, this::getConfigManager)
                .setLazyInit(true)
                .setScope(ConfigurableBeanFactory.SCOPE_SINGLETON)
                .setRole(BeanDefinition.ROLE_INFRASTRUCTURE)
                .getBeanDefinition();
        registry.registerBeanDefinition(beanName, beanDefinition);
        logger.info("Register TRpc config manager named '{}'", beanName);
    }

    /**
     * Register tRPC clients as beans.
     *
     * @param registry bean definition registry
     */
    private void registerTRpcClientBeanDefinition(BeanDefinitionRegistry registry) {
        Map<String, BackendConfig> backendConfigMap = configManager.getClientConfig().getBackendConfigMap();
        for (Map.Entry<String, BackendConfig> backendConfigEntry : backendConfigMap.entrySet()) {
            String beanName = backendConfigEntry.getKey();
            BackendConfig backendConfig = backendConfigEntry.getValue();
            Class<?> serviceInterface = backendConfig.getServiceInterface();
            if (serviceInterface != null) {
                AbstractBeanDefinition beanDefinition = BeanDefinitionBuilder
                        .genericBeanDefinition(TRpcClientFactoryBean.class)
                        .addConstructorArgValue(beanName)
                        .addConstructorArgValue(serviceInterface)
                        .addDependsOn(TRpcConfiguration.CONFIG_MANAGER_INITIALIZER_BEAN_NAME)
                        .setLazyInit(true)
                        .setScope(ConfigurableBeanFactory.SCOPE_SINGLETON)
                        .getBeanDefinition();
                beanDefinition.setResourceDescription("TRpc client config");
                registry.registerBeanDefinition(beanName, beanDefinition);
                logger.info("Register TRpc client named '{}' for service interface '{}'", beanName, serviceInterface);
            }
        }
    }

    /**
     * Register {@link com.tencent.trpc.core.filter.spi.Filter} bean definition.
     *
     * @param registry bean definition registry
     */
    private void registerTRpcFilterBeanDefinition(BeanDefinitionRegistry registry) {
        Set<String> filterNames = new HashSet<>();

        ServerConfig serverConfig = configManager.getServerConfig();
        Optional.ofNullable(serverConfig.getFilters()).ifPresent(filterNames::addAll);
        for (ServiceConfig serviceConfig : serverConfig.getServiceMap().values()) {
            Optional.ofNullable(serviceConfig.getFilters()).ifPresent(filterNames::addAll);
        }

        ClientConfig clientConfig = configManager.getClientConfig();
        Optional.ofNullable(clientConfig.getFilters()).ifPresent(filterNames::addAll);
        for (BackendConfig backendConfig : clientConfig.getBackendConfigMap().values()) {
            Optional.ofNullable(backendConfig.getFilters()).ifPresent(filterNames::addAll);
        }

        for (String filterName : filterNames) {
            AbstractBeanDefinition beanDefinition = BeanDefinitionBuilder
                    .genericBeanDefinition(Filter.class, () -> FilterManager.get(filterName))
                    .setScope(ConfigurableBeanFactory.SCOPE_SINGLETON)
                    .setRole(BeanDefinition.ROLE_INFRASTRUCTURE)
                    .getBeanDefinition();
            beanDefinition.setResourceDescription("TRpc filter config");
            registry.registerBeanDefinition(filterName, beanDefinition);
            logger.info("Register TRpc filter named '{}'", filterName);
        }
    }

}
