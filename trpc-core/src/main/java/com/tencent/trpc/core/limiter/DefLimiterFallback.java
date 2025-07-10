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

package com.tencent.trpc.core.limiter;

import com.tencent.trpc.core.exception.LimiterFallbackException;
import com.tencent.trpc.core.extension.Extension;
import com.tencent.trpc.core.limiter.spi.LimiterFallback;
import com.tencent.trpc.core.rpc.Invoker;
import com.tencent.trpc.core.rpc.Request;
import com.tencent.trpc.core.rpc.Response;
import com.tencent.trpc.core.utils.FutureUtils;
import java.util.concurrent.CompletionStage;

/**
 * Fallback Limiter
 */
@Extension("default")
public class DefLimiterFallback implements LimiterFallback {

    @Override
    public CompletionStage<Response> fallback(Invoker<?> filterChain, Request request, LimiterFallbackException ex) {
        CompletionStage<Response> completableFuture = FutureUtils.newFuture();
        completableFuture.toCompletableFuture().completeExceptionally(ex);
        return completableFuture;
    }

}
