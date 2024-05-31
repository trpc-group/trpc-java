package com.tencent.trpc.container.config.system.parser;

import com.tencent.trpc.core.utils.YamlParser;
import junit.framework.TestCase;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class DefaultPropertySourceParserTest extends TestCase {

    @Test
    public void testGetFlattableMap() {

        Map<String, Object> yamlConfigMap = YamlParser.parseAsFromClassPath("listener_default.yaml", Map.class);
        DefaultPropertySourceParser propertySourceParser = new DefaultPropertySourceParser();
        Map<String, Object> flattableMap = propertySourceParser.getFlattableMap(yamlConfigMap);
        Assert.assertNotNull(flattableMap);
    }

    @Test
    public void testParseFlattableMap() {
        Map<String, Object> yamlConfigMap = YamlParser.parseAsFromClassPath("listener_default.yaml", Map.class);
        DefaultPropertySourceParser propertySourceParser = new DefaultPropertySourceParser();
        Map<String, Object> stringObjectMap = propertySourceParser.parseFlattableMap(yamlConfigMap);
        Assert.assertNotNull(stringObjectMap);
    }
}