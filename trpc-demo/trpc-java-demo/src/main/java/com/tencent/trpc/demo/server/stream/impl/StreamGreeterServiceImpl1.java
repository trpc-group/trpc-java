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

package com.tencent.trpc.demo.server.stream.impl;

import com.google.protobuf.TextFormat;
import com.tencent.trpc.core.rpc.RpcContext;
import com.tencent.trpc.demo.proto.HelloRequestProtocol;
import com.tencent.trpc.demo.proto.StreamGreeterServiceStreamAPI;
import java.time.Duration;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class StreamGreeterServiceImpl1 implements StreamGreeterServiceStreamAPI {

    @Override
    public Flux<HelloRequestProtocol.HelloResponse> serverSayHellos(RpcContext ctx,
            HelloRequestProtocol.HelloRequest request) {
        System.out.println(">>>>>>serverSayHellos got request: " + TextFormat.shortDebugString(request));
        return Flux.interval(Duration.ofMillis(100))
                .take(100)
                .map(id -> HelloRequestProtocol.HelloResponse.newBuilder()
                        .setMessage("sequence <<<" + id + ">>>, server say hello to " + request.getMessage())
                        .build()
                );
    }

    @Override
    public Mono<HelloRequestProtocol.HelloResponse> clientSayHellos(RpcContext ctx,
            Publisher<HelloRequestProtocol.HelloRequest> requests) {
        return Flux.from(requests)
                .doOnNext(request ->
                        System.out.println(">>>>>>sayHello got request: " + TextFormat.shortDebugString(request))
                )
                .count()
                .map(cnt -> HelloRequestProtocol.HelloResponse.newBuilder()
                        .setMessage("client said times: " + cnt)
                        .build()
                );
    }

    @Override
    public Flux<HelloRequestProtocol.HelloResponse> allSayHellos(RpcContext ctx,
            Publisher<HelloRequestProtocol.HelloRequest> requests) {
        return Flux.from(requests)
                .map(request -> {
                    System.out.println(">>>>>>sayHello got request: " + TextFormat.shortDebugString(request));

                    return HelloRequestProtocol.HelloResponse.newBuilder()
                            .setMessage("say hello back to " + request.getMessage())
                            .build();
                });
    }

}
