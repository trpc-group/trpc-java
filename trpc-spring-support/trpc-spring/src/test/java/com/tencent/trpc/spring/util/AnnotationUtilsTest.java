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

package com.tencent.trpc.spring.util;

import static org.junit.Assert.assertArrayEquals;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.env.PropertyResolver;

public class AnnotationUtilsTest {

    @Mock
    private PropertyResolver propertyResolver;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testGetAttribute() {
        String key = "attributeName";
        String value = "testValue";
        AnnotationAttributes attributes = AnnotationAttributes.fromMap(Collections.singletonMap(key, value));
        String object = AnnotationUtils.getAttribute(attributes, key);
        Assert.assertEquals(value, object);

        object = AnnotationUtils.getAttribute(attributes, "other");
        Assert.assertNull(object);
    }

    @Test
    public void testResolvePlaceholders() {
        String eleValueKey = "value";
        String eleValueValue = "${placeholderVal}";
        String eleNumberKey = "number";
        int eleNumberValue = 1024;
        String result = "resolved_value";

        Map<String, Object> attributes = new LinkedHashMap<>();
        attributes.put(eleValueKey, eleValueValue);
        attributes.put(eleNumberKey, eleNumberValue);

        when(propertyResolver.resolvePlaceholders(eleValueValue)).thenReturn(result);
        Map<String, Object> resolvedAttributes = AnnotationUtils.resolvePlaceholders(attributes, propertyResolver);

        Assert.assertEquals(result, resolvedAttributes.get(eleValueKey));
        Assert.assertEquals(Integer.valueOf(eleNumberValue), resolvedAttributes.get(eleNumberKey));
    }

    // 测试resolvePlaceholders处理字符串数组的情况
    @Test
    public void testResolvePlaceholdersWithArray() {
        Map<String, Object> attributes = new LinkedHashMap<>();
        String[] values = new String[]{"${placeholder1}", "${placeholder2}"};
        attributes.put("values", values);

        // 假设propertyResolver.resolvePlaceholders分别解析两个占位符
        when(propertyResolver.resolvePlaceholders("${placeholder1}")).thenReturn("value1");
        when(propertyResolver.resolvePlaceholders("${placeholder2}")).thenReturn("value2");

        Map<String, Object> resolvedAttributes = AnnotationUtils.resolvePlaceholders(attributes, propertyResolver);

        assertArrayEquals(new String[]{"value1", "value2"}, (String[]) resolvedAttributes.get("values"));
    }

    // 测试resolvePlaceholders忽略特定属性的情况
    @Test
    public void testResolvePlaceholdersIgnoreAttributes() {
        Map<String, Object> attributes = new LinkedHashMap<>();
        attributes.put("value", "${placeholder}");
        attributes.put("ignore", "${ignore}");

        // 假设只有value属性需要解析
        when(propertyResolver.resolvePlaceholders("${placeholder}")).thenReturn("val");
        Map<String, Object> resolvedAttributes = AnnotationUtils.resolvePlaceholders(attributes, propertyResolver,
                "ignore");

        Assert.assertNull(resolvedAttributes.get("ignore")); // 忽略未解析的属性
        Assert.assertEquals("val", resolvedAttributes.get("value")); // 忽略未解析的属性
    }

    @Test
    public void testAttribute() {
        Map<String, Object> objectMap = AnnotationUtils.getAttributes(null, propertyResolver, true, "");
        Assert.assertEquals(0, objectMap.size());
    }
}
