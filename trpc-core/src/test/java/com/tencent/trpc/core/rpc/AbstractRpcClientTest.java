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

package com.tencent.trpc.core.rpc;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tencent.trpc.core.common.config.ConsumerConfig;
import com.tencent.trpc.core.common.config.ProtocolConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class AbstractRpcClientTest {

    private AbstractRpcClient abstractRpcClient;

    @BeforeEach
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
