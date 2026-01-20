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

import com.tencent.trpc.core.exception.LimiterBlockException;
import com.tencent.trpc.core.extension.ExtensionLoader;
import com.tencent.trpc.core.limiter.spi.LimiterBlockHandler;
import com.tencent.trpc.core.logger.TestInvoker;
import com.tencent.trpc.core.rpc.Response;
import com.tencent.trpc.core.rpc.RpcInvocation;
import com.tencent.trpc.core.rpc.def.DefRequest;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class DefLimiterBlockHandlerTest {

    @Test
    public void testFallback() {
        DefLimiterBlockHandler limiterBlockHandler = (DefLimiterBlockHandler) ExtensionLoader
                .getExtensionLoader(LimiterBlockHandler.class).getExtension("default");

        DefRequest request = new DefRequest();
        RpcInvocation rpcInvocation = new RpcInvocation();
        rpcInvocation.setFunc("/test/func");
        request.setInvocation(rpcInvocation);

        CompletionStage<Response> completionStage = limiterBlockHandler.handle(new TestInvoker(), new DefRequest(),
                new LimiterBlockException("test call block handler"));
        try {
            completionStage.toCompletableFuture().get().getException();
        } catch (InterruptedException exception) {
            exception.printStackTrace();
        } catch (ExecutionException executionException) {
            Throwable cause = executionException.getCause();
            Assertions.assertTrue(cause instanceof LimiterBlockException);
            Assertions.assertTrue("test call block handler".equals(cause.getMessage()));
        }
    }

}
