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

package com.tencent.trpc.registry.nacos.constant;

/**
 * Nacos constant class
 *
 */
public class NacosConstant {

    /**
     * ID SEPARATOR
     */
    public static final String INSTANCE_ID_SEPARATOR = "-";

    /**
     * Metadata URL configuration
     */
    public static final String URL_META_KEY = "url";

    /**
     * Whether to open
     */
    public static final String IS_ENABLE = "is_enable";

    /**
     * Whether to perform health check
     */
    public static final String IS_HEALTH = "is_healthy";

    /**
     * Whether it is valid
     */
    public static final String IS_AVAILABLE = "UP";

    /**
     * Default value of registry center address
     */
    public static final String DEFAULT_REGISTRY_CENTER_ADDRESSED_KEY = "127.0.0.1:8848";
}
