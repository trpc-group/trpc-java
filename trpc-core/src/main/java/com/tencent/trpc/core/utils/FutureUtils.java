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

package com.tencent.trpc.core.utils;

import com.tencent.trpc.core.logger.Logger;
import com.tencent.trpc.core.logger.LoggerFactory;
import com.tencent.trpc.core.rpc.TimeoutManager;
import java.time.Duration;
import java.util.Collection;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;

/**
 * Future utility.
 */
public class FutureUtils {

    private static final Logger LOG = LoggerFactory.getLogger(FutureUtils.class);

    public static CompletableFuture<Void> allOf(
            Collection<? extends CompletableFuture<?>> futures) {
        Objects.requireNonNull(futures, "futures must not be null");
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
    }

    public static <T> CompletableFuture<T> failed(Throwable throwable) {
        Objects.requireNonNull(throwable, "throwable must not be null");
        CompletableFuture<T> future = newFuture();
        future.completeExceptionally(throwable);
        return future;
    }

    public static <T> void failed(CompletableFuture<T> future, Throwable throwable) {
        Objects.requireNonNull(future, "future must not be null");
        Objects.requireNonNull(throwable, "throwable must not be null");
        future.completeExceptionally(throwable);
    }

    public static <T> CompletableFuture<T> newFuture() {
        return new CompletableFuture<>();
    }

    public static CompletableFuture<Void> newSuccessFuture() {
        CompletableFuture<Void> future = new CompletableFuture<>();
        future.complete(null);
        return future;
    }

    public static <T> CompletableFuture<T> newSuccessFuture(T t) {
        CompletableFuture<T> future = new CompletableFuture<>();
        future.complete(t);
        return future;
    }

    public static <T> CompletableFuture<T> adapt(CompletionStage<T> stage) {
        return stage == null ? null : stage.toCompletableFuture();
    }

    /**
     * Wrap a timeout-enabled future.
     *
     * @param source the original CompletionStage to wrap with a timeout
     * @param duration the duration of the timeout
     * @param timeoutManager the TimeoutManager to manage the timeout
     * @param <T> the type of the result of the CompletionStage
     * @return a new CompletionStage that is either completed by the original source or a timeout
     */
    public static <T> CompletionStage<T> withTimeout(CompletionStage<T> source, Duration duration,
            TimeoutManager timeoutManager) {
        Objects.requireNonNull(source, "source");
        final CompletableFuture<T> timeoutFuture = newTimeoutFuture(duration, timeoutManager);
        CompletionStage<T> result = source.applyToEither(timeoutFuture, Function.identity());
        return result.whenComplete((r, t) -> {
            if (!timeoutFuture.isDone()) {
                timeoutFuture.cancel(true);
            }
        });
    }

    private static <T> CompletableFuture<T> newTimeoutFuture(Duration duration,
            TimeoutManager timeoutManager) {
        CompletableFuture<T> promise = newFuture();
        Future<?> watch = timeoutManager.watch(() -> {
            try {
                promise.completeExceptionally(
                        new TimeoutException("timeout > " + duration.toMillis() + " ms"));
            } catch (Exception e) {
                LOG.error("timeout task watch exception.", e);
            }
        }, duration.toMillis());
        promise.whenComplete((r, t) -> {
            if (r != null || promise.isCancelled()) {
                watch.cancel(true);
            }
        });
        return promise;
    }

}
