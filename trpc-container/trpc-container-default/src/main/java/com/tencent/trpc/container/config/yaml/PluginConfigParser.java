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

import com.tencent.trpc.core.common.config.PluginConfig;
import java.util.Map;

/**
 * Plugin parser.
 */
public class PluginConfigParser {

    /**
     * Parse plugin configuration.
     *
     * @param yamlConfig plugin configuration
     * @return parsed plugin configurations
     */
    public static Map<Class<?>, Map<String, PluginConfig>> parsePlugins(Map<String, Object> yamlConfig) {
        return PluginConfigParserHelper.parseAllPluginConfig(yamlConfig);
    }

}
