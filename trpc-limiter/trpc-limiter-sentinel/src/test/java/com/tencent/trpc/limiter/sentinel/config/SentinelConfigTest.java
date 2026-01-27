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

import com.tencent.trpc.limiter.sentinel.config.datasource.LocalFileDatasourceConfig;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * SentinelConfig test class
 */
public class SentinelConfigTest {

    @Test
    public void testParse() {
        Map<String, Object> configMap = new HashMap<>();
        Map<String, Object> sentinelMap = new HashMap<>();
        Map<String, Object> datasourceMap = new HashMap<>();
        datasourceMap.put("file", Collections.singletonMap("path", "classpath:flow_rule.json"));
        sentinelMap.put("datasource", datasourceMap);
        configMap.put("sentinel", sentinelMap);
        SentinelConfig sentinelConfig = SentinelConfig.parse(sentinelMap);
        Assertions.assertTrue(sentinelConfig.getDataSourceConfig() instanceof LocalFileDatasourceConfig);
    }

}
