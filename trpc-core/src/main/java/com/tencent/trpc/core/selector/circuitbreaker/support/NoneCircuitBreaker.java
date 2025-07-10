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

package com.tencent.trpc.core.selector.circuitbreaker.support;


import com.tencent.trpc.core.extension.Extension;
import com.tencent.trpc.core.selector.ServiceInstance;
import com.tencent.trpc.core.selector.circuitbreaker.AbstractCircuitBreaker;

@Extension(NoneCircuitBreaker.NAME)
public class NoneCircuitBreaker extends AbstractCircuitBreaker {

    public static final String NAME = "none";

    @Override
    public boolean allowRequest(ServiceInstance serviceInstance) {
        return true;
    }

    @Override
    public boolean isOpen(ServiceInstance serviceInstance) {
        return false;
    }

    @Override
    public void report(ServiceInstance serviceInstance, int code, long delay) {

    }
}
