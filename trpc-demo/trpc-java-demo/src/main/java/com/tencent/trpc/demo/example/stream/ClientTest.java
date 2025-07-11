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

package com.tencent.trpc.demo.example.stream;

import com.tencent.trpc.core.common.ConfigManager;
import com.tencent.trpc.core.common.config.BackendConfig;
import com.tencent.trpc.core.common.config.ConsumerConfig;
import com.tencent.trpc.core.rpc.RpcClientContext;
import com.tencent.trpc.core.rpc.RpcContext;
import com.tencent.trpc.demo.proto.HelloRequestProtocol;
import com.tencent.trpc.demo.proto.StreamGreeterServiceStreamAPI;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class ClientTest {

    private static final int TCP_PORT = 12321;

    public static void main(String[] args) throws Exception {
        try {
            // start global config manager
            ConfigManager.getInstance().start();

            // setup remote service interface
            ConsumerConfig<StreamGreeterServiceStreamAPI> consumerConfig = new ConsumerConfig<>();
            consumerConfig.setServiceInterface(StreamGreeterServiceStreamAPI.class);

            // setup trpc service backend
            BackendConfig backendConfig = new BackendConfig();
            backendConfig.setNamingUrl("ip://127.0.0.1:" + TCP_PORT);

            // create trpc service proxy
            StreamGreeterServiceStreamAPI proxy = backendConfig.getProxy(consumerConfig);

            HelloRequestProtocol.HelloRequest helloRequest = HelloRequestProtocol.HelloRequest.newBuilder()
                    .setMessage("tRPC-Java").build();

            // =================================================================================
            // Following are demos of server, client, and duplex stream calls. Please start the
            // stream.ServerTest first before running the next demos.
            // =================================================================================

            int times = 1;
            RpcContext context = new RpcClientContext();

            // server stream demo
            for (int i = 0; i < times; i++) {
                CountDownLatch countDownLatch = new CountDownLatch(2);

                Flux<HelloRequestProtocol.HelloResponse> responseFlux = proxy
                        .serverSayHellos(context, helloRequest);
                responseFlux
                        .doOnError(t -> System.out.println("response has error: " + t.getMessage()))
                        .doOnNext(resp -> System.out
                                .println(">>>[Client1] receive msg: " + resp.getMessage()))
                        .doFinally(s -> countDownLatch.countDown())
                        .subscribe();

                Flux<HelloRequestProtocol.HelloResponse> responseFlux2 = proxy
                        .serverSayHellos(context, helloRequest);
                responseFlux2
                        .doOnError(t -> System.out.println("response has error: " + t.getMessage()))
                        .doOnNext(resp -> System.out
                                .println(">>>[Client2] receive msg: " + resp.getMessage()))
                        .doFinally(s -> countDownLatch.countDown())
                        .subscribe();

                countDownLatch.await();
                TimeUnit.SECONDS.sleep(1);
            }

            // client stream demo
            for (int i = 0; i < times; i++) {
                CountDownLatch countDownLatch = new CountDownLatch(2);

                Mono<HelloRequestProtocol.HelloResponse> responseMono = proxy.clientSayHellos(context,
                        Flux.interval(Duration.ofMillis(100))
                                .take(100)
                                .map(id -> HelloRequestProtocol.HelloRequest.newBuilder()
                                        .setMessage("[Client1] tRPC-Java " + id)
                                        .build()));
                responseMono
                        .doOnError(t -> System.out.println("response has error: " + t.getMessage()))
                        .doOnNext(resp -> System.out
                                .println(">>>[Client1] receive msg: " + resp.getMessage()))
                        .doFinally(s -> countDownLatch.countDown())
                        .subscribe();

                Mono<HelloRequestProtocol.HelloResponse> responseMono2 = proxy.clientSayHellos(context,
                        Flux.interval(Duration.ofMillis(100))
                                .take(100)
                                .map(id -> HelloRequestProtocol.HelloRequest.newBuilder()
                                        .setMessage("[Client2] tRPC-Java " + id)
                                        .build()));
                responseMono2
                        .doOnError(t -> System.out.println("response has error: " + t.getMessage()))
                        .doOnNext(resp -> System.out
                                .println(">>>[Client2] receive msg: " + resp.getMessage()))
                        .doFinally(s -> countDownLatch.countDown())
                        .subscribe();

                countDownLatch.await();
                TimeUnit.SECONDS.sleep(1);
            }

            // duplex stream demo
            for (int i = 0; i < times; i++) {
                CountDownLatch countDownLatch = new CountDownLatch(2);

                Flux<HelloRequestProtocol.HelloResponse> responseFlux = proxy.allSayHellos(context,
                        Flux.interval(Duration.ofMillis(100))
                                .take(100)
                                .map(id -> HelloRequestProtocol.HelloRequest.newBuilder()
                                        .setMessage("[Client1] tRPC-Java " + id)
                                        .build()));
                responseFlux
                        .doOnError(t -> System.out.println("response has error: " + t.getMessage()))
                        .doOnNext(resp -> System.out
                                .println(">>>[Client1] receive msg: " + resp.getMessage()))
                        .doFinally(s -> countDownLatch.countDown())
                        .subscribe();

                Flux<HelloRequestProtocol.HelloResponse> responseFlux2 = proxy.allSayHellos(context,
                        Flux.interval(Duration.ofMillis(100))
                                .take(100)
                                .map(id -> HelloRequestProtocol.HelloRequest.newBuilder()
                                        .setMessage("[Client2] tRPC-Java " + id)
                                        .build()));
                responseFlux2
                        .doOnError(t -> System.out.println("response has error: " + t.getMessage()))
                        .doOnNext(resp -> System.out
                                .println(">>>[Client2] receive msg: " + resp.getMessage()))
                        .doFinally(s -> countDownLatch.countDown())
                        .subscribe();

                countDownLatch.await();
                TimeUnit.SECONDS.sleep(1);
            }
        } finally {
            ConfigManager.getInstance().stop();
        }
    }
}
