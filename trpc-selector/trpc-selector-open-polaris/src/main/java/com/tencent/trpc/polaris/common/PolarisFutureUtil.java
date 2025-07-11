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

package com.tencent.trpc.polaris.common;

import com.google.common.base.Preconditions;
import com.tencent.polaris.api.rpc.InstancesFuture;
import com.tencent.polaris.api.rpc.InstancesResponse;
import com.tencent.trpc.core.exception.TRpcException;
import com.tencent.trpc.core.logger.Logger;
import com.tencent.trpc.core.logger.LoggerFactory;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Polaris instance Future utils
 */
public class PolarisFutureUtil {

    private static final Logger logger = LoggerFactory.getLogger(PolarisFutureUtil.class);

    /**
     * PolarisFuture to InstancesResponse
     *
     * @param polarisFuture polarisFuture
     * @param executor thread poll
     * @return polaris instance Future
     */
    public static CompletableFuture<InstancesResponse> toCompletableFuture(
            InstancesFuture polarisFuture, Executor executor) {

        Preconditions.checkNotNull(polarisFuture, "polaris future can not be null");
        Preconditions.checkNotNull(executor, "executor can not be null");

        CompletableFuture<InstancesResponse> future = new CompletableFuture<>();

        polarisFuture.whenCompleteAsync((res, exp) -> {
            try {
                if (logger.isDebugEnabled()) {
                    logger.debug("[selector] call polaris result:{}, exp:{}", res, exp);
                }
                if (exp != null) {
                    future.completeExceptionally(TRpcException.trans(exp));
                } else {
                    future.complete(res);
                }
            } catch (Exception e) {
                future.completeExceptionally(TRpcException.trans(e));
            }
        }, executor);

        return future;
    }
}
