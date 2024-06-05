package com.tencent.trpc.container.config.system.parser;

import com.tencent.trpc.container.config.system.Configuration;
import com.tencent.trpc.core.utils.YamlParser;
import junit.framework.TestCase;
import org.junit.Assert;
import org.junit.Test;
import java.util.Map;

public class DefaultPropertySourceParserTest extends TestCase {

    @Test
    public void testGetFlattableMap() {
        Map<String, Object> yamlConfigMap = YamlParser.parseAsFromClassPath("listener_default.yaml", Map.class);
        DefaultPropertySourceParser propertySourceParser = new DefaultPropertySourceParser();
        Map<String, Object> flattableMap = propertySourceParser.getFlattableMap(yamlConfigMap);
        Assert.assertEquals(146, flattableMap.size());
    }

    @Test
    public void testParseFlattableMap() {
        Map<String, Object> yamlConfigMap = YamlParser.parseAsFromClassPath("listener_default.yaml", Map.class);
        DefaultPropertySourceParser propertySourceParser = new DefaultPropertySourceParser();
        Map<String, Object> stringObjectMap = propertySourceParser.parseFlattableMap(yamlConfigMap);
        Assert.assertEquals(4, stringObjectMap.size());
        Boolean aBoolean = Configuration.toBooleanObject(true);
        Assert.assertTrue(aBoolean);
    }
}