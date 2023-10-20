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

package com.tencent.trpc.core.common.config;

import com.google.common.collect.Maps;
import com.tencent.trpc.core.extension.Extensible;
import com.tencent.trpc.core.utils.ClassUtils;
import com.tencent.trpc.core.utils.PreconditionUtils;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Plugin configuration class.
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class PluginConfig {

    /**
     * Plugin name (under a plugin type, the name is a unique identifier for a specific plugin object).
     */
    protected String name;
    /**
     * Plugin interface.
     */
    protected Class pluginInterface;
    /**
     * Plugin implementation class.
     */
    protected Class pluginClass;
    /**
     * Plugin extension configuration parameters.
     */
    protected Map<String, Object> properties;

    public PluginConfig(String name, Class pluginClass) {
        this(name, null, pluginClass, null);
    }

    public PluginConfig(String name, Class pluginClass, Map<String, Object> properties) {
        this(name, null, pluginClass, properties);
    }

    public PluginConfig(String name, Class pluginInterface, Class pluginClass) {
        this(name, pluginInterface, pluginClass, null);
    }

    public PluginConfig(String name, Class pluginInterface, Class pluginClass,
            Map<String, Object> properties) {
        super();
        Objects.requireNonNull(name, "name is null");
        Objects.requireNonNull(pluginClass, "service is null");
        if (pluginInterface == null) {
            this.pluginInterface = parsePluginServiceInterface(pluginClass);
        } else {
            this.pluginInterface = pluginInterface;
        }
        this.name = name;
        this.pluginClass = pluginClass;
        this.properties = (properties == null ? Maps.newHashMap() : properties);
        validate();
    }

    protected void validate() {
        PreconditionUtils.checkArgument(pluginInterface.isAssignableFrom(pluginClass),
                "Plugin class(%s) is not sub class of (%s), plugin config", pluginClass,
                pluginInterface);
    }

    protected Class parsePluginServiceInterface(Class service) {
        List<Class> interfaces = ClassUtils.getAllInterfaces(service);
        if (interfaces.size() < 1) {
            throw new IllegalArgumentException(
                    "Parse plugin service interface failed, serviceImpl(" + service
                            + ") no interface");
        }
        for (Class each : interfaces) {
            if (each.getAnnotation(Extensible.class) != null) {
                return each;
            }
        }
        throw new IllegalArgumentException("Parse plugin service interface failed, serviceImpl ("
                + service + ") could not found a interface which has Extendable annotaion");
    }

    @Override
    public String toString() {
        return "PluginConfig {name=" + name + ", pluginInterface=" + pluginInterface
                + ", pluginClass="
                + pluginClass + ", properties=" + properties + "}";
    }

    public String toSimpleString() {
        return "PluginConfig {name=" + name + ", pluginInterface=" + pluginInterface
                + ", pluginClass="
                + pluginClass + "}";
    }

    public String getName() {
        return name;
    }

    public Class getPluginInterface() {
        return pluginInterface;
    }

    public Class getPluginClass() {
        return pluginClass;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

}
