package com.tencent.trpc.container.config.yaml;

import com.tencent.trpc.container.config.YamlUtils;
import com.tencent.trpc.core.common.config.PluginConfig;
import com.tencent.trpc.core.common.config.constant.ConfigConstants;
import com.tencent.trpc.core.utils.YamlParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;
import java.util.Map;

public class PluginConfigParserTest {

    @Test
    public void testParsePlugins() {
        PluginConfigParser pluginConfigParser = new PluginConfigParser();
        Assertions.assertNotNull(pluginConfigParser);
        Map<String, Object> yamlConfigMap = YamlParser.parseAsFromClassPath("trpc_java_config.yaml", Map.class);
        YamlUtils yamlUtils = new YamlUtils("Label[]");
        Map<String, Object> objectMap = yamlUtils.getMap(yamlConfigMap, ConfigConstants.PLUGINS);
        Map<Class<?>, Map<String, PluginConfig>> classMapMap = PluginConfigParser.parsePlugins(objectMap);
        Assertions.assertNotNull(classMapMap.getClass());
    }
}
