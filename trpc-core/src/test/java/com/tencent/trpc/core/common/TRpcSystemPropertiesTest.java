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

package com.tencent.trpc.core.common;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class TRpcSystemPropertiesTest {

    @BeforeEach
    public void setUp() {
        TRpcSystemProperties.setProperties("key", "value");
    }

    @Test
    public void testGetProperties() {
        String value = TRpcSystemProperties.getProperties("key");
        Assertions.assertEquals("value", value);
        String value1 = TRpcSystemProperties.getProperties("key1");
        Assertions.assertNull(value1);
    }

    @Test
    public void testGetPropertiesWithDef() {
        String value = TRpcSystemProperties.getProperties("key", "value1");
        Assertions.assertEquals("value", value);
        String value1 = TRpcSystemProperties.getProperties("key1", "value1");
        Assertions.assertEquals("value1", value1);
    }

    @Test
    public void testSetProperties() {
        TRpcSystemProperties.setProperties("key", "value2");
        String value = TRpcSystemProperties.getProperties("key");
        Assertions.assertEquals("value2", value);
    }

    @Test
    public void testIsIgnoreSamePluginName() {
        Assertions.assertFalse(TRpcSystemProperties.isIgnoreSamePluginName());
    }

    @Test
    public void testSetIgnoreSamePluginName() {
        TRpcSystemProperties.setIgnoreSamePluginName(true);
        Assertions.assertTrue(TRpcSystemProperties.isIgnoreSamePluginName());
    }
}
