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

package com.tencent.trpc.core.configcenter.spi;

import com.tencent.trpc.core.configcenter.ConfigurationListener;
import com.tencent.trpc.core.exception.ConfigCenterException;
import com.tencent.trpc.core.extension.Extensible;
import java.util.Map;

/**
 * Remote configuration center plugin interface. All adapted configuration center plugins
 * will implement this interface, and uniformly use the methods exposed by this interface
 * to achieve remote configuration acquisition and change monitoring.
 *
 * <p>Different configuration center plugins use the {@code @Extension}
 * annotation to define their plugin names. You can specify the plugin
 * you want to enable by these plugin name, using
 * {@link com.tencent.trpc.core.common.config.ServerConfig#configCenter} and
 * {@link com.tencent.trpc.core.common.config.PluginConfig}.</p>
 *
 * @see com.tencent.trpc.core.extension.Extension
 * @see com.tencent.trpc.core.common.TRpcPluginTypeAlias.SystemPluginType#CONFIG
 * @see com.tencent.trpc.spring.context.configuration.schema.plugin.PluginsSchema#config
 * @see com.tencent.trpc.core.configcenter.ConfigurationManager#getConfigurationLoader
 */
@Extensible("rainbow")
public interface ConfigurationLoader {

    /**
     * Get the value of the specified key corresponding to the KV configuration group.
     *
     * @param key config key
     * @param groupName groupName
     * @return the config value of the key
     * @throws ConfigCenterException ConfigCenterException
     */
    String getValue(String key, String groupName) throws ConfigCenterException;

    /**
     * Get all KV configurations of a group.
     *
     * @param groupName groupName
     * @return all KV configurations of the group
     * @throws ConfigCenterException ConfigCenterException
     */
    Map<String, String> getAllValue(String groupName) throws ConfigCenterException;

    /**
     * Load config content of [file] type, not all config centers support this.
     *
     * @param fileName file name
     * @return config content
     * @throws ConfigCenterException ConfigCenterException
     */
    <T> T loadConfig(String fileName) throws ConfigCenterException;

    /**
     * Add a listener to the configuration.
     *
     * @param listener ConfigurationListener
     */
    void addListener(ConfigurationListener listener);

    /**
     * Remove the added listener.
     *
     * @param listener ConfigurationListener
     */
    void removeListener(ConfigurationListener listener);

}