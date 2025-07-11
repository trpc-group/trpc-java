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

package com.tencent.trpc.core.extension;

import com.tencent.trpc.core.common.config.PluginConfig;
import com.tencent.trpc.core.exception.TRpcExtensionException;
import com.tencent.trpc.core.utils.PreconditionUtils;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Used to simplify plugin management and provide management tools.
 *
 * @param <T> plugin type parameter
 */
public class ExtensionManager<T> {

    protected Class<T> extensionClass;

    public ExtensionManager(Class<T> extensionClass) {
        this.extensionClass = Objects.requireNonNull(extensionClass, "extensionClass");
    }

    /**
     * Get the plugin with the name=name, and perform plugin-related initialization work while getting the plugin.
     *
     * @param name plugin name
     * @throws TRpcExtensionException when the specified plugin with the given name does not exist
     */
    public T get(String name) {
        return ExtensionLoader.getExtensionLoader(extensionClass).getExtension(name);
    }

    /**
     * Check if the plugin with the name=name exists.
     *
     * @param name plugin name
     * @return true if the plugin exists, false otherwise
     */
    public boolean hasExtension(String name) {
        return ExtensionLoader.getExtensionLoader(extensionClass).hasExtension(name);
    }

    /**
     * Get the default plugin.
     *
     * @return the default plugin instance
     */
    public T getDefaultExtension() {
        return ExtensionLoader.getExtensionLoader(extensionClass).getDefaultExtension();
    }

    /**
     * Get all instantiated plugins. Return a read-only list to avoid adding plugins through this.
     *
     * @return a list of all instantiated plugins
     */
    public List<T> getAllInitializedExtension() {
        Collection<ExtensionClass<T>> allExtensionClass = ExtensionLoader
                .getExtensionLoader(extensionClass).getAllExtensionClass();
        return allExtensionClass.stream().filter(ExtensionClass::isInitialized)
                .map(ExtensionClass::getExtInstance).collect(Collectors
                        .collectingAndThen(Collectors.toList(), Collections::unmodifiableList));
    }

    /**
     * Check the plugin configuration for the plugin with the name=name. If it does not exist, return null.
     *
     * @param name plugin name
     * @return the plugin configuration if it exists, null otherwise
     */
    public PluginConfig getConfig(String name) {
        return ExtensionLoader.getPluginConfig(extensionClass, name);
    }

    /**
     * Check if the plugin with the name=name exists. If it does not exist, throw an exception.
     *
     * @param name plugin name
     */
    public void validate(String name) {
        PreconditionUtils.checkArgument(hasExtension(name), "Extension(%s) (name=%s) not exist",
                extensionClass.getName(), name);
    }

    /**
     * Register the plugin with the name=name. If the plugin type already exists, throw an exception. If the existing
     * plugin type is the same as the newly registered plugin type, ignore the exception and consider it successful.
     *
     * @param name plugin name
     * @param serviceType plugin implementation class
     */
    public <S extends T> void registPlugin(String name, Class<S> serviceType) {
        ExtensionLoader.registerPlugin(new PluginConfig(name, extensionClass, serviceType, null));
    }

    /**
     * Refresh the configuration of the plugin with the name=name.
     *
     * @param name plugin name
     * @param newConfig new plugin configuration
     */
    public void refresh(String name, PluginConfig newConfig) {
        T t = get(name);
        if (t instanceof RefreshableExtension) {
            ((RefreshableExtension) t).refresh(newConfig);
        }
    }

    /**
     * Get the default plugin name.
     *
     * @return the default plugin name
     */
    public String getDefaultPluginName() {
        return ExtensionLoader.getExtensionLoader(extensionClass).getDefaultExtName();
    }

}
