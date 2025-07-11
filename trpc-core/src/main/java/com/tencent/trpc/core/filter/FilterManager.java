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

package com.tencent.trpc.core.filter;

import com.google.common.base.Preconditions;
import com.tencent.trpc.core.common.ConfigManager;
import com.tencent.trpc.core.common.config.PluginConfig;
import com.tencent.trpc.core.extension.ExtensionLoader;
import com.tencent.trpc.core.filter.spi.Filter;

/**
 * Filter Plugin manager.
 */
public class FilterManager {

    /**
     * Load the filter plugin with the specified name.
     *
     * @param name plugin name
     * @return Filter Filter
     */
    public static Filter get(String name) {
        validate(name);
        return ExtensionLoader.getExtensionLoader(Filter.class).getExtension(name);
    }

    /**
     * Validate Check if the filter plugin with the specified name exists. IllegalArgumentException
     *
     * @param name plugin name
     */
    public static void validate(String name) {
        Preconditions.checkArgument(ExtensionLoader.getExtensionLoader(Filter.class).hasExtension(name),
                "Not found filter (name=" + name + ")");
    }

    /**
     * Registers a filter plugin.
     *
     * @param <T> the generic type, which must be a subclass of Filter
     * @param name plugin name
     * @param serviceType the implementation type of the plugin, which must be an instance of T
     */
    public static <T extends Filter> void registerPlugin(String name, Class<T> serviceType) {
        ConfigManager.getInstance().registerPlugin(new PluginConfig(name, Filter.class, serviceType, null));
    }

}