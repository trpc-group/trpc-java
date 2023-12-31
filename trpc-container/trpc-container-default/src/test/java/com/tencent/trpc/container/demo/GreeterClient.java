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

package com.tencent.trpc.container.demo;

import com.tencent.trpc.container.demo.HelloRequestProtocol.HelloRequest;
import com.tencent.trpc.container.demo.HelloRequestProtocol.HelloResponse;
import com.tencent.trpc.core.rpc.RpcContext;
import com.tencent.trpc.core.rpc.anno.TRpcMethod;
import com.tencent.trpc.core.rpc.anno.TRpcService;
import java.util.concurrent.CompletionStage;

@TRpcService(name = "10")
public interface GreeterClient {

    @TRpcMethod(name = "100")
    CompletionStage<HelloResponse> asyncSayHello(RpcContext context,
            HelloRequest request);

    @TRpcMethod(name = "100")
    HelloResponse sayHello(RpcContext context, HelloRequest request);

}
