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

package com.tencent.trpc.limiter.sentinel.filter;

import com.tencent.trpc.core.exception.TRpcExtensionException;
import com.tencent.trpc.core.extension.ExtensionLoader;
import com.tencent.trpc.core.extension.InitializingExtension;
import com.tencent.trpc.core.filter.FilterOrdered;
import com.tencent.trpc.core.filter.spi.Filter;
import com.tencent.trpc.core.limiter.spi.Limiter;
import com.tencent.trpc.core.logger.Logger;
import com.tencent.trpc.core.logger.LoggerFactory;
import com.tencent.trpc.core.rpc.Invoker;
import com.tencent.trpc.core.rpc.Request;
import com.tencent.trpc.core.rpc.Response;
import com.tencent.trpc.limiter.sentinel.SentinelLimiter;
import java.util.concurrent.CompletionStage;

/**
 * Default Sentinel flow control filter implementation.
 */
public class SentinelLimiterFilter implements Filter, InitializingExtension {

    private static final Logger logger = LoggerFactory.getLogger(SentinelLimiterFilter.class);

    /**
     * Sentinel flow control filter name.
     */
    private static final String NAME = "sentinel";
    /**
     * Sentinel flow control plugin.
     */
    private SentinelLimiter sentinelLimiter;

    @Override
    public void init() throws TRpcExtensionException {
        sentinelLimiter = (SentinelLimiter) ExtensionLoader.getExtensionLoader(Limiter.class).getExtension(NAME);
        logger.debug("init sentinel limiter success, sentinelLimiter:{}", sentinelLimiter);
    }

    @Override
    public int getOrder() {
        return FilterOrdered.SENTINEL_LIMITER_ORDERED;
    }

    @Override
    public CompletionStage<Response> filter(Invoker<?> filterChain, Request req) {
        return sentinelLimiter.block(filterChain, req);
    }

}
