package com.tencent.trpc.container.config.yaml;

import com.tencent.trpc.container.config.YamlUtils;
import com.tencent.trpc.core.common.config.GlobalConfig;
import com.tencent.trpc.core.common.config.PluginConfig;
import com.tencent.trpc.core.common.config.constant.ConfigConstants;
import com.tencent.trpc.core.utils.YamlParser;
import junit.framework.TestCase;
import org.junit.Assert;
import org.junit.Test;

import java.util.Map;

public class PluginConfigParserTest extends TestCase {

    @Test
    public void testParsePlugins() {
        PluginConfigParser pluginConfigParser = new PluginConfigParser();
        Assert.assertNotNull(pluginConfigParser);
        Map<String, Object> yamlConfigMap = YamlParser.parseAsFromClassPath("trpc_java_config.yaml", Map.class);
        YamlUtils yamlUtils = new YamlUtils("Label[]");
        Map<String, Object> objectMap = yamlUtils.getMap(yamlConfigMap, ConfigConstants.PLUGINS);
        Map<Class<?>, Map<String, PluginConfig>> classMapMap = PluginConfigParser.parsePlugins(objectMap);
        Assert.assertNotNull(classMapMap.getClass());
    }
}