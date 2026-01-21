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

package com.tencent.trpc.core.rpc.def;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class LinkInvokeTimeoutTest {

    private LinkInvokeTimeout linkInvokeTimeout;

    @BeforeEach
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
