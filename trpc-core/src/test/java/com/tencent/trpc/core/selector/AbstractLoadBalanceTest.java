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

package com.tencent.trpc.core.selector;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.google.common.collect.Lists;
import com.tencent.trpc.core.exception.TRpcException;
import com.tencent.trpc.core.rpc.Request;
import com.tencent.trpc.core.rpc.def.DefRequest;
import com.tencent.trpc.core.selector.loadbalance.AbstractLoadBalance;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

public class AbstractLoadBalanceTest {

    @Test
    public void test() {
        LoadBalanceTest testCase = new LoadBalanceTest();
        ServiceInstance a = new ServiceInstance();
        ServiceInstance b = new ServiceInstance();
        assertEquals(b, testCase.select(Lists.newArrayList(a, b), new DefRequest()));

        assertEquals(a, testCase.select(Lists.newArrayList(a), new DefRequest()));

        assertNull(testCase.select(new ArrayList<>(), new DefRequest()));
    }

    public static class LoadBalanceTest extends AbstractLoadBalance {

        @Override
        protected ServiceInstance doSelect(List<ServiceInstance> instances, Request request)
                throws TRpcException {
            return instances.get(1);
        }

    }
}
