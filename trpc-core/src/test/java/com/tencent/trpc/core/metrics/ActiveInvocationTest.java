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

package com.tencent.trpc.core.metrics;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * ActiveInvocation test
 */
public class ActiveInvocationTest {

    private ActiveInvocation activeInvocation;

    @BeforeEach
    public void setUp() throws Exception {
        MetricsCallInfo passiveCallInfo = new MetricsCallInfo("passiveApp", "passiveServer",
                "passiveService", "passiveInterface", "passiveIp",
                "passiveContainer", "passiveConSetId");
        this.activeInvocation = new ActiveInvocation("activeService",
                "activeInterface", passiveCallInfo);
    }

    @Test
    public void testGetActiveService() {
        Assertions.assertEquals("activeService", activeInvocation.getActiveService());
    }

    @Test
    public void testGetActiveInterface() {
        Assertions.assertEquals("activeInterface", activeInvocation.getActiveMethodName());
    }

    @Test
    public void testGetPassiveApp() {
        Assertions.assertEquals("passiveApp", activeInvocation.getPassiveCallInfo().getApp());
    }

    @Test
    public void testGetPassiveServer() {
        Assertions.assertEquals("passiveServer", activeInvocation.getPassiveCallInfo().getServer());
    }

    @Test
    public void testGetPassiveService() {
        Assertions.assertEquals("passiveService", activeInvocation.getPassiveCallInfo().getService());
    }

    @Test
    public void testGetPassiveInterface() {
        Assertions.assertEquals("passiveInterface", activeInvocation.getPassiveCallInfo().getMethodName());
    }

    @Test
    public void testGetPassiveIp() {
        Assertions.assertEquals("passiveIp", activeInvocation.getPassiveCallInfo().getIp());
    }

    @Test
    public void testGetPassiveContainer() {
        Assertions.assertEquals("passiveContainer", activeInvocation.getPassiveCallInfo().getContainer());
    }

    @Test
    public void testGetPassiveConSetId() {
        Assertions.assertEquals("passiveConSetId", activeInvocation.getPassiveCallInfo().getContainerSetId());
    }

    @Test
    public void testTestToString() {
        Assertions.assertNotNull(activeInvocation.toString());
    }
}
