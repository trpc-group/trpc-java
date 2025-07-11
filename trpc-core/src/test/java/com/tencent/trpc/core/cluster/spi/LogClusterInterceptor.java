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

package com.tencent.trpc.core.cluster.spi;

import com.tencent.trpc.core.extension.Extension;
import com.tencent.trpc.core.logger.Logger;
import com.tencent.trpc.core.logger.LoggerFactory;
import com.tencent.trpc.core.rpc.Invoker;
import com.tencent.trpc.core.rpc.Request;
import com.tencent.trpc.core.rpc.Response;
import java.util.concurrent.CompletionStage;

@Extension(value = "log")
public class LogClusterInterceptor implements ClusterInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(LogClusterInterceptor.class);

    @Override
    public CompletionStage<Response> intercept(Invoker<?> invoker, Request request) {
        logger.debug("the log cluster interceptor before invoke");
        return invoker.invoke(request);
    }
}
