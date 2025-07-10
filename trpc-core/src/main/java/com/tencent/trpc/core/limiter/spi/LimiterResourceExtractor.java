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
import com.tencent.trpc.core.limiter.DefLimiterResourceExtractor;
import com.tencent.trpc.core.rpc.Invoker;
import com.tencent.trpc.core.rpc.Request;

/**
 * Plugin for parsing rate limiting resource identifiers.
 *
 * <p>Rate limiting can be applied to a piece of code,
 * a method, or an API interface. The object that needs to be rate limited is called a "resource",
 * and each "resource" has a unique identifier.</p>
 *
 * <p>Custom rate limiting resource parsing plugins need to implement this interface,
 * and can refer to {@link DefLimiterResourceExtractor}.</p>
 */
@Extensible
public interface LimiterResourceExtractor {

    /**
     * Extract the name of the rate limiting resource identifier.
     *
     * @param filterChain Invoker
     * @param request Request
     * @return String
     */
    String extract(Invoker<?> filterChain, Request request);

}
