/*
 * Tencent is pleased to support the open source community by making tRPC available.
 *
 * Copyright (C) 2023 Tencent.
 * All rights reserved.
 *
 * If you have downloaded a copy of the tRPC source code from Tencent,
 * please note that tRPC source code is licensed under the Apache 2.0 License,
 * A copy of the Apache 2.0 License can be found in the LICENSE file.
 */

package com.tencent.trpc.core.common;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class TRpcSystemPropertiesTest {

    @Before
    public void setUp() {
        TRpcSystemProperties.setProperties("key", "value");
    }

    @Test
    public void testGetProperties() {
        String value = TRpcSystemProperties.getProperties("key");
        Assert.assertEquals("value", value);
        String value1 = TRpcSystemProperties.getProperties("key1");
        Assert.assertNull(value1);
    }

    @Test
    public void testGetPropertiesWithDef() {
        String value = TRpcSystemProperties.getProperties("key", "value1");
        Assert.assertEquals("value", value);
        String value1 = TRpcSystemProperties.getProperties("key1", "value1");
        Assert.assertEquals("value1", value1);
    }

    @Test
    public void testSetProperties() {
        TRpcSystemProperties.setProperties("key", "value2");
        String value = TRpcSystemProperties.getProperties("key");
        Assert.assertEquals("value2", value);
    }

    @Test
    public void testIsIgnoreSamePluginName() {
        Assert.assertFalse(TRpcSystemProperties.isIgnoreSamePluginName());
    }

    @Test
    public void testSetIgnoreSamePluginName() {
        TRpcSystemProperties.setIgnoreSamePluginName(true);
        Assert.assertTrue(TRpcSystemProperties.isIgnoreSamePluginName());
    }
}