package com.tencent.trpc.container.config.yaml;

import com.tencent.trpc.container.config.YamlUtils;
import com.tencent.trpc.core.common.config.PluginConfig;
import com.tencent.trpc.core.common.config.ServiceConfig;
import com.tencent.trpc.core.common.config.constant.ConfigConstants;
import com.tencent.trpc.core.utils.YamlParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;
import java.util.Arrays;
import java.util.Map;

public class ServiceConfigParserTest {

    @Test
    public void testParseServiceMapConfig() {
        ServiceConfigParser serviceConfigParser = new ServiceConfigParser();
        Assertions.assertNotNull(serviceConfigParser);
        Map<String, Object> yamlConfigMap = YamlParser.parseAsFromClassPath("listener_default.yaml", Map.class);
        YamlUtils yamlUtils = new YamlUtils("Label[]");
        Map<String, Object> objectMap = yamlUtils.getMap(yamlConfigMap, ConfigConstants.PLUGINS);
        Map<Class<?>, Map<String, PluginConfig>> classMapMap = PluginConfigParser.parsePlugins(objectMap);
        Map<String, ServiceConfig> stringServiceConfigMap =
                ServiceConfigParser.parseServiceMapConfig(Arrays.asList(yamlConfigMap), classMapMap);
        Assertions.assertEquals(1, stringServiceConfigMap.size());
    }

    @Test
    public void testParseServiceConfig() {
        Map<String, Object> yamlConfigMap = YamlParser.parseAsFromClassPath("listener_default.yaml", Map.class);
        YamlUtils yamlUtils = new YamlUtils("Label[]");
        Map<String, Object> objectMap = yamlUtils.getMap(yamlConfigMap, ConfigConstants.PLUGINS);
        Map<Class<?>, Map<String, PluginConfig>> classMapMap = PluginConfigParser.parsePlugins(objectMap);
        ServiceConfig serviceConfig = ServiceConfigParser.parseServiceConfig(yamlConfigMap, classMapMap);
        Assertions.assertFalse(serviceConfig.isSetDefault());
    }
}
