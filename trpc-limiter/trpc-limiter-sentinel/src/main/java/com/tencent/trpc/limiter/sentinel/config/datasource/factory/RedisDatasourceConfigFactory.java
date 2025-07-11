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

package com.tencent.trpc.limiter.sentinel.config.datasource.factory;

import com.alibaba.csp.sentinel.datasource.redis.config.RedisConnectionConfig;
import com.alibaba.csp.sentinel.datasource.redis.config.RedisConnectionConfig.Builder;
import com.tencent.trpc.limiter.sentinel.config.datasource.DatasourceConfig;
import com.tencent.trpc.limiter.sentinel.config.datasource.RedisDatasourceConfig;
import java.util.Map;
import org.apache.commons.collections4.MapUtils;

/**
 * Redis data source configuration factory.
 */
public class RedisDatasourceConfigFactory implements DatasourceConfigFactory {

    /**
     * Redis address configuration item.
     */
    private static final String HOST = "host";
    /**
     * Redis password configuration item.
     */
    private static final String PASSWORD = "password";
    /**
     * Redis database configuration item.
     */
    private static final String DATABASE = "db";
    /**
     * Redis port configuration item.
     */
    private static final String PORT = "port";
    /**
     * Redis channel name configuration item.
     */
    private static final String CHANNEL = "channel";
    /**
     * Redis key configuration item for storing flow control rules.
     */
    private static final String FLOW_RULE_KRY = "rule_key";
    /**
     * Redis connection timeout configuration item, in milliseconds.
     */
    private static final String TIMEOUT_MS = "timeout";
    /**
     * Redis client name configuration item.
     */
    private static final String CLIENT_NAME = "client_name";
    /**
     * Redis sentinel master id configuration item.
     */
    private static final String SENTINEL_MASTER_ID = "sentinel_master_id";

    @Override
    public String name() {
        return DatasourceType.REDIS.getName();
    }

    @Override
    public DatasourceConfig create(Map<String, Object> configs) {
        String channel = MapUtils.getString(configs, CHANNEL);
        String ruleKey = MapUtils.getString(configs, FLOW_RULE_KRY);

        RedisDatasourceConfig redisDataSourceConfig = new RedisDatasourceConfig();
        redisDataSourceConfig.setRuleKey(ruleKey);
        redisDataSourceConfig.setChannel(channel);
        redisDataSourceConfig.setRedisConnectionConfig(buildConnectionConfig(configs));
        return redisDataSourceConfig;
    }

    /**
     * Build redis connection configuration entity.
     */
    private static RedisConnectionConfig buildConnectionConfig(
            Map<String, Object> dataSourceConfigMap) {
        Builder builder = RedisConnectionConfig.builder();
        String host = MapUtils.getString(dataSourceConfigMap, HOST);
        Integer port = MapUtils.getInteger(dataSourceConfigMap, PORT);
        builder.withHost(host).withPort(port);
        if (dataSourceConfigMap.containsKey(CLIENT_NAME)) {
            builder.withClientName(MapUtils.getString(dataSourceConfigMap, CLIENT_NAME));
        }
        if (dataSourceConfigMap.containsKey(SENTINEL_MASTER_ID)) {
            builder.withSentinelMasterId(MapUtils.getString(dataSourceConfigMap, SENTINEL_MASTER_ID));
        }
        if (dataSourceConfigMap.containsKey(PASSWORD)) {
            builder.withPassword(MapUtils.getString(dataSourceConfigMap, PASSWORD));
        }
        if (dataSourceConfigMap.containsKey(DATABASE)) {
            builder.withDatabase(MapUtils.getIntValue(dataSourceConfigMap, DATABASE));
        }
        if (dataSourceConfigMap.containsKey(TIMEOUT_MS)) {
            builder.withTimeout(MapUtils.getLong(dataSourceConfigMap, TIMEOUT_MS));
        }
        return builder.build();
    }

}
