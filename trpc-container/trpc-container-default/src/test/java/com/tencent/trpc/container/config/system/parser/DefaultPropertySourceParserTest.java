package com.tencent.trpc.container.config.system.parser;

import junit.framework.TestCase;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class DefaultPropertySourceParserTest extends TestCase {

    @Test
    public void testGetFlattableMap() {
        DefaultPropertySourceParser propertySourceParser = new DefaultPropertySourceParser();
        Map<String, Object> source = new HashMap<>();
        source.put("key","value");
        source.put("list","[l1,l2]");
        source.put("key.key2","value22");
        source.put("key2", Arrays.asList(1,2));
        Map<String,String> map= new HashMap();
        map.put("map1","value1");
        source.put("key3", map);
        Map<String, Object> flattableMap = propertySourceParser.getFlattableMap(source);
        Assert.assertNotNull(flattableMap);
    }

    @Test
    public void testParseFlattableMap() {
        DefaultPropertySourceParser propertySourceParser = new DefaultPropertySourceParser();
        Map<String, Object> source = new HashMap<>();
        source.put("key","value");
        source.put("list","[l1,l2]");
        source.put("key.key2","value22");
        source.put("key2", Arrays.asList(1,2));
        Map<String,String> map= new HashMap();
        map.put("map1","value1");
        source.put("key3", map);
        Map<String, Object> stringObjectMap = propertySourceParser.parseFlattableMap(source);
        Assert.assertNotNull(stringObjectMap);
    }
}