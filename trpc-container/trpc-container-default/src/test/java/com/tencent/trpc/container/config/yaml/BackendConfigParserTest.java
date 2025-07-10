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

package com.tencent.trpc.container.config.yaml;

import com.tencent.trpc.core.common.config.BackendConfig;
import com.tencent.trpc.core.utils.YamlParser;
import org.junit.Assert;
import org.junit.Test;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * BackendConfig parser.
 */
public class BackendConfigParserTest {

    @Test
    public void testParseConfigMap() {
        BackendConfigParser backendConfigParser = new BackendConfigParser();
        Assert.assertNotNull(backendConfigParser);
        Map<String, Object> yamlConfigMap = YamlParser.parseAsFromClassPath("trpc_java_config.yaml", Map.class);
        List<Map<String, Object>> maps = Arrays.asList(yamlConfigMap);
        Map<String, BackendConfig> stringBackendConfigMap = BackendConfigParser.parseConfigMap(maps);
        Assert.assertNotNull(stringBackendConfigMap);
    }

    @Test
    public void testParseConfig() {
        Map<String, Object> yamlConfigMap = YamlParser.parseAsFromClassPath("trpc_java_config.yaml", Map.class);
        BackendConfig backendConfig = BackendConfigParser.parseConfig(yamlConfigMap);
        Assert.assertNotNull(backendConfig);
    }

}
