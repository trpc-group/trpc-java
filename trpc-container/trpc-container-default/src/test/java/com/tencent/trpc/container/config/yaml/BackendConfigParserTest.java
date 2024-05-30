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

package com.tencent.trpc.container.config.yaml;

import com.tencent.trpc.core.common.config.BackendConfig;
import com.tencent.trpc.core.common.config.constant.ConfigConstants;
import com.tencent.trpc.core.utils.BinderUtils;
import com.tencent.trpc.core.utils.ClassLoaderUtils;
import com.tencent.trpc.core.utils.YamlParser;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * BackendConfig parser.
 */
public class BackendConfigParserTest {

    @Test
    public void testParseConfigMap(){
        Map<String, Object> yamlConfigMap = YamlParser.parseAsFromClassPath("trpc_java_config.yaml", Map.class);
        List<Map<String, Object>> maps = Arrays.asList(yamlConfigMap);
        Map<String, BackendConfig> stringBackendConfigMap = BackendConfigParser.parseConfigMap(maps);
        Assert.assertNotNull(stringBackendConfigMap);
    }

    @Test
    public void testParseConfig(){
        Map<String, Object> yamlConfigMap = YamlParser.parseAsFromClassPath("trpc_java_config.yaml", Map.class);
        BackendConfig backendConfig = BackendConfigParser.parseConfig(yamlConfigMap);
        Assert.assertNotNull(backendConfig);
    }

}
