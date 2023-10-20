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

import com.tencent.trpc.limiter.sentinel.config.datasource.DatasourceConfig;
import com.tencent.trpc.limiter.sentinel.config.datasource.factory.DatasourceConfigFactoryManger;
import java.util.Map;
import org.apache.commons.collections4.MapUtils;

/**
 * Sentinel configuration entity class corresponding to the YAML configuration.
 */
public class SentinelConfig {

    /**
     * Sentinel flow control rule data source configuration.
     */
    private static final String DATASOURCE = "datasource";

    /**
     * Flow control rule data source configuration.
     */
    private DatasourceConfig dataSourceConfig;
    /**
     * Flow control callback plugin, degradation plugin, flow control resource identifier parsing plugin configuration.
     */
    private SentinelLimiterConfig limiterConfig;

    /**
     * Parse sentinel configuration and generate corresponding entity class.
     *
     * @param yamlMapConfig sentinel corresponding YAML configuration
     * @return sentinel configuration corresponding entity class
     */
    public static SentinelConfig parse(Map<String, Object> yamlMapConfig) {
        SentinelConfig sentinelConfig = new SentinelConfig();
        if (MapUtils.isEmpty(yamlMapConfig)) {
            return sentinelConfig;
        }

        sentinelConfig.setDataSourceConfig(parseDataSourceConfig(yamlMapConfig));
        sentinelConfig.setLimiterConfig(SentinelLimiterConfig.parse(yamlMapConfig));
        return sentinelConfig;
    }

    private static DatasourceConfig parseDataSourceConfig(Map<String, Object> yamlMapConfig) {
        Map<String, Object> datasourceMapConfig = (Map<String, Object>) MapUtils.getMap(yamlMapConfig, DATASOURCE);
        if (MapUtils.isEmpty(datasourceMapConfig)) {
            return null;
        }
        String datasourceName = datasourceMapConfig.keySet().iterator().next();
        Map<String, Object> configMap = (Map<String, Object>) MapUtils.getMap(datasourceMapConfig, datasourceName);
        return DatasourceConfigFactoryManger.getDatasourceConfigFactory(datasourceName).create(configMap);
    }

    public DatasourceConfig getDataSourceConfig() {
        return dataSourceConfig;
    }

    public void setDataSourceConfig(DatasourceConfig dataSourceConfig) {
        this.dataSourceConfig = dataSourceConfig;
    }

    public SentinelLimiterConfig getLimiterConfig() {
        return limiterConfig;
    }

    public void setLimiterConfig(SentinelLimiterConfig limiterConfig) {
        this.limiterConfig = limiterConfig;
    }

}
