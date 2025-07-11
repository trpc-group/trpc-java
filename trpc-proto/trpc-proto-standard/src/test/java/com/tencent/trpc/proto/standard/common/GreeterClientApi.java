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

import com.tencent.trpc.core.rpc.RpcClientContext;
import com.tencent.trpc.core.rpc.anno.TRpcMethod;
import com.tencent.trpc.core.rpc.anno.TRpcService;
import com.tencent.trpc.proto.standard.common.HelloRequestProtocol.HelloRequest;
import com.tencent.trpc.proto.standard.common.HelloRequestProtocol.HelloResponse;
import java.util.concurrent.CompletionStage;

@TRpcService(name = "tencent.trpc.GreeterService")
public interface GreeterClientApi {

    @TRpcMethod(name = "sayHello")
    CompletionStage<HelloResponse> asyncSayHello(RpcClientContext context, HelloRequest request);

    @TRpcMethod(name = "sayHello")
    HelloResponse sayHello(RpcClientContext context, HelloRequest request);

    @TRpcMethod(name = "sayHellox")
    HelloResponse sayHellox(RpcClientContext context, HelloRequest request);
}
