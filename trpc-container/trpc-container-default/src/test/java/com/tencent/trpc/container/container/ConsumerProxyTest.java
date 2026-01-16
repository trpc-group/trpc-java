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

package com.tencent.trpc.container.container;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Assertions;
import com.google.protobuf.ByteString;
import com.tencent.trpc.container.demo.HelloRequestProtocol.HelloRequest;
import com.tencent.trpc.container.demo.HelloRequestProtocol.HelloResponse;
import com.tencent.trpc.core.common.ConfigManager;
import com.tencent.trpc.core.common.config.BackendConfig;
import com.tencent.trpc.core.container.spi.Container;
import com.tencent.trpc.core.exception.TRpcException;
import com.tencent.trpc.core.rpc.RpcClientContext;
import com.tencent.trpc.core.rpc.TRpcProxy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ConsumerProxyTest {

    Container container;

    @BeforeEach
    public void before() throws Exception {
        ConfigManager.startTest();
        container = new DefaultContainer();
        ConfigManager.startTest();
        container.start();
    }

    @AfterEach
    public void after() {
        if (container != null) {
            container.stop();
        }
        ConfigManager.stopTest();
    }

    @Test
    public void getProxy() {
        String servcieId = "trpc.TestApp.TestServer.Greeter";
        com.tencent.trpc.container.demo.GreeterClient service = TRpcProxy.getProxy(servcieId);
        HelloResponse sayHello = service.sayHello(new RpcClientContext(),
                HelloRequest.newBuilder().setMessage(ByteString.copyFrom("abc".getBytes()))
                        .build());
        assertEquals(sayHello.getMessage().toStringUtf8(), "abc-");
        ConfigManager instance = ConfigManager.getInstance();
        BackendConfig backendConfig = instance.getClientConfig().getBackendConfigMap()
                .get(servcieId);
        System.out.println("namingOptions:>>>>>>>>>>>>>" + backendConfig.getNamingOptions());
    }

    @Test
    public void notConnect() {
        Assertions.assertThrows(TRpcException.class, () -> {
            String servcieId = "trpc.TestApp.TestServer.Notconnect";
            com.tencent.trpc.container.demo.GreeterClient service = TRpcProxy.getProxy(servcieId);
            service.sayHello(new RpcClientContext(), HelloRequest.newBuilder()
                    .setMessage(ByteString.copyFrom("abc".getBytes()))
                    .build());
        });
    }

}
