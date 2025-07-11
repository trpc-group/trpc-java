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

public class Constants {

    /**
     * Wildcard for any type.
     */
    public static final String ANY_VALUE = "*";

    /**
     * URL separator.
     */
    public static final String URL_SEPARATOR = ",";

    /**
     * Key for service type in registry data. Each service in the registry is composed of four types:
     * 1. PROVIDERS_REGISTRY_CENTER_TYPE: provider data
     * 2. CONSUMERS_REGISTRY_CENTER_TYPE: consumer data
     * 3. ROUTES_REGISTRY_CENTER_TYPE: route data
     * 4. CONFIGS_REGISTRY_CENTER_TYPE: configuration data
     */
    public static final String REGISTRY_CENTER_SERVICE_TYPE_KEY = "type";

    /**
     * Service type for providers.
     */
    public static final String PROVIDERS_REGISTRY_CENTER_SERVICE_TYPE = "providers";

    /**
     * Service type for consumers.
     */
    public static final String CONSUMERS_REGISTRY_CENTER_SERVICE_TYPE = "consumers";

    /**
     * Service type for routes.
     */
    public static final String ROUTES_REGISTRY_CENTER_SERVICE_TYPE = "routes";

    /**
     * Service type for configurations.
     */
    public static final String CONFIGS_REGISTRY_CENTER_SERVICE_TYPE = "configs";

    /**
     * Default service type.
     */
    public static final String DEFAULT_REGISTRY_CENTER_SERVICE_TYPE = PROVIDERS_REGISTRY_CENTER_SERVICE_TYPE;

    /**
     * Size of the timer wheel for scheduled tasks.
     */
    public static final int TICKS_PER_WHEEL = 128;

    private Constants() {
        throw new IllegalStateException("not support invoke");
    }
}
