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

package com.tencent.trpc.core.common.config.builder;

import com.tencent.trpc.core.common.config.PluginConfig;
import java.util.HashMap;
import java.util.Map;

public class PluginConfigBuilder {

    /**
     * Plugin name (within a plugin type, the name is a unique identifier for a specific plugin object).
     */
    protected String name;

    protected Class pluginInterface;

    protected Class pluginClass;

    protected Map<String, Object> properties;

    public static PluginConfigBuilder newBuilder() {
        return new PluginConfigBuilder();
    }

    public PluginConfig build() {
        return new PluginConfig(name, pluginInterface, pluginClass, properties);
    }

    public PluginConfigBuilder addPropertie(String key, String value) {
        if (properties == null) {
            properties = new HashMap<>();
        }
        properties.put(key, value);
        return this;
    }

    public String getName() {
        return name;
    }

    public PluginConfigBuilder setName(String name) {
        this.name = name;
        return this;
    }

    public Class getPluginInterface() {
        return pluginInterface;
    }

    public PluginConfigBuilder setPluginInterface(Class pluginInterface) {
        this.pluginInterface = pluginInterface;
        return this;
    }

    public Class getPluginClass() {
        return pluginClass;
    }

    public PluginConfigBuilder setPluginClass(Class pluginClass) {
        this.pluginClass = pluginClass;
        return this;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public PluginConfigBuilder setProperties(Map<String, Object> properties) {
        this.properties = properties;
        return this;
    }

}