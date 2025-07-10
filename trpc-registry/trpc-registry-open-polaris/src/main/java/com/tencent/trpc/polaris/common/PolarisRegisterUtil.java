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

package com.tencent.trpc.polaris.common;

import static com.tencent.trpc.polaris.common.PolarisRegistryConstant.POLARIS_ADDRESSES_KEY;
import static com.tencent.trpc.polaris.common.PolarisRegistryConstant.POLARIS_API_BIND_IF_KEY;
import static com.tencent.trpc.polaris.common.PolarisRegistryConstant.POLARIS_API_MAX_RETRY_TIMES_KEY;
import static com.tencent.trpc.polaris.common.PolarisRegistryConstant.POLARIS_PROTOCOL_KEY;

import com.google.common.collect.Maps;
import com.tencent.polaris.factory.config.ConfigurationImpl;
import com.tencent.polaris.factory.config.global.APIConfigImpl;
import com.tencent.polaris.factory.config.global.GlobalConfigImpl;
import com.tencent.polaris.factory.config.global.ServerConnectorConfigImpl;
import com.tencent.trpc.core.utils.JsonUtils;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.collections4.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Polaris service registration utility class, used for object conversion, etc.
 */
public class PolarisRegisterUtil {

    private static Logger logger = LoggerFactory.getLogger(PolarisRegisterUtil.class);

    /**
     * Convert a Map with Object values to a Map with String values.
     */
    public static Map<String, String> trans2StringMap(Object object) {
        if (object == null || !(object instanceof Map)) {
            logger.debug("metadata is empty or param error. metadata:{}", object);
            return Maps.newHashMap();
        }
        Map<String, String> originMap = (Map) object;
        return originMap.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, val -> {
            Object value = val.getValue();
            String strValue;
            try {
                strValue = ((value instanceof String) ? (String) value : JsonUtils.toJson(value));
            } catch (Exception e) {
                logger.error("to json failed. value type:{}", value);
                strValue = String.valueOf(value);
            }
            return strValue;
        }));
    }

    /**
     * Update the Polaris configuration object ApiConfig with the Polaris-related configuration provided by TRPC.
     *
     * @param apiConfig The Polaris configuration object.
     * @param extMap The TRPC configuration map.
     */
    public static void overrideApiConfig(APIConfigImpl apiConfig, Map<String, Object> extMap) {
        if (MapUtils.isNotEmpty(extMap)) {
            apiConfig.setMaxRetryTimes(
                    MapUtils.getIntValue(extMap, POLARIS_API_MAX_RETRY_TIMES_KEY, apiConfig.getMaxRetryTimes()));
            apiConfig.setBindIf(MapUtils.getString(extMap, POLARIS_API_BIND_IF_KEY, apiConfig.getBindIf()));
        }
    }

    /**
     * Update the Polaris configuration object ServerConnectorConfig with the Polaris-related configuration provided by
     * TRPC.
     *
     * @param serverConnectorConfig The Polaris configuration object.
     * @param extMap The TRPC plugin configuration.
     */
    public static void overrideServerConnectorConfig(ServerConnectorConfigImpl serverConnectorConfig,
            Map<String, Object> extMap) {
        if (MapUtils.isNotEmpty(extMap)) {
            if (extMap.containsKey(POLARIS_ADDRESSES_KEY)) {
                serverConnectorConfig.setAddresses(
                        Arrays.asList((MapUtils.getString(extMap, POLARIS_ADDRESSES_KEY)).split(",")));
            }
            serverConnectorConfig.setProtocol(
                    MapUtils.getString(extMap, POLARIS_PROTOCOL_KEY, serverConnectorConfig.getProtocol()));
        }
    }

    /**
     * Generate the Polaris configuration object based on the plugin configuration.
     *
     * @param extMap The plugin configuration.
     * @return The Polaris configuration object.
     */
    public static ConfigurationImpl genPolarisConfiguration(Map<String, Object> extMap) {
        ConfigurationImpl configuration = JsonUtils.convertValue(extMap, ConfigurationImpl.class);
        configuration.setDefault();
        GlobalConfigImpl globalConfig = configuration.getGlobal();
        APIConfigImpl apiConfig = globalConfig.getAPI();
        ServerConnectorConfigImpl serverConnectorConfig = globalConfig.getServerConnector();
        // The Polaris configuration is too complex, so commonly used configuration is placed in the first level.
        PolarisRegisterUtil.overrideApiConfig(apiConfig, extMap);
        PolarisRegisterUtil.overrideServerConnectorConfig(serverConnectorConfig, extMap);
        return configuration;
    }
}
