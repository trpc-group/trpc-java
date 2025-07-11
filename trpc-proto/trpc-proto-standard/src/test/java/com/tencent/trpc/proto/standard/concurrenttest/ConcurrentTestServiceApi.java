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

package com.tencent.trpc.proto.standard.concurrenttest;

import com.tencent.trpc.core.rpc.RpcClientContext;
import com.tencent.trpc.core.rpc.anno.TRpcMethod;
import com.tencent.trpc.core.rpc.anno.TRpcService;
import com.tencent.trpc.proto.standard.common.HelloRequestProtocol;
import java.util.concurrent.CompletionStage;

@TRpcService(name = "trpc.TestApp.TestServer.Greeter")
public interface ConcurrentTestServiceApi {

    @TRpcMethod(name = "sayHello")
    HelloRequestProtocol.HelloResponse sayHello(RpcClientContext context,
            HelloRequestProtocol.HelloRequest request);

    @TRpcMethod(name = "sayHello")
    CompletionStage<HelloRequestProtocol.HelloResponse> asyncSayHello(RpcClientContext context,
            HelloRequestProtocol.HelloRequest request);

}
