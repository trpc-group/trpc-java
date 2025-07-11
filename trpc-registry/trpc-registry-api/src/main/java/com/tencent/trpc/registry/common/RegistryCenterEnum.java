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

import static com.tencent.trpc.registry.common.Constants.CONFIGS_REGISTRY_CENTER_SERVICE_TYPE;
import static com.tencent.trpc.registry.common.Constants.CONSUMERS_REGISTRY_CENTER_SERVICE_TYPE;
import static com.tencent.trpc.registry.common.Constants.PROVIDERS_REGISTRY_CENTER_SERVICE_TYPE;
import static com.tencent.trpc.registry.common.Constants.ROUTES_REGISTRY_CENTER_SERVICE_TYPE;

/**
 * The data type of the registry.
 */
public enum RegistryCenterEnum {

    /**
     * Provider data.
     */
    PROVIDERS(PROVIDERS_REGISTRY_CENTER_SERVICE_TYPE),

    /**
     * Consumer data.
     */
    CONSUMERS(CONSUMERS_REGISTRY_CENTER_SERVICE_TYPE),

    /**
     * Route data.
     */
    ROUTES(ROUTES_REGISTRY_CENTER_SERVICE_TYPE),

    /**
     * Configuration data.
     */
    CONFIGS(CONFIGS_REGISTRY_CENTER_SERVICE_TYPE);

    private final String type;

    RegistryCenterEnum(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    public static RegistryCenterEnum transferFrom(String type) {
        for (RegistryCenterEnum registryCenterEnum : RegistryCenterEnum.values()) {
            if (type.equals(registryCenterEnum.getType())) {
                return registryCenterEnum;
            }
        }
        return PROVIDERS;
    }
}
