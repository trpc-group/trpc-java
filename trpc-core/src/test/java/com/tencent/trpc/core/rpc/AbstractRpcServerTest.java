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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.tencent.trpc.core.common.config.ProtocolConfig;
import com.tencent.trpc.core.common.config.ProviderConfig;
import com.tencent.trpc.core.rpc.def.DefProviderInvoker;
import java.util.concurrent.CompletionStage;
import org.junit.Before;
import org.junit.Test;

public class AbstractRpcServerTest {

    private AbstractRpcServer abstractRpcServer;

    private ProtocolConfig protocolConfig;

    @Before
    public void setUp() {
        abstractRpcServer = new AbstractRpcServer() {
            @Override
            protected <T> void doExport(ProviderInvoker<T> invoker) {

            }

            @Override
            protected <T> void doUnExport(ProviderConfig<T> config) {

            }

            @Override
            protected void doOpen() throws Exception {

            }

            @Override
            protected void doClose() {

            }
        };
        protocolConfig = new ProtocolConfig();
        protocolConfig.setIp("127.0.0.1");
        protocolConfig.setPort(12345);
        abstractRpcServer.setConfig(protocolConfig);
    }

    @Test
    public void testExport() {
        ProviderConfig<GenericClient> objectProviderConfig = new ProviderConfig<>();
        objectProviderConfig.setServiceInterface(GenericClient.class);
        objectProviderConfig.setRef(new GenericClient() {
            @Override
            public CompletionStage<byte[]> asyncInvoke(RpcClientContext context, byte[] body) {
                return null;
            }

            @Override
            public byte[] invoke(RpcClientContext context, byte[] body) {
                return new byte[0];
            }
        });
        abstractRpcServer.export(new DefProviderInvoker<>(protocolConfig, objectProviderConfig));
    }

    @Test
    public void testUnexport() {
        abstractRpcServer.unexport(new ProviderConfig<>());
    }

    @Test
    public void testOpen() {
        abstractRpcServer.open();
    }

    @Test
    public void testIsClosed() {
        assertFalse(abstractRpcServer.isClosed());
    }

    @Test
    public void testClose() {
        abstractRpcServer.close();
        assertTrue(abstractRpcServer.isClosed());
    }

    @Test
    public void testCloseFuture() {
        assertNotNull(abstractRpcServer.closeFuture());
    }

    @Test
    public void testGetProtocolConfig() {
        assertNotNull(abstractRpcServer.getProtocolConfig());
    }
}