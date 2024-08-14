package com.tencent.trpc.demo.api;

import com.tencent.trpc.core.rpc.RpcContext;
import com.tencent.trpc.core.rpc.anno.TRpcMethod;
import com.tencent.trpc.core.rpc.anno.TRpcService;

@TRpcService(name = "trpc.test.demo.Hello")
public interface HelloAPI {

    @TRpcMethod(name = "SayHello")
    String sayHello(RpcContext context, String request);
}