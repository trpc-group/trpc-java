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

package com.tencent.trpc.core.metrics;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * PassiveInvocation test
 */
public class PassiveInvocationTest {

    private PassiveInvocation passiveInvocation;

    @Before
    public void setUp() throws Exception {
        MetricsCallInfo activeCallInfo = new MetricsCallInfo("ActiveApp", "ActiveServer",
                "ActiveService", "ActiveInterface", "ActiveIp",
                "ActiveContainer", "ActiveConSetId");
        this.passiveInvocation = new PassiveInvocation(
                "PassiveService", "PassiveInterface", activeCallInfo);
    }

    @Test
    public void testGetActiveApp() {
        Assert.assertEquals("ActiveApp", passiveInvocation.getActiveCallInfo().getApp());
    }

    @Test
    public void testGetActiveServer() {
        Assert.assertEquals("ActiveServer", passiveInvocation.getActiveCallInfo().getServer());
    }

    @Test
    public void testGetActiveService() {
        Assert.assertEquals("ActiveService", passiveInvocation.getActiveCallInfo().getService());
    }

    @Test
    public void testGetActiveInterface() {
        Assert.assertEquals("ActiveInterface", passiveInvocation.getActiveCallInfo().getMethodName());
    }

    @Test
    public void testGetActiveIp() {
        Assert.assertEquals("ActiveIp", passiveInvocation.getActiveCallInfo().getIp());
    }

    @Test
    public void testGetActiveContainer() {
        Assert.assertEquals("ActiveContainer", passiveInvocation.getActiveCallInfo().getContainer());
    }

    @Test
    public void testGetActiveConSetId() {
        Assert.assertEquals("ActiveConSetId", passiveInvocation.getActiveCallInfo().getContainerSetId());
    }

    @Test
    public void testGetPassiveService() {
        Assert.assertEquals("PassiveService", passiveInvocation.getPassiveService());
    }

    @Test
    public void testGetPassiveInterface() {
        Assert.assertEquals("PassiveInterface", passiveInvocation.getPassiveMethodName());
    }

    @Test
    public void testTestToString() {
        Assert.assertNotNull(passiveInvocation.toString());
    }
}