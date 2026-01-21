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

import com.tencent.trpc.core.cluster.def.DefClusterInvocationHandlerTest.BlankRpcServiceName;
import com.tencent.trpc.core.cluster.def.DefClusterInvocationHandlerTest.BlankRpcServiceNameImpl;
import com.tencent.trpc.core.common.config.ProtocolConfig;
import com.tencent.trpc.core.common.config.ProviderConfig;
import com.tencent.trpc.core.common.config.ServiceConfig;
import com.tencent.trpc.core.rpc.RpcClientContext;
import com.tencent.trpc.core.rpc.RpcInvocation;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class DefProviderInvokerTest {

    private DefProviderInvoker<BlankRpcServiceName> providerInvoker;

    private BlankRpcServiceName impl;

    @BeforeEach
    public void setUp() {
        ProtocolConfig protocolConfig = new ProtocolConfig();
        protocolConfig.setIp("127.0.0.1");
        protocolConfig.setPort(12345);
        ProviderConfig<BlankRpcServiceName> objectProviderConfig = new ProviderConfig<>();
        objectProviderConfig.setServiceInterface(BlankRpcServiceName.class);
        impl = new BlankRpcServiceNameImpl();
        objectProviderConfig.setRef(impl);
        ServiceConfig serviceConfig = new ServiceConfig();
        serviceConfig.setEnableLinkTimeout(false);
        objectProviderConfig.setServiceConfig(serviceConfig);
        providerInvoker = new DefProviderInvoker<>(protocolConfig, objectProviderConfig);
    }

    @Test
    public void testInvoke() {
        DefRequest request = new DefRequest();
        request.setContext(new RpcClientContext());
        request.setInvocation(new RpcInvocation());
        providerInvoker.invoke(request);
        RpcInvocation invocation = new RpcInvocation();
        invocation.setRpcMethodName("blank");
        request.setInvocation(invocation);
        providerInvoker.invoke(request);
    }

    @Test
    public void testGetInterface() {
        Assertions.assertEquals(BlankRpcServiceName.class, providerInvoker.getInterface());
    }

    @Test
    public void testGetProtocolConfig() {
        Assertions.assertEquals("127.0.0.1", providerInvoker.getProtocolConfig().getIp());
        Assertions.assertEquals(12345, providerInvoker.getProtocolConfig().getPort());
    }

    @Test
    public void testGetImpl() {
        Assertions.assertEquals(impl, providerInvoker.getImpl());
    }

    @Test
    public void testGetConfig() {
        Assertions.assertEquals(impl, providerInvoker.getConfig().getRef());
    }
}
