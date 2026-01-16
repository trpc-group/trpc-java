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

import com.tencent.trpc.container.config.YamlUtils;
import com.tencent.trpc.core.common.config.PluginConfig;
import com.tencent.trpc.core.common.config.constant.ConfigConstants;
import com.tencent.trpc.core.registry.spi.Registry;
import com.tencent.trpc.core.utils.YamlParser;
import com.tencent.trpc.registry.polaris.PolarisRegistry;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class PluginConfigParserHelperTest {

    @Test
    public void testParseAllPluginConfig() {
        PluginConfigParserHelper pluginConfigParserHelper = new PluginConfigParserHelper();
        Assertions.assertNotNull(pluginConfigParserHelper);
        Map<String, Object> yamlConfigMap =
                YamlParser.parseAsFromClassPath("trpc_java_config.yaml", Map.class);
        YamlUtils yamlUtils = new YamlUtils("Label[]");
        Map<Class<?>, Map<String, PluginConfig>> plugins = PluginConfigParserHelper
                .parseAllPluginConfig(yamlUtils.getMap(yamlConfigMap, ConfigConstants.PLUGINS));
        checkRegistry(plugins.get(Registry.class).get("polaris"));
        try {
            Map mockMap = new HashMap();
            mockMap.put("key", "v");
            PluginConfigParserHelper
                    .parseAllPluginConfig(yamlUtils.getMap(mockMap, ConfigConstants.PLUGINS));
        } catch (Exception e) {
            Assertions.assertTrue(e instanceof IllegalArgumentException);
        }
        try {
            Map<String, Object> mockMap = yamlUtils.getMap(yamlConfigMap, ConfigConstants.PLUGINS);
            Map<String, Object> map = yamlUtils.getMap(yamlConfigMap, ConfigConstants.PLUGINS);
            mockMap.putAll(map);
            PluginConfigParserHelper
                    .parseAllPluginConfig(yamlUtils.getMap(mockMap, ConfigConstants.PLUGINS));
        } catch (Exception e) {
            Assertions.assertTrue(e instanceof IllegalArgumentException);
        }
    }

    @Test
    public void testParsePluginConfig() {
        Map<String, Object> yamlConfigMap =
                YamlParser.parseAsFromClassPath("trpc_java_config.yaml", Map.class);
        Map<String, Object> polaris = (Map<String, Object>) new YamlUtils("Label[]")
                .getMap(yamlConfigMap, ConfigConstants.PLUGINS)
                .get("registry");
        Map<String, PluginConfig> plugins = PluginConfigParserHelper
                .parsePluginConfig("plugin(type=registry)", Registry.class, polaris);
        checkRegistry(plugins.get("polaris"));
        try {
            Map<String, Object> mockMap = new HashMap();
            mockMap.put("key1", "v");
            mockMap.put("key1", "v");
            PluginConfigParserHelper
                    .parsePluginConfig("plugin(type=registry)", Registry.class, mockMap);
        } catch (Exception e) {
            Assertions.assertTrue(e instanceof IllegalArgumentException);
        }
    }

    private void checkRegistry(PluginConfig config) {
        Assertions.assertEquals(config.getName(), "polaris");
        Assertions.assertEquals(config.getPluginInterface(), Registry.class);
        Assertions.assertEquals(config.getPluginClass(), PolarisRegistry.class);
        Assertions.assertEquals(config.getProperties().get("address_list"), "10.235.25.48:8090");
        Assertions.assertEquals(config.getProperties().get("register_self"), false);
        List<Map<String, Object>> services = (List<Map<String, Object>>) config.getProperties()
                .get(ConfigConstants.SERVICE);
        Assertions.assertEquals(services.get(0).get("namespace"), "java-sdk-test-service1");
        Assertions.assertEquals(services.get(0).get("token"), "xxxx");
        Assertions.assertEquals(services.get(0).get("instance_id"),
                "feda4ceffed0b7b08cf5ec665dcd320e50434549");
        Assertions.assertEquals(services.get(0).get("name"), "trpc.TestApp.TestServer.Greeter");
    }

}
