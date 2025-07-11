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

package com.tencent.trpc.core.logger;

import com.tencent.trpc.core.rpc.Invoker;
import com.tencent.trpc.core.rpc.Request;
import com.tencent.trpc.core.rpc.Response;
import com.tencent.trpc.core.rpc.def.DefResponse;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class TestInvoker implements Invoker<TestRemoteLogger> {

    @Override
    public Class getInterface() {
        return TestRemoteLogger.class;
    }

    @Override
    public CompletionStage<Response> invoke(Request request) {
        DefResponse defResponse = new DefResponse();
        defResponse.setValue(new Object());
        return CompletableFuture.completedFuture(defResponse);
    }
}
