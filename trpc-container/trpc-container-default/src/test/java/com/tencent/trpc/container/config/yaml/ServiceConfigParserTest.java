package com.tencent.trpc.container.config.yaml;

import com.tencent.trpc.container.config.YamlUtils;
import com.tencent.trpc.core.common.config.PluginConfig;
import com.tencent.trpc.core.common.config.ServiceConfig;
import com.tencent.trpc.core.common.config.constant.ConfigConstants;
import com.tencent.trpc.core.utils.YamlParser;
import junit.framework.TestCase;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class ServiceConfigParserTest extends TestCase {

    @Test
    public void testParseServiceMapConfig() {
        ServiceConfigParser serviceConfigParser = new ServiceConfigParser();
        Assert.assertNotNull(serviceConfigParser);
        Map<String, Object> yamlConfigMap = YamlParser.parseAsFromClassPath("listener_default.yaml", Map.class);
        YamlUtils yamlUtils = new YamlUtils("Label[]");
        Map<String, Object> objectMap = yamlUtils.getMap(yamlConfigMap, ConfigConstants.PLUGINS);
        Map<Class<?>, Map<String, PluginConfig>> classMapMap = PluginConfigParser.parsePlugins(objectMap);
        Map<String, ServiceConfig> stringServiceConfigMap =
                ServiceConfigParser.parseServiceMapConfig(Arrays.asList(yamlConfigMap), classMapMap);
        Assert.assertNotNull(stringServiceConfigMap);
    }

    @Test
    public void testParseServiceConfig() {
        Map<String, Object> yamlConfigMap = YamlParser.parseAsFromClassPath("listener_default.yaml", Map.class);
        YamlUtils yamlUtils = new YamlUtils("Label[]");
        Map<String, Object> objectMap = yamlUtils.getMap(yamlConfigMap, ConfigConstants.PLUGINS);
        Map<Class<?>, Map<String, PluginConfig>> classMapMap = PluginConfigParser.parsePlugins(objectMap);
        ServiceConfig serviceConfig = ServiceConfigParser.parseServiceConfig(yamlConfigMap, classMapMap);
        Assert.assertNotNull(serviceConfig);
    }
}