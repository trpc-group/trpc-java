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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

public class LinkInvokeTimeoutTest {

    private LinkInvokeTimeout linkInvokeTimeout;

    @Before
    public void setUp() throws Exception {
        linkInvokeTimeout = LinkInvokeTimeout.builder()
                .startTime(0)
                .timeout(2000)
                .leftTimeout(1000)
                .serviceEnableLinkTimeout(true)
                .build();
    }

    @Test
    public void testGetTimeout() {
        assertEquals(2000, linkInvokeTimeout.getTimeout());
    }

    @Test
    public void testGetStartTime() {
        assertEquals(0, linkInvokeTimeout.getStartTime());
    }

    @Test
    public void testGetLeftTimeout() {
        assertEquals(1000, linkInvokeTimeout.getLeftTimeout());
    }

    @Test
    public void testIsServiceEnableLinkTimeout() {
        assertTrue(linkInvokeTimeout.isServiceEnableLinkTimeout());
    }

    @Test
    public void testTestToString() {
        assertNotNull(linkInvokeTimeout.toString());
    }
}