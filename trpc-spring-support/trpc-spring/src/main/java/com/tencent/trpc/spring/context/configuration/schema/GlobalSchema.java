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

package com.tencent.trpc.spring.context.configuration.schema;

import com.google.common.collect.Maps;
import java.util.Map;

/**
 * Global Configurations
 *
 * @see com.tencent.trpc.core.common.config.GlobalConfig
 */
public class GlobalSchema {

    /**
     * Namespace, Production/Development
     */
    private String namespace;

    /**
     * Development environment name
     */
    private String envName;

    /**
     * Enable set router
     */
    private YesOrNo enableSet;

    /**
     * FullSetName
     */
    private String fullSetName;

    /**
     * Container name
     */
    private String containerName;

    /**
     * Extension configs
     */
    private Map<String, Object> ext = Maps.newHashMap();

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getEnvName() {
        return envName;
    }

    public void setEnvName(String envName) {
        this.envName = envName;
    }

    public YesOrNo getEnableSet() {
        return enableSet;
    }

    public void setEnableSet(YesOrNo enableSet) {
        this.enableSet = enableSet;
    }

    public String getFullSetName() {
        return fullSetName;
    }

    public void setFullSetName(String fullSetName) {
        this.fullSetName = fullSetName;
    }

    public String getContainerName() {
        return containerName;
    }

    public void setContainerName(String containerName) {
        this.containerName = containerName;
    }

    public Map<String, Object> getExt() {
        return ext;
    }

    public void setExt(Map<String, Object> ext) {
        this.ext = ext;
    }

    @Override
    public String toString() {
        return "GlobalSchema{" +
                "namespace='" + namespace + '\'' +
                ", envName='" + envName + '\'' +
                ", enableSet=" + enableSet +
                ", fullSetName='" + fullSetName + '\'' +
                ", containerName='" + containerName + '\'' +
                ", ext=" + ext +
                '}';
    }
}
