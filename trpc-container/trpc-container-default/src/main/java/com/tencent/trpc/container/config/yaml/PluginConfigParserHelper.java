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

package com.tencent.trpc.container.config.yaml;

import com.google.common.collect.Maps;
import com.tencent.trpc.container.config.YamlUtils;
import com.tencent.trpc.core.common.TRpcPluginTypeAlias;
import com.tencent.trpc.core.common.config.PluginConfig;
import com.tencent.trpc.core.common.config.builder.PluginConfigBuilder;
import com.tencent.trpc.core.common.config.constant.ConfigConstants;
import com.tencent.trpc.core.extension.ExtensionClass;
import com.tencent.trpc.core.extension.ExtensionLoader;
import com.tencent.trpc.core.utils.PreconditionUtils;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import org.apache.commons.collections4.MapUtils;

public class PluginConfigParserHelper {

    /**
     * Parse plugin configuration.
     *
     * @param position plugin configuration position
     * @param pluginInterfaceClass plugin interface
     * @param config specific plugin configuration
     * @return plugin name configuration mapping
     */
    @SuppressWarnings("unchecked")
    public static Map<String, PluginConfig> parsePluginConfig(String position, Class<?> pluginInterfaceClass,
            Map<String, Object> config) {
        Map<String, PluginConfig> configMap = new HashMap<>();
        YamlUtils yamlUtils = new YamlUtils("label[plugins:" + position + "]");
        for (Entry<String, Object> entry : config.entrySet()) {
            String name = entry.getKey();
            Object value = entry.getValue();
            if (configMap.containsKey(name)) {
                throw new IllegalArgumentException(position + " duplicate plugin name [" + name + "]");
            }
            String pluginType = name;
            Map<String, Object> pluginExtMap = Maps.newHashMap();
            if (Optional.ofNullable(value).isPresent()) {
                pluginExtMap = yamlUtils.requireMap(value, name);
                if (pluginExtMap.containsKey(ConfigConstants.SYSTEM_TYPE)) {
                    pluginType = yamlUtils.getString(pluginExtMap, ConfigConstants.SYSTEM_TYPE);
                }
            }
            ExtensionClass<?> extensionClass = ExtensionLoader.getExtensionLoader(pluginInterfaceClass)
                    .getExtensionClass(pluginType);
            PreconditionUtils.checkArgument(extensionClass != null, position + " Not found plugin(name=%s)", name);
            configMap.put(name, PluginConfigBuilder.newBuilder()
                    .setName(name)
                    .setPluginInterface(pluginInterfaceClass)
                    .setPluginClass(extensionClass.getClazz())
                    .setProperties(pluginExtMap)
                    .build());
        }
        return configMap;
    }

    /**
     * Parse all plugins.
     */
    @SuppressWarnings("unchecked")
    public static Map<Class<?>, Map<String, PluginConfig>> parseAllPluginConfig(Map<String, Object> config) {
        if (MapUtils.isEmpty(config)) {
            return Collections.emptyMap();
        }
        Map<Class<?>, Map<String, PluginConfig>> pluginConfigMap = Maps.newHashMap();
        // alias -> (name -> map)
        config.forEach((k, v) -> {
            Optional<? extends Class<?>> plugin = Optional.ofNullable(TRpcPluginTypeAlias.getPluginInterface(k));
            if (!plugin.isPresent()) {
                throw new IllegalArgumentException("Not found extension loader(typeAlias=" + k + ")");
            }
            if (pluginConfigMap.containsKey(plugin.get())) {
                throw new IllegalArgumentException("Found duplicate plugin config(type=" + plugin.get() + ")");
            }
            pluginConfigMap.put(plugin.get(), parsePluginConfig("plugin(type=" + k + ")", plugin.get(),
                    (Map<String, Object>) v));
        });
        return pluginConfigMap;
    }
    
}
