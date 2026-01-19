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

import com.google.protobuf.ByteString;
import com.tencent.trpc.container.demo.HelloRequestProtocol;
import com.tencent.trpc.core.container.spi.Container;
import com.tencent.trpc.core.extension.ExtensionLoader;
import com.tencent.trpc.core.rpc.RpcClientContext;
import com.tencent.trpc.core.rpc.TRpcProxy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class DefaultContainerTest {

    Container container;

    @BeforeEach
    public void start() {
        container =
                ExtensionLoader.getExtensionLoader(Container.class).getExtension("default");
        container.start();
    }

    @Test
    public void testStart() {
        String servcieId = "trpc.TestApp.TestServer.Greeter";
        com.tencent.trpc.container.demo.GreeterClient service = TRpcProxy.getProxy(servcieId);
        HelloRequestProtocol.HelloResponse sayHello = service.sayHello(new RpcClientContext(),
                HelloRequestProtocol.HelloRequest.newBuilder()
                        .setMessage(ByteString.copyFrom("abc".getBytes()))
                        .build());
        assertEquals(sayHello.getMessage().toStringUtf8(), "abc-");
    }

    @AfterEach
    public void testStop() throws InterruptedException {
        Thread.sleep(1000);
        container.stop();
    }

}
