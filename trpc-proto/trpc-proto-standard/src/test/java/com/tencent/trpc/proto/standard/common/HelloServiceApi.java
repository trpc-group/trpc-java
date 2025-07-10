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

package com.tencent.trpc.proto.standard.common;

import com.tencent.trpc.core.rpc.RpcContext;
import com.tencent.trpc.core.rpc.anno.TRpcMethod;
import com.tencent.trpc.core.rpc.anno.TRpcService;

@TRpcService(name = "trpc.TestApp.TestServer.Hello")
public interface HelloServiceApi {

    @TRpcMethod(name = "sayHello")
    HelloRequestProtocol.HelloResponse sayHello(RpcContext context, HelloRequestProtocol.HelloRequest request);

    @TRpcMethod(name = "doUnExitedMethod")
    HelloRequestProtocol.HelloResponse doUnExitedMethod(RpcContext context, HelloRequestProtocol.HelloRequest request);

    @TRpcMethod(name = "doDefaultMethod", isDefault = true)
    HelloRequestProtocol.HelloResponse doDefaultMethod(RpcContext context, HelloRequestProtocol.HelloRequest request);

    @TRpcMethod(name = "doGenericMethod", isGeneric = true)
    byte[] doGenericMethod(RpcContext context, byte[] request);
}
