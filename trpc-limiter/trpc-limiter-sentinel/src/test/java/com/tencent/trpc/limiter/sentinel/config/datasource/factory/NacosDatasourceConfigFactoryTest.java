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

import com.tencent.trpc.limiter.sentinel.config.datasource.DatasourceConfig;
import com.tencent.trpc.limiter.sentinel.config.datasource.NacosDatasourceConfig;
import java.util.HashMap;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;

/**
 * NacosDatasourceConfigFactory test class
 */
public class NacosDatasourceConfigFactoryTest {

    @Test
    public void testCreate() {
        Map<String, Object> configs = new HashMap<>();
        configs.put("group_id", "test_group_id");
        configs.put("data_id", "test_data_id");
        configs.put("remote_address", "127.0.0.1");
        DatasourceConfig datasourceConfig = new NacosDatasourceConfigFactory().create(configs);
        Assert.assertTrue(datasourceConfig instanceof NacosDatasourceConfig);
    }

}
