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

package com.tencent.trpc.core.rpc.def;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

public class LeftTimeoutTest {

    private LeftTimeout leftTimeout;

    @Before
    public void setUp() throws Exception {
        leftTimeout = new LeftTimeout(2000, 100);
    }

    @Test
    public void testGetOriginTimeout() {
        assertEquals(2000, leftTimeout.getOriginTimeout());
    }

    @Test
    public void testSetOriginTimeout() {
        leftTimeout.setOriginTimeout(1000);
        assertEquals(1000, leftTimeout.getOriginTimeout());

    }

    @Test
    public void testGetLeftTimeout() {
        assertEquals(100, leftTimeout.getLeftTimeout());
    }

    @Test
    public void testSetLeftTimeout() {
        leftTimeout.setLeftTimeout(200);
        assertEquals(200, leftTimeout.getLeftTimeout());

    }
}