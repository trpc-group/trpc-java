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

package com.tencent.trpc.spring.boot.starters.context.configuration.bind;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.tencent.trpc.core.logger.Logger;
import com.tencent.trpc.core.logger.LoggerFactory;
import com.tencent.trpc.core.utils.JsonUtils;
import com.tencent.trpc.spring.boot.starters.context.configuration.TRpcConfigurationProperties;
import java.util.Collections;
import java.util.Map;

public class TRpcConfigurationBindResult {

    private static final ObjectMapper MAP_CONVERTER = JsonUtils.copy()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE);

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<Map<String, Object>>() {
    };

    private static final Logger logger = LoggerFactory.getLogger(TRpcConfigurationBindResult.class);

    private final TRpcConfigurationProperties data;

    public TRpcConfigurationBindResult(TRpcConfigurationProperties data) {
        this.data = data;
    }

    public TRpcConfigurationProperties getData() {
        return data;
    }

    public Map<String, Object> toMap() {
        try {
            return MAP_CONVERTER.convertValue(data, MAP_TYPE);
        } catch (Throwable e) {
            logger.error("Property bind failed, cause: " + e.getMessage(), e);
            return Collections.emptyMap();
        }
    }
}