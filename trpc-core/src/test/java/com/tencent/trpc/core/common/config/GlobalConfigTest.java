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

package com.tencent.trpc.core.common.config;

import java.util.HashMap;
import java.util.Map;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class GlobalConfigTest {

    private GlobalConfig globalConfig;

    @Before
    public void testSetUp() {
        this.globalConfig = new GlobalConfig();
        globalConfig.setEnableSet(true);
        globalConfig.setEnvName("env");
        globalConfig.setFullSetName("div");
        globalConfig.setNamespace("prod");
        globalConfig.setContainerName("container");
    }

    @Test
    public void testSetDefault() {
        globalConfig.setDefault();
        Assert.assertTrue(globalConfig.isSetDefault());
    }

    @Test
    public void testCheckFiledModifyPrivilege() {
        globalConfig.checkFiledModifyPrivilege();
    }

    @Test
    public void getNamespace() {
        Assert.assertEquals(globalConfig.getNamespace(), "prod");
    }

    @Test
    public void setNamespace() {
        globalConfig.setNamespace("dev");
        Assert.assertEquals(globalConfig.getNamespace(), "dev");
    }

    @Test
    public void isSetDefault() {
        Assert.assertFalse(globalConfig.isSetDefault());
    }

    @Test
    public void getEnvName() {
        Assert.assertEquals(globalConfig.getEnvName(), "env");
    }

    @Test
    public void setEnvName() {
        globalConfig.setEnvName("env-dev");
        Assert.assertEquals(globalConfig.getEnvName(), "env-dev");
    }

    @Test
    public void isEnableSet() {
        Assert.assertTrue(globalConfig.isEnableSet());
    }

    @Test
    public void setEnableSet() {
        globalConfig.setEnableSet(false);
        Assert.assertFalse(globalConfig.isEnableSet());
    }

    @Test
    public void getSetDivision() {
        Assert.assertEquals(globalConfig.getFullSetName(), "div");
    }

    @Test
    public void setSetDivision() {
        globalConfig.setFullSetName("set-div");
        Assert.assertEquals(globalConfig.getFullSetName(), "set-div");

    }

    @Test
    public void getContainerName() {
        Assert.assertEquals(globalConfig.getContainerName(), "container");
    }

    @Test
    public void setContainerName() {
        globalConfig.setContainerName("c");
        Assert.assertEquals(globalConfig.getContainerName(), "c");
    }

    @Test
    public void testGetExt() {
        Assert.assertTrue(globalConfig.getExt().isEmpty());
    }

    @Test
    public void testSetExt() {
        Map<String, Object> extMap = new HashMap<>();
        extMap.put("ext", "ext");
        globalConfig.setExt(extMap);
        Assert.assertEquals(globalConfig.getExt().get("ext"), "ext");
    }
}
