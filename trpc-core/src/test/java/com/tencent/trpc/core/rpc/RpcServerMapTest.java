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

import static org.junit.Assert.assertEquals;

import com.tencent.trpc.core.common.config.ProtocolConfig;
import com.tencent.trpc.core.extension.ExtensionLoader;
import com.tencent.trpc.core.rpc.spi.RpcServerFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ExtensionLoader.class})
public class RpcServerMapTest {

    @Before
    public void init() {
        PowerMockito.mockStatic(ExtensionLoader.class);// 3
    }

    @Test
    public void test() {
        ProtocolConfig config = new ProtocolConfig();
        config.setProtocol("trpc");
        ExtensionLoader<RpcServerFactory> extensionLoader = PowerMockito
                .mock(ExtensionLoader.class);
        PowerMockito.when(ExtensionLoader.getExtensionLoader(RpcServerFactory.class))
                .thenReturn(extensionLoader);
        RpcServerFactory rpcServerFactory = PowerMockito.mock(RpcServerFactory.class);
        PowerMockito.when(extensionLoader.getExtension("trpc")).thenReturn(rpcServerFactory);
        RpcServer rpcServer = PowerMockito.mock(RpcServer.class);
        PowerMockito.when(rpcServer.getProtocolConfig()).thenReturn(new ProtocolConfig());
        PowerMockito.when(rpcServerFactory.createRpcServer(config)).thenReturn(rpcServer);
        assertEquals(RpcServerManager.getOrCreateRpcServer(config), rpcServer);
        RpcServerManager.remove(config);
        RpcServerManager.shutdown();
        RpcServerManager.reset();
        assertEquals(RpcServerManager.getOrCreateRpcServer(config), rpcServer);
        RpcServerManager.shutdown();
    }
}
