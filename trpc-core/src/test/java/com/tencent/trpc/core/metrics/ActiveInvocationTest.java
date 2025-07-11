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
 * ActiveInvocation test
 */
public class ActiveInvocationTest {

    private ActiveInvocation activeInvocation;

    @Before
    public void setUp() throws Exception {
        MetricsCallInfo passiveCallInfo = new MetricsCallInfo("passiveApp", "passiveServer",
                "passiveService", "passiveInterface", "passiveIp",
                "passiveContainer", "passiveConSetId");
        this.activeInvocation = new ActiveInvocation("activeService",
                "activeInterface", passiveCallInfo);
    }

    @Test
    public void testGetActiveService() {
        Assert.assertEquals("activeService", activeInvocation.getActiveService());
    }

    @Test
    public void testGetActiveInterface() {
        Assert.assertEquals("activeInterface", activeInvocation.getActiveMethodName());
    }

    @Test
    public void testGetPassiveApp() {
        Assert.assertEquals("passiveApp", activeInvocation.getPassiveCallInfo().getApp());
    }

    @Test
    public void testGetPassiveServer() {
        Assert.assertEquals("passiveServer", activeInvocation.getPassiveCallInfo().getServer());
    }

    @Test
    public void testGetPassiveService() {
        Assert.assertEquals("passiveService", activeInvocation.getPassiveCallInfo().getService());
    }

    @Test
    public void testGetPassiveInterface() {
        Assert.assertEquals("passiveInterface", activeInvocation.getPassiveCallInfo().getMethodName());
    }

    @Test
    public void testGetPassiveIp() {
        Assert.assertEquals("passiveIp", activeInvocation.getPassiveCallInfo().getIp());
    }

    @Test
    public void testGetPassiveContainer() {
        Assert.assertEquals("passiveContainer", activeInvocation.getPassiveCallInfo().getContainer());
    }

    @Test
    public void testGetPassiveConSetId() {
        Assert.assertEquals("passiveConSetId", activeInvocation.getPassiveCallInfo().getContainerSetId());
    }

    @Test
    public void testTestToString() {
        Assert.assertNotNull(activeInvocation.toString());
    }
}