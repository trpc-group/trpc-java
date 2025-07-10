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

package com.tencent.trpc.container.config;

import com.tencent.trpc.container.config.yaml.ClientConfigParser;
import com.tencent.trpc.container.config.yaml.GlobalConfigParser;
import com.tencent.trpc.container.config.yaml.PluginConfigParser;
import com.tencent.trpc.container.config.yaml.ServerConfigParser;
import com.tencent.trpc.core.common.ConfigManager;
import com.tencent.trpc.core.common.config.PluginConfig;
import com.tencent.trpc.core.common.config.constant.ConfigConstants;
import com.tencent.trpc.core.extension.Extensible;
import java.util.Map;

/**
 * Application configuration parsing SPI.
 */
@Extensible("yaml")
public interface ApplicationConfigParser {

    /**
     * Parse the configuration file at the trpc_config_path parameter path. If it does not exist, load the
     * resources/trpc_java.yaml file.
     *
     * <p>The default is to convert yaml to a map, and then inject properties into ConfigManger through doParse. The
     * default implementation is used so that the client does not perceive it, but it also needs to expose the
     * extension capability.</p>
     *
     * @return parsed ConfigManager
     */
    default ConfigManager parse() {
        return this.doParse(parseMap(null));
    }

    /**
     * Parse the specified path trpc_java.yaml file. The reason for using the default implementation is the same
     * as parse().
     *
     * @param configPath full path of the configuration file
     * @return parsed ConfigManager
     */
    default ConfigManager parse(String configPath) {
        return this.doParse(parseMap(configPath));
    }

    /**
     * Parse the file of configPath in the classPath. The reason for using the default implementation is the same
     * as parse().
     *
     * @param configPath relative path of the configuration file
     * @return parsed ConfigManager
     */
    default ConfigManager parseFromClassPath(String configPath) {
        return this.doParse(parseMapFromClassPath(configPath));
    }

    /**
     * Default parsing of yaml map config.
     *
     * @param yamlMapConfig yaml configuration
     * @return parsed ConfigManager
     */
    default ConfigManager doParse(Map<String, Object> yamlMapConfig) {
        ConfigManager configManager = ConfigManager.getInstance();
        YamlUtils yamlUtils = new YamlUtils("Label[]");
        // parse global config
        configManager.setGlobalConfig(GlobalConfigParser.parseGlobalConfig(yamlUtils.getMap(yamlMapConfig,
                ConfigConstants.GLOBAL)));
        // parse plugins config
        Map<Class<?>, Map<String, PluginConfig>> pluginConfigs = PluginConfigParser.parsePlugins(
                yamlUtils.getMap(yamlMapConfig, ConfigConstants.PLUGINS));
        configManager.getPluginConfigMap().putAll(pluginConfigs);
        // parse server config
        configManager.setServerConfig(ServerConfigParser.parseServerConfig(yamlUtils.getMap(yamlMapConfig,
                ConfigConstants.SERVER), pluginConfigs));
        // parse client config
        configManager.setClientConfig(ClientConfigParser.parseClientConfig(yamlUtils.getMap(yamlMapConfig,
                ConfigConstants.CLIENT)));
        return configManager;
    }

    /**
     * Parse the specified path trpc_java.yaml file.
     *
     * @param configPath parse trpc_java.yaml from the configPath
     * @return yaml configuration map
     */
    Map<String, Object> parseMap(String configPath);

    /**
     * Parse the file of configPath in the classPath.
     *
     * @param configPath parse trpc_java.yaml from the classpath
     * @return yaml configuration map
     */
    Map<String, Object> parseMapFromClassPath(String configPath);
    
}
