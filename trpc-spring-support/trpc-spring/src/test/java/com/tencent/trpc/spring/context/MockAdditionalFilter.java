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

package com.tencent.trpc.spring.context;

import com.tencent.trpc.core.filter.spi.Filter;
import com.tencent.trpc.core.rpc.Invoker;
import com.tencent.trpc.core.rpc.Request;
import com.tencent.trpc.core.rpc.Response;
import java.util.concurrent.CompletionStage;

public class MockAdditionalFilter implements Filter {

    @Override
    public CompletionStage<Response> filter(Invoker<?> filterChain, Request req) {
        return filterChain.invoke(req);
    }
}
