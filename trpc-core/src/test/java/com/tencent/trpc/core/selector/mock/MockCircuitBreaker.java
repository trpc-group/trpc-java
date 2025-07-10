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

package com.tencent.trpc.core.selector.mock;

import com.tencent.trpc.core.selector.ServiceInstance;
import com.tencent.trpc.core.selector.spi.CircuitBreaker;
import java.util.concurrent.ThreadLocalRandom;

public class MockCircuitBreaker implements CircuitBreaker {

    @Override
    public boolean allowRequest(ServiceInstance serviceInstance) {
        int i = ThreadLocalRandom.current().nextInt(10);
        return i > 3;
    }

    @Override
    public boolean isOpen(ServiceInstance serviceInstance) {
        return false;
    }

    @Override
    public void report(ServiceInstance serviceInstance, int code, long delay) {

    }
}
