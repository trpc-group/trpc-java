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

package com.tencent.trpc.proto.standard.stream.server.impl;

import com.tencent.trpc.core.rpc.RpcContext;
import com.tencent.trpc.proto.standard.stream.common.HelloRequestProtocol.HelloRequest;
import com.tencent.trpc.proto.standard.stream.common.HelloRequestProtocol.HelloResponse;
import com.tencent.trpc.proto.standard.stream.server.StreamGreeterService3;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class StreamGreeterServiceImpl3 implements StreamGreeterService3 {

    @Override
    public HelloResponse sayHello(RpcContext context, HelloRequest request) {
        throw new UnsupportedOperationException("shouldn't call this method");
    }

    @Override
    public Flux<HelloResponse> serverSayHellos(RpcContext ctx, HelloRequest request) {
        throw new UnsupportedOperationException("shouldn't call this method");
    }

    @Override
    public Mono<HelloResponse> clientSayHellos(RpcContext ctx, Publisher<HelloRequest> requests) {
        throw new UnsupportedOperationException("shouldn't call this method");
    }

    @Override
    public Flux<HelloResponse> allSayHellos(RpcContext ctx, Publisher<HelloRequest> requests) {
        throw new UnsupportedOperationException("shouldn't call this method");
    }
}
