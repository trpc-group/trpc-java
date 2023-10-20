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

package com.tencent.trpc.limiter.sentinel.config.datasource.factory;

import com.tencent.trpc.limiter.sentinel.config.datasource.DatasourceConfig;
import com.tencent.trpc.limiter.sentinel.config.datasource.RedisDatasourceConfig;
import java.util.HashMap;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;

/**
 * RedisDatasourceConfigFactory test class
 */
public class RedisDatasourceConfigFactoryTest {

    @Test
    public void testCreate() {
        Map<String, Object> configs = new HashMap<>();
        configs.put("host", "127.0.0.1");
        configs.put("port", 6379);
        configs.put("db", 1);
        configs.put("channel", "channel");
        configs.put("password", "123");
        configs.put("rule_key", "rule_keys");
        configs.put("client_name", "client_name");
        configs.put("sentinel_master_id", "sentinel_master_id");
        configs.put("timeout", 5000);
        DatasourceConfig datasourceConfig = new RedisDatasourceConfigFactory().create(configs);
        Assert.assertTrue(datasourceConfig instanceof RedisDatasourceConfig);
    }

    @Test
    public void testName() {
        Assert.assertTrue(new RedisDatasourceConfigFactory().name().equals(DatasourceType.REDIS.getName()));
    }

}
