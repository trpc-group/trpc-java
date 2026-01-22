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
import com.tencent.trpc.limiter.sentinel.config.datasource.LocalFileDatasourceConfig;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * LocalFileDatasourceConfigFactory test class
 */
public class LocalFileDatasourceConfigFactoryTest {

    @Test
    public void testCreate() {
        Map<String, Object> configs = new HashMap<>();
        configs.put("path", "classpath:flow_rule.json");
        DatasourceConfig datasourceConfig = new LocalFileDatasourceConfigFactory().create(configs);
        Assertions.assertTrue(datasourceConfig instanceof LocalFileDatasourceConfig);
    }

    @Test
    public void testName() {
        Map<String, Object> configs = new HashMap<>();
        configs.put("path", "classpath:flow_rule.json");
        Assertions.assertTrue(DatasourceType.LOCAL_FILE.getName().equals(new LocalFileDatasourceConfigFactory().name()));
    }

}
