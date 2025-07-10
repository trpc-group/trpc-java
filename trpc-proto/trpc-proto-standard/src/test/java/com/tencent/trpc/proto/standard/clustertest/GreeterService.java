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

package com.tencent.trpc.proto.standard.clustertest;

import com.tencent.trpc.core.rpc.RpcServerContext;
import com.tencent.trpc.core.rpc.anno.TRpcMethod;
import com.tencent.trpc.core.rpc.anno.TRpcService;
import com.tencent.trpc.proto.standard.common.HelloRequestProtocol;

@TRpcService(name = "trpc.TestApp.TestServer.Greeter")
public interface GreeterService {

    @TRpcMethod(name = "sayHello")
    HelloRequestProtocol.HelloResponse sayHello(RpcServerContext context,
            HelloRequestProtocol.HelloRequest request);

}
