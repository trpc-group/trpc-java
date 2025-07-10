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

package com.tencent.trpc.core.rpc;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.tencent.trpc.core.common.config.ConsumerConfig;
import com.tencent.trpc.core.common.config.ProtocolConfig;
import org.junit.Before;
import org.junit.Test;

public class AbstractRpcClientTest {

    private AbstractRpcClient abstractRpcClient;

    @Before
    public void setUp() throws Exception {
        abstractRpcClient = new AbstractRpcClient() {
            @Override
            protected void doOpen() throws Exception {

            }

            @Override
            protected void doClose() {

            }

            @Override
            public <T> ConsumerInvoker<T> createInvoker(ConsumerConfig<T> consumerConfig) {
                return null;
            }
        };

        abstractRpcClient.setConfig(new ProtocolConfig());

    }

    @Test
    public void testOpen() {
        abstractRpcClient.open();
    }

    @Test
    public void testIsClosed() {
        assertFalse(abstractRpcClient.isClosed());

    }

    @Test
    public void testIsAvailable() {
        assertFalse(abstractRpcClient.isAvailable());
    }

    @Test
    public void testClose() {
        abstractRpcClient.close();
        assertTrue(abstractRpcClient.isClosed());
    }

    @Test
    public void testCloseFuture() {
        assertNotNull(abstractRpcClient.closeFuture());
    }

    @Test
    public void testGetProtocolConfig() {
        assertNotNull(abstractRpcClient.getProtocolConfig());
    }

    @Test
    public void testTestToString() {
        assertNotNull(abstractRpcClient.toString());
    }
}