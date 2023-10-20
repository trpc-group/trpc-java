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

package com.tencent.trpc.core.selector.loadbalance;

import com.tencent.trpc.core.exception.TRpcException;
import com.tencent.trpc.core.rpc.Request;
import com.tencent.trpc.core.selector.ServiceInstance;
import com.tencent.trpc.core.selector.spi.LoadBalance;
import java.util.List;

public abstract class AbstractLoadBalance implements LoadBalance {

    @Override
    public ServiceInstance select(List<ServiceInstance> instances, Request request)
            throws TRpcException {
        if (instances == null || instances.isEmpty()) {
            return null;
        }

        if (instances.size() == 1) {
            return instances.get(0);
        }

        return doSelect(instances, request);
    }


    protected abstract ServiceInstance doSelect(List<ServiceInstance> instances, Request request)
            throws TRpcException;
}
