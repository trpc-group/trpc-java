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

package com.tencent.trpc.proto.standard.common;

import static org.junit.Assert.assertEquals;

import com.google.protobuf.ByteString;
import com.tencent.trpc.core.common.ConfigManager;
import com.tencent.trpc.core.common.config.BackendConfig;
import com.tencent.trpc.core.common.config.ConsumerConfig;
import com.tencent.trpc.core.common.config.ProtocolConfig;
import com.tencent.trpc.core.exception.TRpcException;
import com.tencent.trpc.core.extension.Extension;
import com.tencent.trpc.core.extension.ExtensionLoader;
import com.tencent.trpc.core.rpc.RpcClient;
import com.tencent.trpc.core.rpc.RpcClientContext;
import com.tencent.trpc.core.rpc.RpcServerManager;
import com.tencent.trpc.core.rpc.spi.RpcClientFactory;
import com.tencent.trpc.core.utils.NetUtils;
import com.tencent.trpc.proto.standard.common.HelloRequestProtocol.HelloRequest;
import com.tencent.trpc.proto.standard.common.HelloRequestProtocol.HelloResponse;
import com.tencent.trpc.proto.support.DefRpcClient;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TRpcMockServerTest {

    ConsumerConfig<GreeterClientApi> clientConfig;
    BackendConfig backendConfig;
    RpcClient createRpcClient;

    @Before
    public void before() {
        ConfigManager.stopTest();
        ConfigManager.getInstance().startTest();
        ExtensionLoader.getExtensionLoader(RpcClientFactory.class).addExtension("trpc_mock",
                StandardRpcClientFactory.class);
        String host = NetUtils.LOCAL_HOST;
        int port = NetUtils.getAvailablePort();
        ProtocolConfig protoConfig = ProtocolConfig.newInstance();
        protoConfig.setIp(host);
        protoConfig.setPort(port);
        protoConfig.setProtocol("trpc_mock");
        protoConfig.isSetDefault();
        protoConfig.setDefault();
        ProtocolConfig copy = protoConfig.clone();
        copy.setPort(NetUtils.getAvailablePort());
        createRpcClient = new StandardRpcClientFactory().createRpcClient(copy);
        createRpcClient.open();
        clientConfig = new ConsumerConfig<>();
        backendConfig = new BackendConfig();
        clientConfig.setBackendConfig(backendConfig);
        clientConfig.getBackendConfig().setNamingUrl("ip://" + host + ":" + port);
        clientConfig.setServiceInterface(GreeterClientApi.class);
        clientConfig.setMock(true);
        clientConfig.setMockClass(GreeterServiceMockImp.class.getName());
    }

    @After
    public void after() {
        ConfigManager.stopTest();
        if (backendConfig != null) {
            backendConfig.stop();
        }
        RpcServerManager.shutdown();
        createRpcClient.close();
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void oneWayTest() {
        RpcClientContext context = new RpcClientContext();
        context.getReqAttachMap().put("key1", "abc");
        context.setTimeoutMills(100000);
        context.setOneWay(true);

        HelloRequest request =
                HelloRequest.newBuilder().setMessage(ByteString.copyFrom("normal1".getBytes()))
                        .build();
        GreeterClientApi proxy = clientConfig.getProxy();
        HelloResponse sayHello = proxy.sayHello(context, request);
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        assertEquals(sayHello, null);

    }

    @Test
    public void serverNormalTest() {
        GreeterClientApi proxy = clientConfig.getProxy();
        RpcClientContext context = new RpcClientContext();
        context.getReqAttachMap().put("key1", "abc");
        context.setTimeoutMills(1000);
        HelloRequest request =
                HelloRequest.newBuilder().setMessage(ByteString.copyFrom("normalx1".getBytes()))
                        .build();
        HelloResponse sayHello = proxy.sayHello(context, request);
        assertEquals(sayHello.getMessage().toStringUtf8(), "normalx1");
        context.setTimeoutMills(1000);
        HelloRequest request2 =
                HelloRequest.newBuilder().setMessage(ByteString.copyFrom("normalx2".getBytes()))
                        .build();

        HelloResponse sayHello2 = proxy.asyncSayHello(context, request2).toCompletableFuture()
                .join();
        assertEquals(sayHello2.getMessage().toStringUtf8(), "normalx2");
        System.out.println(">>>>>>>>>" + (String) (context.getRspAttachMap().get("key1")));
        assertEquals((String) (context.getRspAttachMap().get("key1")), "abc-abc");
    }

    @Extension("trpc_mock")
    public static class StandardRpcClientFactory implements RpcClientFactory {

        @Override
        public RpcClient createRpcClient(ProtocolConfig config) throws TRpcException {
            return new DefRpcClient(config, new StandardClientCodec());
        }
    }
}
