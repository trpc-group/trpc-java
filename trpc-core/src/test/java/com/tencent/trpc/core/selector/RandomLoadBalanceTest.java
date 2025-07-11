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

import static org.junit.Assert.assertTrue;

import com.google.common.collect.Lists;
import com.tencent.trpc.core.selector.loadbalance.support.RandomLoadBalance;
import org.junit.Test;

public class RandomLoadBalanceTest {

    @Test
    public void test() {
        RandomLoadBalance balance = new RandomLoadBalance();
        ServiceInstance select =
                balance.select(Lists.newArrayList(new ServiceInstance("127.0.0.1", 8888)), null);
        assertTrue(select.getHost().equals("127.0.0.1"));
        assertTrue(select.getPort() == 8888);
    }
}
