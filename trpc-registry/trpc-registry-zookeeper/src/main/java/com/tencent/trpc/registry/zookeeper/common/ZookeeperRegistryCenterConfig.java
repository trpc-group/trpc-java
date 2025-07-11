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

package com.tencent.trpc.registry.zookeeper.common;

import com.tencent.trpc.core.common.config.PluginConfig;
import com.tencent.trpc.registry.common.RegistryCenterConfig;
import java.util.Map;
import org.apache.commons.collections4.MapUtils;

/**
 * Zookeeper registry configuration items
 */
public class ZookeeperRegistryCenterConfig extends RegistryCenterConfig {

    /**
     * Zookeeper's default namespace
     */
    private String namespace;

    public ZookeeperRegistryCenterConfig(PluginConfig pluginConfig) {
        super(pluginConfig);
        Map<String, Object> properties = pluginConfig.getProperties();
        this.namespace = MapUtils
                .getString(properties, ZookeeperConstants.NAMESPACE_KEY, ZookeeperConstants.DEFAULT_NAMESPACE);
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }
}
