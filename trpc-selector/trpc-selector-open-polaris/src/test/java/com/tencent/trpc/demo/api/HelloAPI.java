package com.tencent.trpc.demo.api;

import com.tencent.trpc.core.rpc.RpcContext;
import com.tencent.trpc.core.rpc.anno.TRpcMethod;
import com.tencent.trpc.core.rpc.anno.TRpcService;
import com.tencent.trpc.demo.api.Hello.HelloReq;
import com.tencent.trpc.demo.api.Hello.HelloRsp;

@TRpcService(name = "trpc.test.demo.Hello")
public interface HelloAPI {
    
    @TRpcMethod(name = "SayHello")
    HelloRsp sayHello(RpcContext context, HelloReq request);
}