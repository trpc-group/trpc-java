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

package com.tencent.trpc.core.selector.router;

import com.tencent.trpc.core.exception.TRpcException;
import com.tencent.trpc.core.rpc.Request;
import com.tencent.trpc.core.selector.ServiceInstance;
import com.tencent.trpc.core.selector.spi.Router;
import java.util.List;

public abstract class AbstractRouter implements Router {

    @Override
    public List<ServiceInstance> route(List<ServiceInstance> instances, Request request)
            throws TRpcException {
        return doRoute(instances, request);
    }

    protected abstract List<ServiceInstance> doRoute(List<ServiceInstance> instances,
            Request request)
            throws TRpcException;

}
