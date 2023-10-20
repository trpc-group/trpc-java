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

package com.tencent.trpc.proto.standard.stream.server.impl;

import com.google.common.collect.Maps;
import com.google.protobuf.TextFormat;
import com.tencent.trpc.core.exception.TRpcException;
import com.tencent.trpc.core.rpc.RpcContext;
import com.tencent.trpc.core.utils.RpcContextUtils;
import com.tencent.trpc.core.worker.WorkerPoolManager;
import com.tencent.trpc.core.worker.spi.WorkerPool;
import com.tencent.trpc.proto.standard.stream.common.HelloRequestProtocol.HelloRequest;
import com.tencent.trpc.proto.standard.stream.common.HelloRequestProtocol.HelloResponse;
import com.tencent.trpc.proto.standard.stream.server.StreamGreeterService;
import org.apache.commons.lang3.StringUtils;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.time.Duration;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

public class StreamGreeterServiceImpl1 implements StreamGreeterService {

    public static final int LEN_10K = 10 * 1024;

    public static final String TEST_FLAG = "flag";
    public static final String TEST_FLAG_NORMAL = "normal";
    public static final String TEST_FLAG_NOBIND = "nobind";
    public static final String TEST_FLAG_OVERFLOW = "overflow";
    public static final String TEST_FLAG_EXCEPTION = "exception";

    private static final int ERR_CODE_TEST = 101; // customized error code for test

    private final ConcurrentMap<String, AtomicInteger> stats = Maps.newConcurrentMap();
    private final ConcurrentMap<String, CountDownLatch> latches = Maps.newConcurrentMap();

    @Override
    public Flux<HelloResponse> serverSayHellos(RpcContext ctx, HelloRequest request) {
        if (request.getMessage().equals("exception")) {
            throw new RuntimeException("exception for test");
        }

        System.out.println(">>>>>>serverSayHellos got request: " + TextFormat.shortDebugString(request));
        return Flux.interval(Duration.ofMillis(100))
                .take(30)
                .map(id -> HelloResponse.newBuilder()
                        .setMessage("sequence <<<" + id + ">>>, server says: " + request.getMessage())
                        .build()
                );
    }

    @Override
    public Mono<HelloResponse> clientSayHellos(RpcContext ctx, Publisher<HelloRequest> requests) {
        return Flux.from(requests)
                .doOnNext(request ->
                        System.out.println(">>>>>>sayHello got request: " + TextFormat.shortDebugString(request))
                )
                .count()
                .map(cnt -> HelloResponse.newBuilder()
                        .setMessage("client said times: " + cnt)
                        .build()
                );
    }

    @Override
    public Flux<HelloResponse> allSayHellos(RpcContext ctx, Publisher<HelloRequest> requests) {
        String flag = RpcContextUtils.getRequestAttachValue(ctx, TEST_FLAG);
        flag = StringUtils.isEmpty(flag) ? TEST_FLAG_NORMAL : flag;
        AtomicInteger stat = stats.computeIfAbsent(flag, key -> new AtomicInteger());

        if (StringUtils.equals(flag, TEST_FLAG_NOBIND)) {
            String currentThreadName = Thread.currentThread().getName();
            System.out.println(currentThreadName + ">>>>no bind test called");
            CountDownLatch latch = new CountDownLatch(1);
            latches.put(flag, latch);

            Sinks.Many<HelloResponse> responseFlux = Sinks.many().unicast().onBackpressureBuffer();
            WorkerPool workerPool = WorkerPoolManager.get(WorkerPoolManager.DEF_PROVIDER_WORKER_POOL_NAME);
            workerPool.execute(() -> Flux.from(requests)
                    .doOnError(Throwable::printStackTrace)
                    .delayElements(Duration.ofMillis(20))
                    .doOnNext(request -> {
                        stat.incrementAndGet();
                        System.out.println(Thread.currentThread().getName() + ">>>>no bind got request: " + request);
                    })
                    .doFinally(s -> {
                        latch.countDown();
                        // requests and responses are not bound together, but responses are notified complete after
                        // the requests are all consumed in case of client close connection prematurely just after
                        // it has all sended.
                        responseFlux.emitComplete(Sinks.EmitFailureHandler.FAIL_FAST);
                    })
                    .subscribe());

            responseFlux.emitNext(HelloResponse.newBuilder().setMessage(TEST_FLAG_NOBIND).build(),
                    Sinks.EmitFailureHandler.FAIL_FAST);
            return responseFlux.asFlux();
        } else if (StringUtils.equals(flag, TEST_FLAG_OVERFLOW)) {
            return Flux.from(requests)
                    .doOnError(Throwable::printStackTrace)
                    .map(request -> {
                        stat.incrementAndGet();
                        String message = request.getMessage();
                        System.out.println(Thread.currentThread().getName() + ">>>>got request "
                                + StringUtils.substring(message, 0, 10)
                                + ", len: " + message.length());

                        return HelloResponse.newBuilder().setMessage(message).build();
                    });
        } else if (StringUtils.equals(flag, TEST_FLAG_EXCEPTION)) {
            return Flux.from(requests)
                    .doOnError(t -> {
                        stat.incrementAndGet();
                        t.printStackTrace();
                        System.out.println(">>>>got exception: " + t);
                    })
                    .map(request -> HelloResponse.newBuilder().setMessage(request.getMessage()).build());
        }

        return Flux.from(requests)
                .map(request -> {
                    stat.incrementAndGet();
                    System.out.println(">>>>got request: " + request);

                    return HelloResponse.newBuilder().setMessage(request.getMessage()).build();
                });
    }

    @Override
    public Flux<HelloResponse> timeoutSayHellos(RpcContext ctx, Publisher<HelloRequest> requests) {
        try {
            Thread.sleep(1000);
        } catch (Exception e) {
            System.out.println("sleep failed: " + e.getMessage());
        }

        return allSayHellos(ctx, requests);
    }

    @Override
    public Flux<HelloResponse> throwExceptionSayHellos(RpcContext ctx, Publisher<HelloRequest> requests) {
        throw TRpcException.newBizException(ERR_CODE_TEST, "test exception");
    }

    public void reset() {
        stats.clear();
        latches.clear();
    }

    public int getStat(String key) {
        AtomicInteger stat = stats.get(key);
        return stat != null ? stat.get() : 0;
    }

    public CountDownLatch getLatch(String key) {
        return latches.get(key);
    }

}
