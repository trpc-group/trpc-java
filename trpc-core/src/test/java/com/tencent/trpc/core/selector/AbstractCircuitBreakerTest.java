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

import com.tencent.trpc.core.selector.circuitbreaker.AbstractCircuitBreaker;
import org.junit.Test;

public class AbstractCircuitBreakerTest {

    @Test
    public void test() {
        new AbstractCircuitBreaker() {
            @Override
            public boolean allowRequest(ServiceInstance serviceInstance) {
                return false;
            }

            @Override
            public boolean isOpen(ServiceInstance serviceInstance) {
                return false;
            }

            @Override
            public void report(ServiceInstance serviceInstance, int code, long delay) {

            }
        };
    }
}
