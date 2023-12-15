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

package com.tencent.trpc.core.rpc;

import com.tencent.trpc.core.common.ConfigManager;
import com.tencent.trpc.core.common.config.BackendConfig;
import com.tencent.trpc.core.common.config.NamingOptions;

/**
 * Proxy utility class.
 */
public class TRpcProxy {

    /**
     * Get the corresponding plugin by name. Returns null if the proxy with the specified name does not exist.
     *
     * @param name the plugin name
     */
    @SuppressWarnings("unchecked")
    public static <T> T getProxy(String name) {
        ConfigManager instance = ConfigManager.getInstance();
        BackendConfig backendConfig = instance.getClientConfig().getBackendConfigMap().get(name);
        if (backendConfig != null) {
            return backendConfig.getDefaultProxy();
        }
        return null;
    }

    public static <T> T getProxy(String name, Class<T> clazz) {
        ConfigManager instance = ConfigManager.getInstance();
        BackendConfig backendConfig = instance.getClientConfig().getBackendConfigMap().get(name);
        if (backendConfig != null) {
            return backendConfig.getProxy(clazz);
        }
        return null;
    }

    /**
     * Specify client set to call, do not use the set of this service itself.
     *
     * @param name service alias
     * @param setName client specified set name
     */
    public static <T> T getProxyWithSourceSet(String name, String setName) {
        ConfigManager instance = ConfigManager.getInstance();
        BackendConfig backendConfig = instance.getClientConfig().getBackendConfigMap().get(name);
        if (backendConfig == null) {
            return null;
        }
        backendConfig.getExtMap().put(NamingOptions.SOURCE_SET, setName);
        return backendConfig.getDefaultProxy();
    }

    /**
     * Specify client set to call, do not use the set of this service itself.
     *
     * @param name service alias
     * @param clazz service interface
     * @param setName client specified set name
     */
    public static <T> T getProxyWithSourceSet(String name, Class<T> clazz, String setName) {
        ConfigManager instance = ConfigManager.getInstance();
        BackendConfig backendConfig = instance.getClientConfig().getBackendConfigMap().get(name);
        if (backendConfig == null) {
            return null;
        }
        backendConfig.getExtMap().put(NamingOptions.SOURCE_SET, setName);
        return backendConfig.getProxy(clazz);
    }

    /**
     * Specify the callee's set name, this callee service must enable set and the set name must be exactly the same.
     *
     * @param name service alias
     * @param setName callee set name
     */
    public static <T> T getProxyWithDestinationSet(String name, String setName) {
        ConfigManager instance = ConfigManager.getInstance();
        BackendConfig backendConfig = instance.getClientConfig().getBackendConfigMap().get(name);
        if (backendConfig == null) {
            return null;
        }
        backendConfig.setDestinationSet(setName);
        return backendConfig.getDefaultProxy();
    }

    /**
     * Specify the callee's set name, this callee service must enable set and the set name must be exactly the same.
     *
     * @param name service alias
     * @param clazz service interface
     * @param setName callee set name
     */
    public static <T> T getProxyWithDestinationSet(String name, Class<T> clazz, String setName) {
        ConfigManager instance = ConfigManager.getInstance();
        BackendConfig backendConfig = instance.getClientConfig().getBackendConfigMap().get(name);
        if (backendConfig == null) {
            return null;
        }
        backendConfig.setDestinationSet(setName);
        return backendConfig.getProxy(clazz);
    }
}