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

package com.tencent.trpc.spring.cloud.gateway.filter.test.server;

import com.tencent.trpc.core.rpc.RpcContext;
import com.tencent.trpc.core.rpc.anno.TRpcMethod;
import com.tencent.trpc.core.rpc.anno.TRpcService;
import com.tencent.trpc.spring.cloud.gateway.filter.test.server.Hello.HelloReq;
import com.tencent.trpc.spring.cloud.gateway.filter.test.server.Hello.HelloRsp;

@TRpcService(name = "trpc.test.demo.Hello")
public interface HelloAPI {

    @TRpcMethod(name = "SayHello")
    HelloRsp sayHello(RpcContext context, HelloReq request);

}