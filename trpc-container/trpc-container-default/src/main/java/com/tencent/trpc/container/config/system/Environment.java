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

package com.tencent.trpc.container.config.system;

import com.tencent.trpc.container.config.ApplicationConfigParser;
import com.tencent.trpc.container.config.system.parser.PropertySourceParser;
import com.tencent.trpc.core.common.Constants;
import com.tencent.trpc.core.extension.ExtensionLoader;
import com.tencent.trpc.core.logger.Logger;
import com.tencent.trpc.core.logger.LoggerFactory;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.commons.collections4.MapUtils;

/**
 * Environment decorator (mainly for incremental coverage of trpc_java.yaml configuration, allowing users more flexible
 * configuration properties. Priority: system properties > environment variables > yaml configuration).
 *
 * <p>1. Wrap the default ApplicationConfigParser, intercept the parsed map, and override the corresponding properties.
 * The client holds this wrapper class without perception.</p>
 *
 * <p>2. Convert trpc_java.yaml, environment variables, and system properties into a flattened map to implement
 * incremental coverage of yaml configuration.</p>
 *
 * <p>3. Finally, convert the overridden configuration into a map that ConfigManager can parse.</p>
 */
public class Environment implements ApplicationConfigParser, Configuration {

    private static final Logger logger = LoggerFactory.getLogger(Environment.class);
    /**
     * Configuration filter regular expression (global, server, client, plugin).
     */
    private static final Pattern CONFIG_FILTER_PATTERN = Pattern.compile("^((global)|(server)|(client)|(plugin)).*$");

    /**
     * Whether there are property overrides.
     */
    private boolean override = false;
    /**
     * trpc_java.yaml configuration.
     */
    private Map<String, Object> localConfigMap;
    /**
     * trpc_java.yaml configuration (flattened key-value).
     */
    private Map<String, Object> localFlattableConfigMap;
    /**
     * System properties (properties specified by the command line -D).
     */
    private Map<String, Object> systemProperties;
    /**
     * Environment variables.
     */
    private Map<String, Object> environmentProperties;
    /**
     * System and environment property parser.
     */
    private PropertySourceParser propertySourceParser;
    /**
     * Default configuration parser (yaml).
     */
    private ApplicationConfigParser applicationConfigParser;
    /**
     * System property configuration.
     */
    private SystemConfiguration systemConfiguration;
    /**
     * Environment variable configuration.
     */
    private EnvironmentConfiguration environmentConfiguration;

    public Environment(ApplicationConfigParser applicationConfigParser) {
        this.systemConfiguration = new SystemConfiguration();
        this.environmentConfiguration = new EnvironmentConfiguration();
        this.applicationConfigParser = applicationConfigParser;
        this.propertySourceParser = ExtensionLoader.getExtensionLoader(PropertySourceParser.class)
                .getExtension(Constants.DEFAULT);
    }

    @Override
    public Map<String, Object> parseMap(String configPath) {
        this.localConfigMap = this.applicationConfigParser.parseMap(configPath);
        this.overrideConfig();
        if (override) {
            logger.debug("use part of system config instead of local yaml config.");
            return this.propertySourceParser.parseFlattableMap(this.localFlattableConfigMap);
        }
        return this.localConfigMap;
    }

    @Override
    public Map<String, Object> parseMapFromClassPath(String configPath) {
        this.localConfigMap = this.applicationConfigParser.parseMapFromClassPath(configPath);
        this.overrideConfig();
        if (override) {
            return this.propertySourceParser.parseFlattableMap(this.localFlattableConfigMap);
        }
        return this.localConfigMap;
    }

    /**
     * Override configuration (priority: system properties > environment variables > local configuration).
     */
    private void overrideConfig() {
        this.systemProperties = this.filterSystemConfigMap(true);
        this.environmentProperties = this.filterSystemConfigMap(false);
        // multi layer -> single layer
        this.localFlattableConfigMap = propertySourceParser.getFlattableMap(this.localConfigMap);
        if (MapUtils.isEmpty(this.localFlattableConfigMap)) {
            logger.warn("no yaml config after parsed.");
        }

        if (!MapUtils.isEmpty(this.environmentProperties)) {
            this.localFlattableConfigMap.putAll(this.environmentProperties);
            override = true;
        }

        if (!MapUtils.isEmpty(this.systemProperties)) {
            this.localFlattableConfigMap.putAll(this.systemProperties);
            override = true;
        }
    }

    private Map<String, Object> filterSystemConfigMap(boolean useSystemProps) {
        Map<Object, Object> systemConfig = useSystemProps ? System.getProperties() : (Map) System.getenv();
        // Use regular expressions to filter configuration (to prevent parsing other configurations), and wrap it
        // as a map
        return systemConfig.entrySet().stream().filter(entry ->
                CONFIG_FILTER_PATTERN.matcher(String.valueOf(entry.getKey())).find()
        ).collect(Collectors.toMap(entry -> String.valueOf(entry.getKey()), entry -> entry.getValue()));
    }

    /**
     * Actual method to get the property.
     *
     * @param key specified property key
     * @return value
     */
    @Override
    public Object getInternalProperty(String key) {
        // 1. First get the system property specified by -D, if it exists, return it directly
        Optional<String> optional = Optional.ofNullable(this.systemConfiguration).map(conf -> conf.getString(key));
        if (optional.isPresent()) {
            return optional.get();
        }

        // 2. If the property specified by -D does not exist, parse the configuration in the environment variables
        optional = Optional.ofNullable(this.environmentConfiguration).map(conf -> conf.getString(key));
        if (optional.isPresent()) {
            return optional.get();
        }
        return null;
    }

}
