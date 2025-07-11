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

package com.tencent.trpc.container.config.yaml;

import com.google.common.collect.Maps;
import com.tencent.trpc.container.config.YamlUtils;
import com.tencent.trpc.core.common.config.PluginConfig;
import com.tencent.trpc.core.common.config.ProviderConfig;
import com.tencent.trpc.core.common.config.ServiceConfig;
import com.tencent.trpc.core.common.config.constant.ConfigConstants;
import com.tencent.trpc.core.registry.spi.Registry;
import com.tencent.trpc.core.utils.BinderUtils;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;

/**
 * ServiceConfig parser utility class.
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class ServiceConfigParser {

    /**
     * Parse server service configuration.
     *
     * @param yamlConfig yaml configuration
     * @param pluginConfigMap plugin configuration
     * @return {@code Map<String, ServiceConfig>}
     */
    public static Map<String, ServiceConfig> parseServiceMapConfig(Collection yamlConfig,
            Map<Class<?>, Map<String, PluginConfig>> pluginConfigMap) {
        Collection<Object> yamlListConfig = (Collection<Object>) yamlConfig;
        return yamlListConfig.stream().map(conf -> parseServiceConfig(conf, pluginConfigMap))
                .collect(Collectors.toMap(ServiceConfig::getName, Function.identity()));
    }

    /**
     * Parse server service configuration.
     *
     * @param yamlConfig yaml configuration
     * @param pluginConfigMap plugin configuration
     * @return parsed ServiceConfig
     */
    public static ServiceConfig parseServiceConfig(Object yamlConfig,
            Map<Class<?>, Map<String, PluginConfig>> pluginConfigMap) {
        ServiceConfig config = new ServiceConfig();
        Map<String, Object> yamlMapConfig = (Map<String, Object>) yamlConfig;
        BinderUtils.bind(config, yamlMapConfig);
        YamlUtils yamlUtils = new YamlUtils("Label[server->service]");
        config.getProviderConfigs().addAll(parseProviderConfig(yamlUtils, yamlMapConfig));
        setRegistryFromRegistryPlugin(config, config.getName(), pluginConfigMap);
        Optional.ofNullable(yamlUtils.getMap(yamlMapConfig, ConfigConstants.REGISTRYS))
                .ifPresent(registries -> registries.forEach((k, v) -> {
                    if (config.getRegistries().containsKey(k)) {
                        throw new IllegalStateException("Yaml config parse exception, position(server->service),"
                                + " found duplicate registries config (key=" + k + ")");
                    }
                    if (Optional.ofNullable(v).isPresent()) {
                        config.getRegistries().put(k, yamlUtils.requireMap(v, ConfigConstants.REGISTRYS));
                    } else {
                        config.getRegistries().put(k, Maps.newHashMap());
                    }
                }));
        return config;
    }

    /**
     * Container publishing platform puts service and token information in the registry configuration. Here is a
     * parsing to parse the content into ServerConfig.
     */
    private static void setRegistryFromRegistryPlugin(ServiceConfig config, String name,
            Map<Class<?>, Map<String, PluginConfig>> pluginConfigMap) {
        Map<String, PluginConfig> routerConfigMap = pluginConfigMap.get(Registry.class);
        Map<String, Map<String, Object>> registries = Maps.newHashMap();
        if (StringUtils.isNotBlank(name) && routerConfigMap != null) {
            routerConfigMap.forEach((key, value) -> {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> services = (List<Map<String, Object>>) value.getProperties()
                        .get(ConfigConstants.SERVICE);
                if (services != null) {
                    services.stream()
                            .filter(s -> Objects.equals(name, s.get(ConfigConstants.NAME)))
                            .forEach(s -> registries.put(key, s));
                }
            });
        }
        config.setRegistries(registries);
    }

    /**
     * Parse the provider.
     * <p>Before version 0.5.5, impls was a list of fully qualified class names of the implementation classes.</p>
     * <p>After version 0.5.5, support for impl-level configuration, that is, the String list is changed to an object
     * list.</p>
     * <p>This parsing is compatible with historical version configuration.</p>
     * That is,
     * <pre>
     * impls:
     *   - xxxx
     *   - xxx
     * </pre>
     * And support the new version
     * <pre>
     * impls:
     *   - impl: xxx
     *     request_timeout: 2000
     *     filters:
     *       - xxx
     *       - xxx
     * </pre>
     *
     * @param yamlUtils utility class
     * @param yamlMapConfig yaml configuration
     * @return parsed ProviderConfig list
     */
    private static List<ProviderConfig> parseProviderConfig(YamlUtils yamlUtils, Map<String, Object> yamlMapConfig) {
        List<?> impls = yamlUtils.getList(yamlMapConfig, ConfigConstants.IMPLS);
        if (impls == null) {
            return Collections.emptyList();
        }

        return impls.stream().map(impl -> {
            ProviderConfig providerConfig = new ProviderConfig();
            if (impl instanceof String) {
                providerConfig.setRefClazz((String) impl);
            } else {
                Map<String, Object> implMap = (Map<String, Object>) impl;
                BinderUtils.bind(providerConfig, implMap);
            }
            return providerConfig;
        }).collect(Collectors.toList());
    }
}
