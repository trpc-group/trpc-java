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

package com.tencent.trpc.core.extension;

import com.tencent.trpc.core.common.config.PluginConfig;
import com.tencent.trpc.core.exception.TRpcExtensionException;

/**
 * Plugin PluginConfig injection interface, thread-safe, timing: when ExtensionLoader.getExtension is called.
 */
public interface PluginConfigAware {

    /**
     * Set plugin configuration. When the framework is initialized,
     * this interface will be executed in the lifecycle of the implementation of the PluginConfigAware interface.
     * The execution order of this plugin is earlier than the init interface.
     * The plugin parses the configuration, and the initialization operation is performed through the init interface.
     * Note: the configuration of this plugin may be empty, and it is up to the plugin implementer to determine.
     *
     * @param pluginConfig PluginConfig
     * @throws TRpcExtensionException
     */
    void setPluginConfig(PluginConfig pluginConfig) throws TRpcExtensionException;

}