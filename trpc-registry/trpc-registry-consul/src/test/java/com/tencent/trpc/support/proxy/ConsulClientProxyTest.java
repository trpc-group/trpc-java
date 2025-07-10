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

package com.tencent.trpc.support.proxy;

import com.ecwid.consul.v1.ConsulClient;
import com.tencent.trpc.support.ConsulInstanceManager;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * Consul client proxy test class
 */
public class ConsulClientProxyTest {

    @Test
    public void getProxy() {
        ConsulInstanceManager consulRegistry = Mockito.mock(ConsulInstanceManager.class);
        ConsulClientProxy consulClientProxy = new ConsulClientProxy(consulRegistry);
        ConsulClient consulClient = new ConsulClient();
        consulClientProxy.resetConsulClient(consulClient);
    }

}