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

import com.tencent.trpc.core.extension.Extension;
import com.tencent.trpc.core.limiter.spi.LimiterResourceExtractor;
import com.tencent.trpc.core.rpc.Invoker;
import com.tencent.trpc.core.rpc.Request;

/**
 * Default rate limiting resource identifier parsing plugin.
 */
@Extension("default")
public class DefLimiterResourceExtractor implements LimiterResourceExtractor {

    /**
     * Extract the rate limiting resource identifier,
     * using the name of the called service method as the resource identifier.
     *
     * @param filterChain Invoker
     * @param request Request
     * @return String name
     */
    @Override
    public String extract(Invoker<?> filterChain, Request request) {
        return request.getInvocation().getFunc();
    }

}
