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

package com.tencent.trpc.core.selector.router.support;


import com.tencent.trpc.core.exception.TRpcException;
import com.tencent.trpc.core.extension.Extension;
import com.tencent.trpc.core.rpc.Request;
import com.tencent.trpc.core.selector.ServiceInstance;
import com.tencent.trpc.core.selector.router.AbstractRouter;
import java.util.List;
import java.util.stream.Collectors;

@Extension(NoneRouter.NAME)
public class NoneRouter extends AbstractRouter {

    public static final String NAME = "none";

    @Override
    protected List<ServiceInstance> doRoute(List<ServiceInstance> instances, Request request)
            throws TRpcException {
        if (instances != null) {
            List<ServiceInstance> result = instances.stream()
                    .filter(ServiceInstance::isHealthy)
                    .collect(Collectors.toList());
            if (result.size() == 0) {
                // If all isUnHealthy,return all!
                return instances;
            }
            return result;
        }
        return null;
    }
}
