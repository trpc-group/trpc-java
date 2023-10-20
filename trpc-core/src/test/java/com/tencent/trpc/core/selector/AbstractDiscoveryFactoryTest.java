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

package com.tencent.trpc.core.selector;

import static org.junit.Assert.assertEquals;

import com.google.common.collect.Lists;
import com.tencent.trpc.core.selector.spi.Discovery;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import org.junit.Test;

public class AbstractDiscoveryFactoryTest {

    public static final ServiceInstance SYNC_RESULT = new ServiceInstance();
    public static final ServiceInstance ASYNC_RESULT = new ServiceInstance();

    @Test
    public void test() {
        Discovery discovery = new Discovery() {
            @Override
            public List<ServiceInstance> list(ServiceId serviceId) {
                return Lists.newArrayList(SYNC_RESULT);
            }

            @Override
            public CompletionStage<List<ServiceInstance>> asyncList(ServiceId serviceId,
                    Executor executor) {
                return CompletableFuture.completedFuture(Lists.newArrayList(ASYNC_RESULT));
            }
        };
        assertEquals(SYNC_RESULT, discovery.list(new ServiceId()).get(0));
        assertEquals(ASYNC_RESULT, discovery.asyncList(new ServiceId(), null).toCompletableFuture().join().get(0));
    }
}
