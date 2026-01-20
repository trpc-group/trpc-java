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
 * PassiveInvocation test
 */
public class PassiveInvocationTest {

    private PassiveInvocation passiveInvocation;

    @BeforeEach
    public void setUp() throws Exception {
        MetricsCallInfo activeCallInfo = new MetricsCallInfo("ActiveApp", "ActiveServer",
                "ActiveService", "ActiveInterface", "ActiveIp",
                "ActiveContainer", "ActiveConSetId");
        this.passiveInvocation = new PassiveInvocation(
                "PassiveService", "PassiveInterface", activeCallInfo);
    }

    @Test
    public void testGetActiveApp() {
        Assertions.assertEquals("ActiveApp", passiveInvocation.getActiveCallInfo().getApp());
    }

    @Test
    public void testGetActiveServer() {
        Assertions.assertEquals("ActiveServer", passiveInvocation.getActiveCallInfo().getServer());
    }

    @Test
    public void testGetActiveService() {
        Assertions.assertEquals("ActiveService", passiveInvocation.getActiveCallInfo().getService());
    }

    @Test
    public void testGetActiveInterface() {
        Assertions.assertEquals("ActiveInterface", passiveInvocation.getActiveCallInfo().getMethodName());
    }

    @Test
    public void testGetActiveIp() {
        Assertions.assertEquals("ActiveIp", passiveInvocation.getActiveCallInfo().getIp());
    }

    @Test
    public void testGetActiveContainer() {
        Assertions.assertEquals("ActiveContainer", passiveInvocation.getActiveCallInfo().getContainer());
    }

    @Test
    public void testGetActiveConSetId() {
        Assertions.assertEquals("ActiveConSetId", passiveInvocation.getActiveCallInfo().getContainerSetId());
    }

    @Test
    public void testGetPassiveService() {
        Assertions.assertEquals("PassiveService", passiveInvocation.getPassiveService());
    }

    @Test
    public void testGetPassiveInterface() {
        Assertions.assertEquals("PassiveInterface", passiveInvocation.getPassiveMethodName());
    }

    @Test
    public void testTestToString() {
        Assertions.assertNotNull(passiveInvocation.toString());
    }
}
