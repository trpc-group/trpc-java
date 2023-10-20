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

package com.tencent.trpc.integration.test.stub;

import com.tencent.trpc.core.rpc.RpcContext;
import com.tencent.trpc.integration.test.stub.EchoService.EchoRequest;
import com.tencent.trpc.integration.test.stub.EchoService.EchoResponse;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class StreamingEchoAPIImpl implements StreamingEchoAPI {

    private static final Logger LOGGER = LoggerFactory.getLogger(StreamingEchoAPIImpl.class);

    @Override
    public Mono<EchoResponse> clientStreamEcho(RpcContext context, Publisher<EchoRequest> request) {
        AtomicInteger counter = new AtomicInteger();
        AtomicBoolean error = new AtomicBoolean();
        return Flux.from(request)
                .doOnNext(req -> {
                    LOGGER.info("got req {}", req.getMessage());
                    counter.incrementAndGet();
                })
                .count()
                .doOnError(e -> error.set(true))
                .map(count -> EchoResponse.newBuilder()
                        .setMessage(count + (error.get() ? "e" : ""))
                        .build());
    }

    @Override
    public Flux<EchoResponse> serverStreamEcho(RpcContext context, EchoRequest request) {
        boolean error = false;
        int count;
        if (request.getMessage().endsWith("e")) {
            error = true;
            count = Integer.parseInt(request.getMessage().substring(0, request.getMessage().length() - 1));
        } else {
            count = Integer.parseInt(request.getMessage());
        }
        Flux<EchoResponse> responseFlux = Flux.interval(Duration.ofMillis(100))
                .take(count)
                .map(i -> EchoResponse.newBuilder().setMessage(String.valueOf(i)).build());
        if (error) {
            return responseFlux.concatWith(Flux.error(new RuntimeException("server error")));
        } else {
            return responseFlux;
        }
    }

    @Override
    public Flux<EchoResponse> mutualStreamEcho(RpcContext context, Publisher<EchoRequest> request) {
        return Flux.from(request)
                .onErrorReturn(EchoRequest.newBuilder().setMessage("e").build())
                .map(req -> {
                    LOGGER.info("got req {}", req.getMessage());
                    if ("e".equals(req.getMessage())) {
                        throw new RuntimeException("server error");
                    }
                    return EchoResponse.newBuilder().setMessage(req.getMessage()).build();
                });
    }
}
