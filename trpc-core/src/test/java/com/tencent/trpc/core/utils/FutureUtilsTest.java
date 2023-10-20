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

import com.google.common.collect.Lists;
import com.tencent.trpc.core.rpc.def.DefTimeoutManager;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import org.junit.Assert;
import org.junit.Test;

public class FutureUtilsTest {

    @Test
    public void test() {
        CompletionStage<Integer> stage = FutureUtils.newFuture();
        stage.toCompletableFuture().complete(1);
        Assert.assertEquals(FutureUtils.adapt(stage).join().intValue(), 1);
        CompletionStage<Integer> stage2 = FutureUtils.failed(new IllegalArgumentException());
        Exception expectEx2 = null;
        try {
            stage2.toCompletableFuture().join();
        } catch (Exception ex) {
            expectEx2 = ex;
        }
        Assert.assertTrue(expectEx2 != null && expectEx2.getCause() instanceof IllegalArgumentException);
        CompletionStage<Integer> stage3 = FutureUtils.newFuture();
        FutureUtils.failed(stage3.toCompletableFuture(), new IllegalArgumentException());
        Exception expectEx3 = null;
        try {
            stage3.toCompletableFuture().join();
        } catch (Exception ex) {
            expectEx3 = ex;
        }
        Assert.assertTrue(expectEx3 != null && expectEx3
                .getCause() instanceof IllegalArgumentException);
        FutureUtils.allOf(Lists.newArrayList(stage3.toCompletableFuture()));
        CompletionStage<Integer> timeoutStage = FutureUtils.withTimeout(FutureUtils.newFuture(),
                Duration.ofMillis(10), new DefTimeoutManager(10));
        Exception expectEx4 = null;
        try {
            timeoutStage.toCompletableFuture().join();
        } catch (Exception ex) {
            expectEx4 = ex;
        }
        Assert.assertTrue(expectEx4 != null && expectEx4.getCause() instanceof TimeoutException);
    }

    @Test
    public void testNewSuccessFuture() throws ExecutionException, InterruptedException {
        CompletableFuture<Void> voidCompletableFuture = FutureUtils.newSuccessFuture();
        Assert.assertNotNull(voidCompletableFuture);
        Object object = new Object();
        CompletableFuture<Object> objectCompletableFuture = FutureUtils.newSuccessFuture(object);
        Assert.assertSame(objectCompletableFuture.get().getClass(), Object.class);
    }
}
