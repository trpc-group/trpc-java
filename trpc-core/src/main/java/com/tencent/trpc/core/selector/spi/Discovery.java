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
import com.tencent.trpc.core.selector.ServiceId;
import com.tencent.trpc.core.selector.ServiceInstance;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;

@Extensible("polaris")
public interface Discovery {

    /**
     * Service discovery interface
     * @param serviceId come from consumerConfig and contains many param for remark service
     */
    List<ServiceInstance> list(ServiceId serviceId);

    /**
     * Async service discovery interface
     *
     * @param serviceId come from consumerConfig and contains many param for remark service
     * @param executor async thread poll executor
     */
    CompletionStage<List<ServiceInstance>> asyncList(ServiceId serviceId, Executor executor);
}
