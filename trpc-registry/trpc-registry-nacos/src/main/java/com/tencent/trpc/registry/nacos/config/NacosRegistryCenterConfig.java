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

package com.tencent.trpc.registry.nacos.config;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;

import com.tencent.trpc.core.common.config.PluginConfig;
import org.apache.commons.collections4.MapUtils;

/**
 * Configuration of Nacos registry center
 * Support for plugin extension configuration parameters
 */
public class NacosRegistryCenterConfig {

    /**
     * Plugin extension configuration parameters
     */
    private final Map<String, Object> parameters;

    public NacosRegistryCenterConfig(PluginConfig pluginConfig) {
        Objects.requireNonNull(pluginConfig, "the pluginConfig can't be null");
        if (MapUtils.isEmpty(pluginConfig.getProperties())) {
            throw new IllegalArgumentException("the pluginConfig properties can't be empty");
        }
        parameters = pluginConfig.getProperties();
    }

    /**
     * Get configuration based on assertion
     *
     * @param nameToSelect assertion
     * @return Configuration
     */
    public Map<String, String> getParameters(Predicate<String> nameToSelect) {
        Map<String, String> selectedParameters = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : getParameters().entrySet()) {
            String name = entry.getKey();
            if (nameToSelect.test(name)) {
                selectedParameters.put(name, String.valueOf(entry.getValue()));
            }
        }
        return Collections.unmodifiableMap(selectedParameters);
    }

    public Map<String, Object> getParameters() {
        return parameters;
    }
}
