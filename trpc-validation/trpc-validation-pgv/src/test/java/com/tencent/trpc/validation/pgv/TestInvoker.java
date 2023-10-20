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

package com.tencent.trpc.validation.pgv;

import com.tencent.trpc.core.rpc.Invoker;
import com.tencent.trpc.core.rpc.Request;
import com.tencent.trpc.core.rpc.Response;
import com.tencent.trpc.core.rpc.def.DefResponse;
import com.tencent.trpc.validation.pgv.TestValidator.TestResponse;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class TestInvoker implements Invoker<TestValidator> {

    @Override
    public Class getInterface() {
        return TestValidator.class;
    }

    @Override
    public CompletionStage<Response> invoke(Request request) {
        DefResponse defResponse = new DefResponse();
        TestResponse response = new TestResponse();
        response.setVar1("2222222");
        defResponse.setValue(response);
        return CompletableFuture.completedFuture(defResponse);
    }

}
