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

package com.tencent.trpc.core.common.config;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class GlobalConfigTest {

    private GlobalConfig globalConfig;

    @BeforeEach
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
        Assertions.assertTrue(globalConfig.isSetDefault());
    }

    @Test
    public void testCheckFiledModifyPrivilege() {
        globalConfig.checkFiledModifyPrivilege();
    }

    @Test
    public void getNamespace() {
        Assertions.assertEquals(globalConfig.getNamespace(), "prod");
    }

    @Test
    public void setNamespace() {
        globalConfig.setNamespace("dev");
        Assertions.assertEquals(globalConfig.getNamespace(), "dev");
    }

    @Test
    public void isSetDefault() {
        Assertions.assertFalse(globalConfig.isSetDefault());
    }

    @Test
    public void getEnvName() {
        Assertions.assertEquals(globalConfig.getEnvName(), "env");
    }

    @Test
    public void setEnvName() {
        globalConfig.setEnvName("env-dev");
        Assertions.assertEquals(globalConfig.getEnvName(), "env-dev");
    }

    @Test
    public void isEnableSet() {
        Assertions.assertTrue(globalConfig.isEnableSet());
    }

    @Test
    public void setEnableSet() {
        globalConfig.setEnableSet(false);
        Assertions.assertFalse(globalConfig.isEnableSet());
    }

    @Test
    public void getSetDivision() {
        Assertions.assertEquals(globalConfig.getFullSetName(), "div");
    }

    @Test
    public void setSetDivision() {
        globalConfig.setFullSetName("set-div");
        Assertions.assertEquals(globalConfig.getFullSetName(), "set-div");

    }

    @Test
    public void getContainerName() {
        Assertions.assertEquals(globalConfig.getContainerName(), "container");
    }

    @Test
    public void setContainerName() {
        globalConfig.setContainerName("c");
        Assertions.assertEquals(globalConfig.getContainerName(), "c");
    }

    @Test
    public void testGetExt() {
        Assertions.assertTrue(globalConfig.getExt().isEmpty());
    }

    @Test
    public void testSetExt() {
        Map<String, Object> extMap = new HashMap<>();
        extMap.put("ext", "ext");
        globalConfig.setExt(extMap);
        Assertions.assertEquals(globalConfig.getExt().get("ext"), "ext");
    }
}
