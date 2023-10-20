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

package com.tencent.trpc.core.selector;

import static org.junit.Assert.assertEquals;

import com.tencent.trpc.core.selector.discovery.ListDiscovery;
import org.junit.Test;

public class ListDiscoveryTest {

    @Test
    public void test() {
        ListDiscovery listDiscovery = new ListDiscovery();
        listDiscovery.init();
        ServiceId serviceId = new ServiceId();
        serviceId.setServiceName("192.168.1.1:100");
        assertEquals("192.168.1.1",
                listDiscovery.asyncList(serviceId, null).toCompletableFuture().join().get(0)
                        .getHost());
        assertEquals(100,
                listDiscovery.asyncList(serviceId, null).toCompletableFuture().join().get(0)
                        .getPort());

        // ipv6
        serviceId.setServiceName("fe80::4aa:2955:3049:9eb7%en0:12345");
        assertEquals("fe80::4aa:2955:3049:9eb7%en0",
                listDiscovery.asyncList(serviceId, null).toCompletableFuture().join().get(0)
                        .getHost());
        assertEquals(12345,
                listDiscovery.asyncList(serviceId, null).toCompletableFuture().join().get(0)
                        .getPort());
    }
}
