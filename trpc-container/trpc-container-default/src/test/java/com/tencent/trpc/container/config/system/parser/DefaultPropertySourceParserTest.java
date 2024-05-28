package com.tencent.trpc.container.config.system.parser;

import junit.framework.TestCase;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class DefaultPropertySourceParserTest extends TestCase {

    @Test
    public void testGetFlattableMap() {
        DefaultPropertySourceParser propertySourceParser = new DefaultPropertySourceParser();
        Map<String, Object> source = new HashMap<>();
        source.put("key","valye");
        Map<String, Object> flattableMap = propertySourceParser.getFlattableMap(source);
        Assert.assertNotNull(flattableMap);
    }

    @Test
    public void testParseFlattableMap() {
        DefaultPropertySourceParser propertySourceParser = new DefaultPropertySourceParser();
        Map<String, Object> source = new HashMap<>();
        source.put("key.key2","valye");
        Map<String, Object> stringObjectMap = propertySourceParser.parseFlattableMap(source);
        Assert.assertNotNull(stringObjectMap);
    }
}