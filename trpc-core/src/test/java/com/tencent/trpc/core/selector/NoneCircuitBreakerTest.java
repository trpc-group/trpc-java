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

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tencent.trpc.core.selector.circuitbreaker.support.NoneCircuitBreaker;
import com.tencent.trpc.core.selector.spi.CircuitBreaker;
import java.util.HashMap;
import org.junit.jupiter.api.Test;

public class NoneCircuitBreakerTest {

    @Test
    public void test() {
        ServiceInstance serviceInstance = new ServiceInstance("127.0.0.1", 123, new HashMap<>());
        CircuitBreaker create = new NoneCircuitBreaker();

        assertTrue(create.allowRequest(serviceInstance));
        assertTrue(!create.isOpen(serviceInstance));
    }
}
