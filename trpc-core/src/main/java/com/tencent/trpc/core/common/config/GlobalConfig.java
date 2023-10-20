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
import com.tencent.trpc.core.common.config.annotation.ConfigProperty;
import java.util.Map;

/**
 * Framework global configuration information.
 */
public class GlobalConfig {

    /**
     * Environment type, divided into formal production and informal.
     */
    @ConfigProperty
    protected String namespace;
    /**
     * Environment name, the name of multiple environments in an informal environment.
     */
    @ConfigProperty
    protected String envName;
    /**
     * Whether to enable set-based routing.
     */
    protected boolean enableSet;
    /**
     * Set name.
     */
    @ConfigProperty
    protected String fullSetName;
    /**
     * Container name.
     */
    @ConfigProperty
    protected String containerName;
    /**
     * Global extension parameters.
     */
    @ConfigProperty
    protected Map<String, Object> ext = Maps.newHashMap();
    /**
     * Whether the service is registered.
     */
    protected volatile boolean setDefault = false;

    /**
     * Set default values.
     */
    public synchronized void setDefault() {
        if (!setDefault) {
            setDefault = true;
        }
    }

    protected void checkFiledModifyPrivilege() {
    }

    public String getNamespace() {
        return namespace;
    }

    public GlobalConfig setNamespace(String namespace) {
        this.namespace = namespace;
        return this;
    }

    public boolean isSetDefault() {
        return setDefault;
    }

    public String getEnvName() {
        return envName;
    }

    public GlobalConfig setEnvName(String envName) {
        this.envName = envName;
        return this;
    }

    public boolean isEnableSet() {
        return enableSet;
    }

    public GlobalConfig setEnableSet(boolean enableSet) {
        this.enableSet = enableSet;
        return this;
    }

    public String getFullSetName() {
        return fullSetName;
    }

    public GlobalConfig setFullSetName(String fullSetName) {
        this.fullSetName = fullSetName;
        return this;
    }

    public String getContainerName() {
        return containerName;
    }

    public GlobalConfig setContainerName(String containerName) {
        this.containerName = containerName;
        return this;
    }

    public Map<String, Object> getExt() {
        return ext;
    }

    public GlobalConfig setExt(Map<String, Object> ext) {
        this.ext = ext;
        return this;
    }

}
