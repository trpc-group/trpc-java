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
import com.tencent.trpc.core.selector.ServiceInstance;
import com.tencent.trpc.core.selector.loadbalance.support.RandomLoadBalance;
import java.util.List;

/**
 * The interface Load balance.
 */
@Extensible(RandomLoadBalance.NAME)
public interface LoadBalance {

    /**
     * Select service instance.
     *
     * @param instances the instances
     * @param request the request
     * @return the service instance, may bull if can`t find any valid ServiceInstance
     * @throws TRpcException the t rpc exception if any exception happens
     */
    ServiceInstance select(List<ServiceInstance> instances, Request request) throws TRpcException;
}
