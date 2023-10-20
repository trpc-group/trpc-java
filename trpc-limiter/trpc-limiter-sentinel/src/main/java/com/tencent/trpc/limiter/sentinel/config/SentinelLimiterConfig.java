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

package com.tencent.trpc.limiter.sentinel.config;

import java.util.Map;
import org.apache.commons.collections4.MapUtils;

/**
 * Sentinel configuration entity class corresponding to the limiter_config, used to configure flow control callback,
 * degradation, and resource parsing plugins.
 */
public class SentinelLimiterConfig {

    /**
     * Flow control resource identifier parser plugin name configuration item.
     */
    public static final String RESOURCE_EXTRACTOR = "resource_extractor";
    /**
     * Flow control related plugin configuration item.
     */
    private static final String LIMITER_CONFIG = "limiter_config";
    /**
     * Flow control callback plugin name configuration item.
     */
    private static final String BLOCK_HANDLER = "block_handler";
    /**
     * Flow control degradation plugin name configuration item.
     */
    private static final String FALLBACK = "fallback";
    private static final String DEFAULT_BLOCK_HANDLER_NAME = "default";
    private static final String DEFAULT_FALLBACK_NAME = "default";
    private static final String DEFAULT_RESOURCE_EXTRACTOR_NAME = "default";
    /**
     * Flow control callback plugin name.
     */
    private String blockHandler;
    /**
     * Degradation plugin name.
     */
    private String fallback;
    /**
     * Flow control resource identifier parser plugin.
     */
    private String resourceExtractor;

    public static SentinelLimiterConfig parse(Map<String, Object> yamlMapConfig) {
        Map<String, Object> configMap = (Map<String, Object>) MapUtils.getMap(yamlMapConfig, LIMITER_CONFIG);
        SentinelLimiterConfig limiterConfig = new SentinelLimiterConfig();
        limiterConfig.setBlockHandler(MapUtils.getString(configMap, BLOCK_HANDLER, DEFAULT_BLOCK_HANDLER_NAME));
        limiterConfig.setFallback(MapUtils.getString(configMap, FALLBACK, DEFAULT_FALLBACK_NAME));
        limiterConfig.setResourceExtractor(MapUtils.getString(configMap, RESOURCE_EXTRACTOR,
                DEFAULT_RESOURCE_EXTRACTOR_NAME));
        return limiterConfig;
    }

    public String getBlockHandler() {
        return blockHandler;
    }

    public void setBlockHandler(String blockHandler) {
        this.blockHandler = blockHandler;
    }

    public String getFallback() {
        return fallback;
    }

    public void setFallback(String fallback) {
        this.fallback = fallback;
    }

    public String getResourceExtractor() {
        return resourceExtractor;
    }

    public void setResourceExtractor(String resourceExtractor) {
        this.resourceExtractor = resourceExtractor;
    }

}
