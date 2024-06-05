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

package com.tencent.trpc.limiter.sentinel.config.datasource;

import com.alibaba.csp.sentinel.datasource.ReadableDataSource;
import com.alibaba.csp.sentinel.datasource.redis.RedisDataSource;
import com.alibaba.csp.sentinel.datasource.redis.config.RedisConnectionConfig;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import com.fasterxml.jackson.core.type.TypeReference;
import com.tencent.trpc.core.exception.LimiterDataSourceException;
import com.tencent.trpc.core.utils.JsonUtils;
import com.tencent.trpc.core.utils.StringUtils;
import java.util.List;

/**
 * Configuration class for using Redis as the flow control rule data source.
 */
public class RedisDatasourceConfig extends AbstractDatasourceConfig {

    /**
     * Redis connection basic configuration.
     */
    RedisConnectionConfig redisConnectionConfig;
    /**
     * Redis pipeline name.
     */
    private String channel;
    /**
     * Flow control rule storage key in Redis.
     */
    private String ruleKey;

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public void setRuleKey(String ruleKey) {
        this.ruleKey = ruleKey;
    }

    public void setRedisConnectionConfig(
            RedisConnectionConfig redisConnectionConfig) {
        this.redisConnectionConfig = redisConnectionConfig;
    }

    @Override
    protected void validate() {
        if (StringUtils.isEmpty(ruleKey)) {
            throw new LimiterDataSourceException("sentinel redis datasource config error, rule key is empty");
        }

        if (StringUtils.isEmpty(channel)) {
            throw new LimiterDataSourceException("sentinel redis datasource config error, channel is empty");
        }
    }

    @Override
    public void register() {
        super.register();
        logger.info("start to register redis as sentinel flow rule data source, channel = {}, ruleKey = {}", channel,
                ruleKey);
        ReadableDataSource<String, List<FlowRule>> flowRuleDataSource = new RedisDataSource<>(redisConnectionConfig,
                ruleKey, channel, source -> JsonUtils.fromJson(source, new TypeReference<List<FlowRule>>() {
        }));
        FlowRuleManager.register2Property(flowRuleDataSource.getProperty());
        logger.info("succeed to register redis as sentinel flow rule datasource");
    }

}
