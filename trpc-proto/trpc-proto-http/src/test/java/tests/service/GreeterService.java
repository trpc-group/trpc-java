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

package tests.service;

import com.tencent.trpc.core.rpc.RpcContext;
import com.tencent.trpc.core.rpc.anno.TRpcMethod;
import com.tencent.trpc.core.rpc.anno.TRpcService;
import tests.service.HelloRequestProtocol.HelloRequest;
import tests.service.HelloRequestProtocol.HelloResponse;

@TRpcService(name = "tencent.trpc.http.GreeterService")
public interface GreeterService {

    @TRpcMethod(name = "sayHello")
    HelloResponse sayHello(RpcContext context, HelloRequest request);

    @TRpcMethod(name = "sayBlankHello")
    String sayBlankHello(RpcContext context, HelloRequest request);

    @TRpcMethod(name = "sayHelloNonPbType")
    TestBeanConvertWithGetMethodRsp sayHelloNonPbType(RpcContext context, TestBeanConvertWithGetMethodReq request);
}
