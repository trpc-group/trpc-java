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

import com.tencent.trpc.core.rpc.RpcContext;

public class TestRemoteLogger implements RemoteLogger {

    @Override
    public void info(RpcContext context, String msg) {

    }

    @Override
    public void infoContext(RpcContext context, String msg) {
    }

    @Override
    public void traceContext(RpcContext context, String msg) {
    }

    @Override
    public void debugContext(RpcContext context, String msg) {
    }

    @Override
    public void warnContext(RpcContext context, String msg) {
    }

    @Override
    public void errorContext(RpcContext context, String msg) {
    }
}
