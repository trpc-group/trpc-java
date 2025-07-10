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
import com.tencent.trpc.limiter.sentinel.config.datasource.ZookeeperDatasourceConfig;
import java.util.HashMap;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;

/**
 * ZookeeperDatasourceConfigFactory test class
 */
public class ZookeeperDatasourceConfigFactoryTest {

    @Test
    public void testCreate() {
        Map<String, Object> configs = new HashMap<>();
        configs.put("remote_address", "127.0.0.1:2181");
        configs.put("path", "/");
        DatasourceConfig datasourceConfig = new ZookeeperDatasourceConfigFactory().create(configs);
        Assert.assertTrue(datasourceConfig instanceof ZookeeperDatasourceConfig);
    }

}
