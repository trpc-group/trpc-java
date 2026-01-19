package com.tencent.trpc.container.config.system.parser;

import com.tencent.trpc.container.config.system.Configuration;
import com.tencent.trpc.core.utils.YamlParser;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;

public class DefaultPropertySourceParserTest {

    @Test
    public void testGetFlattableMap() {
        Map<String, Object> yamlConfigMap = YamlParser.parseAsFromClassPath("listener_default.yaml", Map.class);
        DefaultPropertySourceParser propertySourceParser = new DefaultPropertySourceParser();
        Map<String, Object> flattableMap = propertySourceParser.getFlattableMap(yamlConfigMap);
        Assertions.assertEquals(147, flattableMap.size());
    }

    @Test
    public void testParseFlattableMap() {
        Map<String, Object> yamlConfigMap = YamlParser.parseAsFromClassPath("listener_default.yaml", Map.class);
        DefaultPropertySourceParser propertySourceParser = new DefaultPropertySourceParser();
        Map<String, Object> stringObjectMap = propertySourceParser.parseFlattableMap(yamlConfigMap);
        Assertions.assertEquals(4, stringObjectMap.size());
        Boolean aBoolean = Configuration.toBooleanObject(true);
        Assertions.assertTrue(aBoolean);
    }
}
