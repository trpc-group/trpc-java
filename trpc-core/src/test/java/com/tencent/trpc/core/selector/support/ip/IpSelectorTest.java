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

package com.tencent.trpc.core.selector.support.ip;

import com.tencent.trpc.core.rpc.def.DefRequest;
import com.tencent.trpc.core.selector.ServiceId;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class IpSelectorTest {

    @Rule
    public ExpectedException expectedEx = ExpectedException.none();

    private IpSelector ipSelector;

    private ServiceId serviceId;

    @Before
    public void setUp() {
        this.ipSelector = new IpSelector();
        ipSelector.init();

        serviceId = new ServiceId();
        serviceId.setServiceName("a");
        serviceId.setGroup("a");
        serviceId.setVersion("a");
    }

    @Test
    public void testWarmup() {
        serviceId.setServiceName("127.0.0.1:8080");
        ipSelector.warmup(serviceId);
    }

    @Test
    public void testAsyncSelectOne() {
        serviceId.setServiceName("127.0.0.1:8080");
        ipSelector.asyncSelectOne(serviceId, new DefRequest());
    }

    @Test
    public void testAsyncSelectAll() {
        serviceId.setServiceName("127.0.0.1:8080");
        ipSelector.asyncSelectAll(serviceId, new DefRequest());
    }

    @Test
    public void testSelectAll() {
        serviceId.setServiceName("127.0.0.1:8080");
        ipSelector.selectAll(serviceId);
    }

    @Test
    public void testReport() {
        ipSelector.report(null, 0, 0);
    }
}