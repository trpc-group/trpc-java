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

package com.tencent.trpc.core.selector.spi;

import com.tencent.trpc.core.exception.TRpcException;
import com.tencent.trpc.core.extension.Extensible;
import com.tencent.trpc.core.rpc.Request;
import com.tencent.trpc.core.selector.ServiceId;
import com.tencent.trpc.core.selector.ServiceInstance;
import java.util.List;
import java.util.concurrent.CompletionStage;

/**
 * cluster naming is the abstract of discoverying service instances, router, load balance and
 * circuitBreaker
 */
@Extensible("polaris")
public interface Selector {

    default void warmup(ServiceId serviceId) {

    }

    /**
     * Select one available {@link ServiceInstance} of
     * <li>ServiceId</li> if there exists more than one ServiceInstance, a load balance will apply
     * to select one ServiceInstance. the returned ServiceInstance is not circuit broken
     *
     * @param serviceId the identifier of service
     * @param request the request, must not be null
     * @return CompletionStage : available ServiceInstance or null if can not find any valid
     *         ServiceInstance
     */
    CompletionStage<ServiceInstance> asyncSelectOne(ServiceId serviceId, Request request);

    /**
     * Async select all available ServiceInstances of
     * <li>ServiceId</li>
     *
     * @param serviceId the service id
     * @param request the request, must not be null
     * @return CompletionStage : all available ServiceInstances or empty list if can not find any
     *         valid ServiceInstance
     */
    CompletionStage<List<ServiceInstance>> asyncSelectAll(ServiceId serviceId, Request request);

    /**
     * Report the rpc call result of the serviceInstance
     *
     * @param serviceInstance serviceInstance
     * @param code result code
     * @param costMs the duration of rpc call, in millisecond
     * @throws TRpcException the t rpc exception
     */
    void report(ServiceInstance serviceInstance, int code, long costMs) throws TRpcException;
}
