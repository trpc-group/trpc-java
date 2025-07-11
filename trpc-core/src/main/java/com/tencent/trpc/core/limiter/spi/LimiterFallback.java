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

package com.tencent.trpc.core.limiter.spi;

import com.tencent.trpc.core.exception.LimiterFallbackException;
import com.tencent.trpc.core.extension.Extensible;
import com.tencent.trpc.core.limiter.DefLimiterFallback;
import com.tencent.trpc.core.rpc.Invoker;
import com.tencent.trpc.core.rpc.Request;
import com.tencent.trpc.core.rpc.Response;
import java.util.concurrent.CompletionStage;

/**
 * Rate limiting degradation plugin.
 * Used for the logic that needs to be executed after an exception
 * occurs during the process of accessing the rate limiting "resource".
 *
 * <p>Custom degradation implementations need to implement this interface,
 * and can refer to {@link DefLimiterFallback}.</p>
 */
@Extensible
public interface LimiterFallback {

    /**
     * Entry method for rate limiting degradation.
     *
     * <p>If an exception occurs during the process of accessing the rate limiting "resource",
     * the rate limiting degradation interface will be called. However,
     * if an exception occurs in the rate limiting callback method {@link LimiterBlockHandler#handle},
     * the degradation processing will not be called.</p>
     *
     * @param filterChain Invoker
     * @param request Request
     * @param ex LimiterFallbackException
     * @return CompletionStage Response
     */
    CompletionStage<Response> fallback(Invoker<?> filterChain, Request request, LimiterFallbackException ex);

}