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

package com.tencent.trpc.core.selector.spi;

import com.tencent.trpc.core.extension.Extensible;
import com.tencent.trpc.core.selector.ServiceInstance;
import com.tencent.trpc.core.selector.circuitbreaker.support.NoneCircuitBreaker;

/**
 * CircuitBreaker
 * <p>{@code allowRequest} The method returns whether access is allowed</p>
 * <p>{@code isOpen} return circuitBreaker is open or not</p>
 * <p>{@code report} Sets the circuitBreaker to closed(close),while requiring the circuitBreaker to be reset</p>
 */
@Extensible(NoneCircuitBreaker.NAME)
public interface CircuitBreaker {

    /**
     * Whether to allow access
     *
     * @return Returns whether the request can be submitted
     */
    boolean allowRequest(ServiceInstance serviceInstance);

    /**
     * Judging the current circuitBreaker status: open or close
     *
     * @return return circuitBreaker is open or not
     */
    boolean isOpen(ServiceInstance serviceInstance);

    /**
     * Sets the circuitBreaker to closed(close),while requiring the circuitBreaker to be reset
     */
    void report(ServiceInstance serviceInstance, int code, long delay);
}
