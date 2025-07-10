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

import com.tencent.trpc.core.extension.Extensible;
import com.tencent.trpc.core.rpc.Invoker;
import com.tencent.trpc.core.rpc.Request;
import com.tencent.trpc.core.rpc.Response;
import java.util.concurrent.CompletionStage;

/**
 * Universal interface for rate limiting plugins.
 *
 * <p>Rate limiting is used to protect service availability. It can be used to limit a piece of code,
 * a method, or an API interface, etc. The object that needs to be rate limited is called a "resource".</p>
 *
 * <p>Generally, a complete rate limiting process includes: rate limiting condition judgment,
 * rate limiting callback processing, and rate limiting degradation processing.</p>
 *
 * <p>1. Rate limiting condition judgment: Determines whether to trigger rate limiting for the "resource".
 * Each "resource" has a unique identifier,
 * and a custom resource identifier can be implemented through{@link LimiterResourceExtractor}.</p>
 *
 * <p>2. Rate limiting callback processing: The logic executed after the rate limiting condition is met.
 * Custom rate limiting callback processing can be implemented through the {@link LimiterBlockHandler} interface.</p>
 *
 * <p>3. Rate limiting degradation processing:
 * The logic executed when an exception occurs during the process of accessing the "resource"
 * that requires rate limiting.
 * Custom rate limiting degradation processing can be implemented through the {@link LimiterFallback} interface.</p>
 */
@Extensible
public interface Limiter {

    /**
     * Entry method for rate limiting.
     * The rate limiting judgment is made in this method to determine
     * whether to execute the rate limiting callback or the rate limiting degradation processing.
     *
     * @param filterChain Invoker
     * @param request Request
     * @return Response {@link CompletionStage}
     */
    CompletionStage<Response> block(Invoker<?> filterChain, Request request);

}