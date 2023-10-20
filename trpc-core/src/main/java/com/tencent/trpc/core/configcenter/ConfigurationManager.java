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

package com.tencent.trpc.core.configcenter;

import com.tencent.trpc.core.configcenter.spi.ConfigurationLoader;
import com.tencent.trpc.core.extension.ExtensionLoader;

/**
 * Remote configuration center plugin manager, mainly used to
 * get the global singleton instance of @{code ConfigurationLoader}.
 * Using in {@link com.tencent.trpc.server.support.configcenter.ConfigCenterHelper#getConfigurationLoader}.
 *
 * @see com.tencent.trpc.core.configcenter.spi.ConfigurationLoader
 * @see com.tencent.trpc.core.extension.ExtensionLoader
 */
public class ConfigurationManager {

    /**
     * Load singleton instance of ConfigurationLoader by plugin name.
     *
     * @param pluginName plugin name
     * @return ConfigurationLoader
     */
    public static ConfigurationLoader getConfigurationLoader(String pluginName) {
        return ExtensionLoader.getExtensionLoader(ConfigurationLoader.class).getExtension(pluginName);
    }
    
}