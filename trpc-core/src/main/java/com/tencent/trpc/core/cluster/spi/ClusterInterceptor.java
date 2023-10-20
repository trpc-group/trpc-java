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

package com.tencent.trpc.core.cluster.spi;

import com.tencent.trpc.core.extension.Extensible;
import com.tencent.trpc.core.filter.Ordered;
import com.tencent.trpc.core.rpc.Invoker;
import com.tencent.trpc.core.rpc.Request;
import com.tencent.trpc.core.rpc.Response;
import java.util.concurrent.CompletionStage;

/**
 * Cluster interceptor, mainly used to intercept and enhance before and after the selector gets a single address.
 * However, the 'filter' is a chain processing after the 'selector' is obtained, and the timing of the two is different.
 * It can be extended through the SPI plugin.
 */
@Extensible
public interface ClusterInterceptor extends Ordered {

    CompletionStage<Response> intercept(Invoker<?> invoker, Request request);

}
