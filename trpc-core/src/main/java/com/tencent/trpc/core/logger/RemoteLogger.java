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

package com.tencent.trpc.core.logger;

import com.tencent.trpc.core.rpc.RpcContext;

@SuppressWarnings("unchecked")
public interface RemoteLogger {

    void info(RpcContext context, String msg);

    void infoContext(RpcContext context, String msg);

    void traceContext(RpcContext context, String msg);

    void debugContext(RpcContext context, String msg);

    void warnContext(RpcContext context, String msg);

    void errorContext(RpcContext context, String msg);

}