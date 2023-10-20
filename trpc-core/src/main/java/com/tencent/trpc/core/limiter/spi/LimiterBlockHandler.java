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

package com.tencent.trpc.core.limiter.spi;

import com.tencent.trpc.core.exception.LimiterBlockException;
import com.tencent.trpc.core.extension.Extensible;
import com.tencent.trpc.core.limiter.DefLimiterBlockHandler;
import com.tencent.trpc.core.rpc.Invoker;
import com.tencent.trpc.core.rpc.Request;
import com.tencent.trpc.core.rpc.Response;
import java.util.concurrent.CompletionStage;

/**
 * Rate limiting callback processing plugin.
 * Used for the logic that needs to be executed after the rate limiting "resource" is triggered for rate limiting.
 *
 * <p>Custom rate limiting callback processing plugins need to implement this interface,
 * and can refer to {@link DefLimiterBlockHandler}.</p>
 */
@Extensible
public interface LimiterBlockHandler {

    /**
     * Entry method for the callback after rate limiting is triggered.
     *
     * @param filterChain Invoker
     * @param request Request
     * @param ex LimiterBlockException
     * @return CompletionStage Response
     */
    CompletionStage<Response> handle(Invoker<?> filterChain, Request request, LimiterBlockException ex);

}