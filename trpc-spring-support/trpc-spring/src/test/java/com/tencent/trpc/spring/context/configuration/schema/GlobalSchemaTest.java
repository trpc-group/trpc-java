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

package com.tencent.trpc.spring.context.configuration.schema;

import java.util.HashMap;
import java.util.Map;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class GlobalSchemaTest {

    private GlobalSchema schema;

    @Before
    public void stUp() {
        schema = new GlobalSchema();
    }

    @Test
    public void testSetEnableSet() {
        schema.setEnableSet(YesOrNo.Y);
        YesOrNo enableSet = schema.getEnableSet();
        Assert.assertEquals(YesOrNo.Y, enableSet);
    }

    @Test
    public void testSetFullSetName() {
        String fullSetName = "myFullName";
        schema.setFullSetName(fullSetName);
        String name = schema.getFullSetName();
        Assert.assertEquals(fullSetName, name);
    }

    @Test
    public void tesSetExt() {
        Map<String, Object> extMap = new HashMap<>();
        extMap.put("k1", new Object());
        schema.setExt(extMap);
        Map<String, Object> schemaExt = schema.getExt();
        Assert.assertEquals(extMap.size(), schemaExt.size());
    }

    @Test
    public void testToString() {
        schema.setFullSetName("fullName");
        String schemaString = schema.toString();
        Assert.assertTrue(schemaString.contains("fullSetName='fullName'"));
    }
}
