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

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.tencent.trpc.core.common.config.ProtocolConfig;
import com.tencent.trpc.core.extension.ExtensionLoader;
import com.tencent.trpc.core.rpc.spi.RpcServerFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

@ExtendWith(MockitoExtension.class)
public class RpcServerMapTest {

    @Test
    public void test() {
        try (MockedStatic<ExtensionLoader> mockedStatic = Mockito.mockStatic(ExtensionLoader.class)) {
            ProtocolConfig config = new ProtocolConfig();
            config.setProtocol("trpc");
            ExtensionLoader<RpcServerFactory> extensionLoader = Mockito.mock(ExtensionLoader.class);
            mockedStatic.when(() -> ExtensionLoader.getExtensionLoader(RpcServerFactory.class))
                    .thenReturn(extensionLoader);
            RpcServerFactory rpcServerFactory = Mockito.mock(RpcServerFactory.class);
            Mockito.when(extensionLoader.getExtension("trpc")).thenReturn(rpcServerFactory);
            RpcServer rpcServer = Mockito.mock(RpcServer.class);
            Mockito.when(rpcServer.getProtocolConfig()).thenReturn(new ProtocolConfig());
            Mockito.when(rpcServerFactory.createRpcServer(config)).thenReturn(rpcServer);
            assertEquals(RpcServerManager.getOrCreateRpcServer(config), rpcServer);
            RpcServerManager.remove(config);
            RpcServerManager.shutdown();
            RpcServerManager.reset();
            assertEquals(RpcServerManager.getOrCreateRpcServer(config), rpcServer);
            RpcServerManager.shutdown();
        }
    }
}
