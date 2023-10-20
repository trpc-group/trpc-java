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

package com.tencent.trpc.proto.standard.stream.server;

import com.tencent.trpc.core.rpc.RpcContext;
import com.tencent.trpc.core.rpc.anno.TRpcMethod;
import com.tencent.trpc.core.rpc.anno.TRpcService;
import com.tencent.trpc.proto.standard.stream.common.HelloRequestProtocol.HelloRequest;
import com.tencent.trpc.proto.standard.stream.common.HelloRequestProtocol.HelloResponse;
import java.util.concurrent.CompletionStage;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

// Exceptional test only interface which has stream and standard functions, which is not supported.
@TRpcService(name = "trpc.TestApp.TestServer.Greeter3")
public interface StreamGreeterService4 {

    // standard function
    @TRpcMethod(name = "sayHello")
    CompletionStage<HelloResponse> sayHello(RpcContext context, HelloRequest request);

    // server stream
    @TRpcMethod(name = "serverSayHellos")
    Flux<HelloResponse> serverSayHellos(RpcContext ctx, HelloRequest request);

    // client stream
    @TRpcMethod(name = "clientSayHellos")
    Mono<HelloResponse> clientSayHellos(RpcContext ctx, Publisher<HelloRequest> requests);

    // duplex stream
    @TRpcMethod(name = "allSayHellos")
    Flux<HelloResponse> allSayHellos(RpcContext ctx, Publisher<HelloRequest> requests);

}
