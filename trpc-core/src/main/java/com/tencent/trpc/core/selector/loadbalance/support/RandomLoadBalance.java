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

package com.tencent.trpc.core.selector.loadbalance.support;

import com.tencent.trpc.core.exception.TRpcException;
import com.tencent.trpc.core.extension.Extension;
import com.tencent.trpc.core.rpc.Request;
import com.tencent.trpc.core.selector.ServiceInstance;
import com.tencent.trpc.core.selector.spi.LoadBalance;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Extension(RandomLoadBalance.NAME)
public class RandomLoadBalance implements LoadBalance {

    public static final String NAME = "random";

    @Override
    public ServiceInstance select(List<ServiceInstance> instances, Request request)
            throws TRpcException {
        if (instances.size() == 0) {
            return null;
        } else {
            if (instances.size() == 1) {
                return instances.get(0);
            } else {
                return instances.get(ThreadLocalRandom.current().nextInt(instances.size()));
            }
        }
    }
}
