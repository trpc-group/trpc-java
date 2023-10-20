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

package com.tencent.trpc.spring.boot.starters.env;

import static org.springframework.core.env.StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME;

import com.tencent.trpc.core.common.ConfigManager;
import com.tencent.trpc.core.common.TRpcSystemProperties;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.config.ConfigDataEnvironmentPostProcessor;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.boot.logging.DeferredLog;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.core.env.CompositePropertySource;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.util.CollectionUtils;

/**
 * Reads the specified TRPC configuration files and loads them into {@link ConfigManager}.
 *
 * Default loading order (all of the following configurations will be loaded; if a user-specified configuration
 * does not exist or is unreadable, it will be skipped):
 * <ol>
 *     <li>System Properties: Usually specified by -Dtrpc.*=v</li>
 *     <li>System Environment: System environment variables, starting with trpc_*</li>
 *     <li>Configuration file specified by -Dtrpc_config_path=/path/to/file</li>
 *     <li>Spring Config File: Local configuration files loaded by Spring, usually classpath*:application-*.yml</li>
 *     <li>If item 3 is not specified, add classpath:trpc_java.yaml</li>
 *     <li>If item 3 is not specified, add classpath:META-INF/trpc/trpc_java.yaml</li>
 *     <li>If item 3 is not specified, add classpath:trpc_java-${firstActivatedProfile}.yaml</li>
 * </ol>
 *
 * <ul>
 *     <li>If you want to insert configurations from a custom configuration center and set their priority (e.g., between 1.2 and 1.3 or between 1.4 and 1.5):</li>
 * <pre>{@code
 *     // Load configuration from the configuration center
 *     PropertySource<?> configCenterPropertySource = loadFromConfigCenter();
 *
 *     MutablePropertySources mutablePropertySources = ConfigurableEnvironment.getPropertySources();
 *     // Add to the end (lowest priority)
 *     mutablePropertySources.addLast(configCenterPropertySource);
 *
 *     // Higher priority than SystemProperties
 *     mutablePropertySources.addBefore(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME);
 *
 *     // Higher priority than user-specified TRPC configuration file
 *     mutablePropertySources.addBefore(TRpcSpringEnvironments.CUSTOM_PROPERTY_SOURCE_NAME);
 *
 *     // Higher priority than TRPC default local configuration file
 *     mutablePropertySources.addBefore(TRpcSpringEnvironments.DEFAULT_PROPERTY_SOURCE_NAME);
 *
 *     // Lower priority than TRPC default local configuration file
 *     mutablePropertySources.addAfter(TRpcSpringEnvironments.DEFAULT_PROPERTY_SOURCE_NAME);
 * }
 *
 * The system predefines two property source names, which users can insert before and after the custom source:
 *     TRpcSpringPropertySources.CUSTOM：Configuration file specified by -Dtrpc_config_path=/path/to/file
 *     TRpcSpringPropertySources.DEFAULT：TRPC default local configuration file
 * </pre>
 *
 *     <li>You can also create a custom {@code EnvironmentPostProcessor} and implement {@code Ordered}.</li>
 * </ul>
 *
 * @see StandardEnvironment StandardEnvironment: Default contains System Properties and System Environment
 * @see ConfigDataEnvironmentPostProcessor ConfigFileApplicationListener: Spring Boot's entry point for loading local configuration files
 * @see TRpcSpringPropertySources
 * @see ConfigManager
 * @see #ORDER
 */
public class TRpcConfigurationEnvironmentPostProcessor implements EnvironmentPostProcessor,
        ApplicationContextInitializer<ConfigurableApplicationContext>, Ordered {

    public static final int ORDER = ConfigDataEnvironmentPostProcessor.ORDER + 100;

    private static final String FILE_SCHEMA = "file://";

    private static final String[] DEFAULT_PROPERTY_SOURCE_LOCATIONS = new String[]{
            "classpath:trpc_java.yaml",
            "classpath:META-INF/trpc/trpc_java.yaml"
    };

    /**
     * Support for Spring environment naming style configurations, load Spring profiles active files
     * e.g. spring.profiles.active=dev,p1,p2 will load classpath:trpc_java-dev.yaml file
     */
    private static final String PROFILE_PROPERTY_SOURCE_FORMAT = "classpath:trpc_java-%s.yaml";

    private final DeferredLog loggerForLoader = new DeferredLog();

    @Override
    public int getOrder() {
        return ORDER;
    }

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        application.addInitializers(this);
        TRpcPropertySourceLoader propertySourceLoader = new TRpcPropertySourceLoader(loggerForLoader,
                application.getResourceLoader());
        // Load user-defined configuration path first
        boolean customLoaded = ifAnyLoaded(propertySourceLoader.loadAndCompose(TRpcSpringPropertySources.CUSTOM,
                        getCustomPropertySourceLocation()),
                s -> environment.getPropertySources().addAfter(SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME, s));
        if (!customLoaded) {
            // If no user-defined configuration path is loaded, load the default configuration
            // Default configuration at the last (after the Spring configuration files)
            ifAnyLoaded(propertySourceLoader.loadAndCompose(TRpcSpringPropertySources.DEFAULT,
                    getPropertySourceLocations(environment)), environment.getPropertySources()::addLast);
        }
    }

    @Override
    public void initialize(@Nonnull ConfigurableApplicationContext applicationContext) {
        loggerForLoader.switchTo(TRpcPropertySourceLoader.class);
    }

    /**
     * Determines if the configuration has been loaded.
     * If it is successfuly loaded, callbacks are used to add configuration to the environment.
     * If the configuration is not loaded, return false
     *
     * @param propertySource TRpc loaded configuration
     * @param propertySourceConsumer Callback method
     * @return Whether the configuration is loaded
     */
    private boolean ifAnyLoaded(CompositePropertySource propertySource,
            Consumer<CompositePropertySource> propertySourceConsumer) {
        if (CollectionUtils.isEmpty(propertySource.getPropertySources())) {
            return false;
        }
        propertySourceConsumer.accept(propertySource);
        return true;
    }

    /**
     * Get user-defined configuration path
     *
     * @return Complete configuration path
     */
    private String getCustomPropertySourceLocation() {
        String configPath = TRpcSystemProperties.getProperties(TRpcSystemProperties.CONFIG_PATH);
        if (Objects.isNull(configPath)) {
            return null;
        }
        if (configPath.startsWith(FILE_SCHEMA)) {
            return configPath;
        }
        return FILE_SCHEMA + configPath;
    }

    /**
     * Get TRPC configuration file location list
     *
     * @param environment Environment information
     * @return List of configuration file locations
     */
    private String[] getPropertySourceLocations(Environment environment) {
        String[] activatedProfiles = environment.getActiveProfiles();
        if (ArrayUtils.isEmpty(activatedProfiles)) {
            return DEFAULT_PROPERTY_SOURCE_LOCATIONS;
        }
        Set<String> locations = Stream.of(DEFAULT_PROPERTY_SOURCE_LOCATIONS).collect(Collectors.toSet());
        locations.add(String.format(PROFILE_PROPERTY_SOURCE_FORMAT, activatedProfiles[0]));
        return locations.toArray(new String[0]);
    }
}
