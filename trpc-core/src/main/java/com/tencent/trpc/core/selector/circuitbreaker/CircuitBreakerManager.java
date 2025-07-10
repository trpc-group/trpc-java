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

package com.tencent.trpc.core.selector.circuitbreaker;

import com.tencent.trpc.core.extension.ExtensionManager;
import com.tencent.trpc.core.selector.spi.CircuitBreaker;

public class CircuitBreakerManager {

    private static ExtensionManager<CircuitBreaker> manager = new ExtensionManager<>(CircuitBreaker.class);

    public static final ExtensionManager<CircuitBreaker> getManager() {
        return manager;
    }
}
