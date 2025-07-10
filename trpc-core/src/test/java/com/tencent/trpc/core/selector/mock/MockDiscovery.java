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

import com.tencent.trpc.core.selector.ServiceId;
import com.tencent.trpc.core.selector.ServiceInstance;
import com.tencent.trpc.core.selector.spi.Discovery;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;

public class MockDiscovery implements Discovery {

    private List<ServiceInstance> serviceInstanceList;

    public void setServiceInstances(List<ServiceInstance> serviceInstanceList) {
        this.serviceInstanceList = serviceInstanceList;
    }

    @Override
    public List<ServiceInstance> list(ServiceId serviceId) {
        return serviceInstanceList;
    }

    @Override
    public CompletionStage<List<ServiceInstance>> asyncList(ServiceId serviceId,
            Executor executor) {
        return CompletableFuture.completedFuture(serviceInstanceList);
    }
}
