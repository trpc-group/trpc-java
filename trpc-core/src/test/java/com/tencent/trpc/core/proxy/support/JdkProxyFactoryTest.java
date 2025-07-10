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

package com.tencent.trpc.core.proxy.support;

import static org.junit.Assert.assertNotNull;

import com.tencent.trpc.core.cluster.ClusterInvoker;
import org.junit.Test;

public class JdkProxyFactoryTest {

    @Test
    public void testGetProxy() {

        JdkProxyFactory jdkProxyFactory = new JdkProxyFactory();

        ClusterInvoker<?> clusterInvokerProxy = jdkProxyFactory.getProxy(ClusterInvoker.class,
                (proxy, method, args) -> method.invoke(proxy, args));

        assertNotNull(clusterInvokerProxy);
    }

}