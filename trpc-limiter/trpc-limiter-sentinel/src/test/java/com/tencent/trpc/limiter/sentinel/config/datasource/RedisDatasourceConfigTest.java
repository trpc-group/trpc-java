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

import com.tencent.trpc.core.exception.LimiterDataSourceException;
import com.tencent.trpc.limiter.sentinel.config.datasource.factory.DatasourceConfigFactoryManger;
import com.tencent.trpc.limiter.sentinel.config.datasource.factory.DatasourceType;
import java.util.HashMap;
import java.util.Map;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import redis.embedded.RedisServer;

/**
 * RedisDatasourceConfig test class
 */
public class RedisDatasourceConfigTest {

    private RedisServer redisServer;
    private int port = 6380;

    @Before
    public void setUp() {

        redisServer = RedisServer.builder().setting("maxmemory 128M").setting("bind localhost")
                .port(port).build();
        redisServer.start();
    }

    @Test
    public void testRegister() {
        Map<String, Object> configMap = new HashMap<>();
        configMap.put("host", "127.0.0.1");
        configMap.put("port", port);
        configMap.put("rule_key", "sentinel_flow");
        configMap.put("channel", "my_channel");
        RedisDatasourceConfig redisDatasourceConfig = (RedisDatasourceConfig) DatasourceConfigFactoryManger
                .getDatasourceConfigFactory(DatasourceType.REDIS.getName()).create(configMap);
        redisDatasourceConfig.register();
    }

    @Test
    public void testValidate1() {
        Map<String, Object> configMap1 = new HashMap<>();
        configMap1.put("host", "127.0.0.1");
        configMap1.put("port", port);
        configMap1.put("rule_key", "sentinel_flow");
        RedisDatasourceConfig configWithoutRuleKey1 = (RedisDatasourceConfig) DatasourceConfigFactoryManger
                .getDatasourceConfigFactory(DatasourceType.REDIS.getName()).create(configMap1);
        try {
            configWithoutRuleKey1.validate();
        } catch (Exception e) {
            Assert.assertTrue(e instanceof LimiterDataSourceException);
            Assert.assertTrue(e.getMessage().equals("sentinel redis datasource config error, channel is empty"));
        }
    }


    @Test
    public void testValidate2() {
        Map<String, Object> configMap = new HashMap<>();
        configMap.put("host", "127.0.0.1");
        configMap.put("port", port);
        configMap.put("channel", "my_channel");
        RedisDatasourceConfig configWithoutRuleKey = (RedisDatasourceConfig) DatasourceConfigFactoryManger
                .getDatasourceConfigFactory(DatasourceType.REDIS.getName()).create(configMap);
        try {
            configWithoutRuleKey.validate();
        } catch (Exception e) {
            Assert.assertTrue(e instanceof LimiterDataSourceException);
            Assert.assertTrue(e.getMessage().equals("sentinel redis datasource config error, rule key is empty"));
        }
    }

    @After
    public void after() {
        if (redisServer != null) {
            redisServer.stop();
        }
    }

}
