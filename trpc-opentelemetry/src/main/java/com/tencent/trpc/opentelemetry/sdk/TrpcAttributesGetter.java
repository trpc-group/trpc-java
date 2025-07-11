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

package com.tencent.trpc.opentelemetry.sdk;

import com.tencent.trpc.core.rpc.Request;
import io.opentelemetry.instrumentation.api.instrumenter.rpc.RpcAttributesGetter;
import javax.annotation.Nullable;

/**
 * TRPC general attribute setting class
 */
public final class TrpcAttributesGetter implements RpcAttributesGetter<Request> {

    @Nullable
    @Override
    public String getSystem(Request request) {
        return Constants.TRPC;
    }

    @Nullable
    @Override
    public String getService(Request request) {
        return request.getMeta().getCallInfo().getCallerServer();
    }

    @Nullable
    @Override
    public String getMethod(Request request) {
        return request.getInvocation().getFunc();
    }

}
