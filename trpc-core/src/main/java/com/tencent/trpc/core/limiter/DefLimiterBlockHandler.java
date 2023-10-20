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

package com.tencent.trpc.core.limiter;

import com.tencent.trpc.core.exception.LimiterBlockException;
import com.tencent.trpc.core.extension.Extension;
import com.tencent.trpc.core.limiter.spi.LimiterBlockHandler;
import com.tencent.trpc.core.rpc.Invoker;
import com.tencent.trpc.core.rpc.Request;
import com.tencent.trpc.core.rpc.Response;
import com.tencent.trpc.core.utils.FutureUtils;
import java.util.concurrent.CompletionStage;

/**
 * Default rate limiting callback plugin.
 */
@Extension("default")
public class DefLimiterBlockHandler implements LimiterBlockHandler {

    /**
     * Rate limiting callback processing, encapsulating rate limiting exception returns.
     *
     * @param filterChain Invoker
     * @param request Request
     * @param ex LimiterBlockException
     * @return CompletionStage Response
     */
    @Override
    public CompletionStage<Response> handle(Invoker<?> filterChain, Request request, LimiterBlockException ex) {
        CompletionStage<Response> completableFuture = FutureUtils.newFuture();
        completableFuture.toCompletableFuture().completeExceptionally(ex);
        return completableFuture;
    }

}