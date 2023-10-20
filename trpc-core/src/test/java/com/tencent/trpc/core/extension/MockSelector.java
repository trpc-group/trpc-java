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

package com.tencent.trpc.core.extension;

import com.tencent.trpc.core.exception.TRpcException;
import com.tencent.trpc.core.rpc.Request;
import com.tencent.trpc.core.selector.ServiceId;
import com.tencent.trpc.core.selector.ServiceInstance;
import com.tencent.trpc.core.selector.spi.Selector;
import java.util.List;
import java.util.concurrent.CompletionStage;

@Extension("mock")
public class MockSelector implements Selector {

    @Override
    public CompletionStage<ServiceInstance> asyncSelectOne(ServiceId serviceId, Request request) {
        return null;
    }

    @Override
    public CompletionStage<List<ServiceInstance>> asyncSelectAll(ServiceId serviceId, Request request) {
        return null;
    }

    @Override
    public void report(ServiceInstance serviceInstance, int code, long costMs) throws TRpcException {
    }
}