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

package com.tencent.trpc.registry.common;

import static com.tencent.trpc.registry.common.Constants.DEFAULT_REGISTRY_CENTER_SERVICE_TYPE;
import static com.tencent.trpc.registry.common.Constants.REGISTRY_CENTER_SERVICE_TYPE_KEY;

import com.tencent.trpc.core.registry.RegisterInfo;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.commons.collections4.MapUtils;

/**
 * Data types of the registry: four types and their corresponding data mappings.
 * PROVIDERS provider type
 * CONSUMERS consumer type
 * ROUTES route type (to be implemented with specific functions)
 * CONFIGS configuration type (to be implemented with specific functions)
 * For example, consumers will subscribe to the providers node to obtain service providers, and will also subscribe to
 * the routes node to obtain route configurations.
 */
public class RegistryCenterData {

    /**
     * Mapping of registry types and their corresponding data
     */
    private final Map<RegistryCenterEnum, Set<RegisterInfo>> typeToRegisterInfosMap = new ConcurrentHashMap<>();

    /**
     * Mapping of PROVIDERS key-value pairs created by default.
     */
    public RegistryCenterData() {
        this(RegistryCenterEnum.PROVIDERS);
    }

    /**
     * Create a mapping of the specified type.
     *
     * @param type The data type of the registry.
     */
    public RegistryCenterData(RegistryCenterEnum type) {
        typeToRegisterInfosMap.computeIfAbsent(type, t -> new HashSet<>());
    }

    /**
     * Get a mapping of all registry types and their corresponding data.
     *
     * @return A mapping of all registry types and their corresponding data.
     */
    public Map<RegistryCenterEnum, Set<RegisterInfo>> getTypeToRegisterInfosMap() {
        return typeToRegisterInfosMap;
    }

    /**
     * Get the data corresponding to the specified registry type.
     *
     * @param type The registry type.
     * @return The data corresponding to the specified registry type.
     */
    public Set<RegisterInfo> getRegisterInfos(RegistryCenterEnum type) {
        return typeToRegisterInfosMap.computeIfAbsent(type, t -> new HashSet<>());
    }

    /**
     * Add data for the specified registry type.
     *
     * @param type The registry type.
     * @param registerInfo The data to be added.
     */
    public void putRegisterInfo(RegistryCenterEnum type, RegisterInfo registerInfo) {
        getRegisterInfos(type).add(registerInfo);
    }

    /**
     * Add data in bulk.
     *
     * @param registerInfos The list of data to be added.
     */
    public void putAllRegisterInfos(List<RegisterInfo> registerInfos) {
        registerInfos.forEach(ri -> {
            String type = ri.getParameter(REGISTRY_CENTER_SERVICE_TYPE_KEY, DEFAULT_REGISTRY_CENTER_SERVICE_TYPE);
            putRegisterInfo(RegistryCenterEnum.transferFrom(type), ri);
        });
    }

    /**
     * Check if the registry data is empty. This method iterates through all types of data to make the determination.
     *
     * @return true if empty, false if not empty.
     */
    public boolean isEmpty() {
        if (MapUtils.isEmpty(typeToRegisterInfosMap)) {
            return true;
        }
        return typeToRegisterInfosMap.values().stream().map(Set::isEmpty).reduce(true, (r1, r2) -> r1 && r2);
    }
}
