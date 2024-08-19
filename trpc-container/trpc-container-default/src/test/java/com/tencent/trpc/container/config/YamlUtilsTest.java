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

package com.tencent.trpc.container.config;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;


/**
 * YamlUtils test class
 */
public class YamlUtilsTest {

    private Map<String, Object> properties;

    private YamlUtils yamlUtils;

    @Before
    public void setUp() throws Exception {
        this.properties = new HashMap<>();
        properties.put("string", "string");
        properties.put("integer", 10);
        properties.put("boolean", true);
        properties.put("collection", Arrays.asList(1, 2));
        this.yamlUtils = new YamlUtils("");
    }

    @After
    public void tearDown() throws Exception {
        properties.clear();
    }

    @Test
    public void testGetString() {
        String string = yamlUtils.getString(properties, "string");
        Assert.assertEquals("string", string);

        properties.put("string", null);

        try {
            yamlUtils.getString(properties, "string");
        } catch (Exception e) {
            Assert.assertTrue(e instanceof IllegalArgumentException);
        }
    }

    @Test
    public void testGetInteger() {
        int integer = yamlUtils.getInteger(properties, "integer");
        Assert.assertEquals(integer, 10);

        properties.put("integer", null);

        try {
            yamlUtils.getInteger(properties, "integer");
        } catch (Exception e) {
            Assert.assertTrue(e instanceof IllegalArgumentException);
        }
    }

    @Test
    public void testGetBoolean() {
        boolean bool = yamlUtils.getBoolean(properties, "boolean");
        Assert.assertTrue(bool);

        properties.put("boolean", null);
        try {
            yamlUtils.getBoolean(properties, "boolean");
        } catch (Exception e) {
            Assert.assertTrue(e instanceof IllegalArgumentException);
        }
    }

    @Test
    public void testGetCollection() {
        Collection collection = yamlUtils.getCollection(properties, "collection");
        Assert.assertNotNull(collection);
        properties.put("collection", null);
        try {
            yamlUtils.getBoolean(properties, "collection");
        } catch (Exception e) {
            Assert.assertTrue(e instanceof IllegalArgumentException);
        }
    }

    @Test
    public void testGetStringList() {
        List<String> collection = yamlUtils.getStringList(properties, "collection");
        Assert.assertNotNull(collection);
        try {
            yamlUtils.requireMap(Arrays.asList(1, 2), "key");
        } catch (Exception e) {
            Assert.assertTrue(e instanceof IllegalArgumentException);
        }
    }
}
