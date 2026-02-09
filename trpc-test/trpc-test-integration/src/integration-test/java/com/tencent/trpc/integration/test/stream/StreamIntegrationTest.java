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

package com.tencent.trpc.integration.test.stream;

import com.tencent.trpc.core.exception.TRpcException;
import com.tencent.trpc.core.rpc.RpcClientContext;
import com.tencent.trpc.integration.test.TrpcServerApplication;
import com.tencent.trpc.integration.test.stub.EchoService.EchoRequest;
import com.tencent.trpc.integration.test.stub.EchoService.EchoResponse;
import com.tencent.trpc.integration.test.stub.StreamingEchoAPI;
import com.tencent.trpc.spring.annotation.TRpcClient;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Stream related integration tests. Related configuration file: <code>application-stream.yml</code>
 */
@ActiveProfiles("stream")
@SpringBootTest(classes = TrpcServerApplication.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
public class StreamIntegrationTest {

    private final ExecutorService executor = Executors.newCachedThreadPool();
    @TRpcClient(id = "stream-client")
    private StreamingEchoAPI api;

    /**
     * Test for client-side streaming call
     */
    @Test
    public void clientStreamTest() {
        int reqCount = 5; // send {reqCount} EchoRequests
        Mono<EchoResponse> responseMono = api.clientStreamEcho(new RpcClientContext(), buildStreamingRequest(reqCount));
        // expects a Mono containing count of requests send
        StepVerifier.create(responseMono)
                .expectNext(EchoResponse.newBuilder().setMessage(String.valueOf(reqCount)).build())
                .expectComplete()
                .verify(Duration.ofSeconds(10));
    }

    /**
     * Test for concurrent client-side streaming call
     */
    @Test
    public void clientStreamConcurrentTest() {
        // start {concurrency} clients, each sending {i} EchoRequests
        int concurrency = 5;
        List<Future<Duration>> futures = IntStream.range(0, concurrency)
                .mapToObj(i -> Pair.of(i, api.clientStreamEcho(new RpcClientContext(), buildStreamingRequest(i))))
                .map(pair -> executor.submit(() ->
                        StepVerifier.create(pair.getRight())
                                .expectNext(EchoResponse.newBuilder()
                                        .setMessage(String.valueOf(pair.getLeft()))
                                        .build())
                                .expectComplete()
                                .verify(Duration.ofSeconds(10))))
                .collect(Collectors.toList());
        futures.forEach(f -> {
            try {
                f.get();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * Test for client-side streaming error.
     * Emit an error, server should be able to receive and response to that error
     */
    @Test
    public void clientStreamErrorTest() {
        Mono<EchoResponse> responseMono = api.clientStreamEcho(new RpcClientContext(),
                // Flux containing 3 EchoRequests followed by an error
                Flux.fromStream(Stream.of("1", "2", "3"))
                        .map(s -> EchoRequest.newBuilder().setMessage(s).build())
                        .concatWith(Flux.error(new RuntimeException("client error"))));
        // expects a Mono indicates that 3 requests and the following error have been received
        StepVerifier.create(responseMono)
                .expectNext(EchoResponse.newBuilder().setMessage("3e").build())
                .expectComplete()
                .verify(Duration.ofSeconds(10));
    }

    /**
     * Test for server-side streaming call
     */
    @Test
    public void serverStreamTest() {
        // send a request indicates how many EchoResponses should be replied
        int respCount = 5;
        Flux<EchoResponse> responseFlux = api.serverStreamEcho(new RpcClientContext(), EchoRequest.newBuilder()
                .setMessage(String.valueOf(respCount))
                .build());
        // expects a Flux containing {respCount} EchoResponses
        StepVerifier.create(responseFlux)
                .expectNext(IntStream.range(0, respCount)
                        .mapToObj(i -> EchoResponse.newBuilder().setMessage(String.valueOf(i)).build())
                        .toArray(EchoResponse[]::new))
                .expectComplete()
                .verify(Duration.ofSeconds(10));
    }

    /**
     * Test for concurrent server-side streaming call
     */
    @Test
    public void serverStreamConcurrentTest() {
        // start {concurrency} clients, each requesting {i} EchoResponses
        int concurrency = 5;
        List<Future<Duration>> futures = IntStream.range(0, concurrency)
                .mapToObj(i -> Pair.of(i,
                        api.serverStreamEcho(new RpcClientContext(), EchoRequest.newBuilder()
                                .setMessage(String.valueOf(i))
                                .build())))
                .map(pair -> executor.submit(() ->
                        StepVerifier.create(pair.getRight())
                                .expectNext(IntStream.range(0, pair.getLeft())
                                        .mapToObj(i -> EchoResponse.newBuilder().setMessage(String.valueOf(i)).build())
                                        .toArray(EchoResponse[]::new))
                                .expectComplete()
                                .verify(Duration.ofSeconds(10))))
                .collect(Collectors.toList());
        futures.forEach(f -> {
            try {
                f.get();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * Test for server-side streaming error
     */
    @Test
    public void serverStreamErrorTest() {
        // request server to reply 2 EchoResponses and then an error
        Flux<EchoResponse> responseFlux = api.serverStreamEcho(new RpcClientContext(), EchoRequest.newBuilder()
                .setMessage("2e")
                .build());
        // expect 2 EchoResponses followed by an error
        StepVerifier.create(responseFlux)
                .expectNext(EchoResponse.newBuilder().setMessage("0").build())
                .expectNext(EchoResponse.newBuilder().setMessage("1").build())
                .expectErrorMatches(e ->
                        e instanceof TRpcException && "java.lang.RuntimeException: server error".equals(e.getMessage()))
                .verify(Duration.ofSeconds(10));
    }

    /**
     * Test for bi-direction streaming call
     */
    @Test
    public void mutualStreamTest() {
        // send {reqCount} EchoRequests
        int reqCount = 5;
        Flux<EchoResponse> responseFlux = api.mutualStreamEcho(new RpcClientContext(), buildStreamingRequest(5));
        // expect {reqCount} EchoResponses
        StepVerifier.create(responseFlux)
                .expectNext(IntStream.range(0, reqCount)
                        .mapToObj(i -> EchoResponse.newBuilder().setMessage(String.valueOf(i)).build())
                        .toArray(EchoResponse[]::new))
                .expectComplete()
                .verify(Duration.ofSeconds(10));
    }

    /**
     * Test for bi-direction streaming client side error
     */
    @Test
    public void mutualStreamClientErrorTest() {
        // send a EchoRequest followed by an error
        Flux<EchoResponse> responseFlux = api.mutualStreamEcho(new RpcClientContext(),
                Flux.just(EchoRequest.newBuilder().setMessage("1").build())
                        .concatWith(Flux.error(new RuntimeException("client error"))));
        // expects a Flux containing 2 EchoResponse
        // first EchoResponse should be a normal response
        // second EchoResponse should indicate a client-side error
        StepVerifier.create(responseFlux)
                .expectNext(EchoResponse.newBuilder().setMessage("1").build())
                .expectNext(EchoResponse.newBuilder().setMessage("e").build())
                .expectComplete()
                .verify(Duration.ofSeconds(10));
    }

    /**
     * Test for bi-direction streaming server side error
     */
    @Test
    public void mutualStreamServerErrorTest() {
        // request server to reply an EchoResponse and then an error
        Flux<EchoResponse> responseFlux = api.mutualStreamEcho(new RpcClientContext(),
                Flux.just(EchoRequest.newBuilder().setMessage("1").build(),
                        EchoRequest.newBuilder().setMessage("e").build()));
        // expect an EchoResponse followed by an error
        StepVerifier.create(responseFlux)
                .expectNext(EchoResponse.newBuilder().setMessage("1").build())
                .expectErrorMatches(e ->
                        e instanceof TRpcException && "java.lang.RuntimeException: server error".equals(e.getMessage()))
                .verify(Duration.ofSeconds(10));
    }

    /**
     * Test for concurrent bi-direction streaming call
     */
    @Test
    public void mutualStreamConcurrentTest() {
        int concurrency = 5;
        List<Future<Duration>> futures = IntStream.range(0, concurrency)
                .mapToObj(i -> Pair.of(i,
                        api.mutualStreamEcho(new RpcClientContext(), buildStreamingRequest(i))))
                .map(pair -> executor.submit(() ->
                        StepVerifier.create(pair.getRight())
                                .expectNext(IntStream.range(0, pair.getLeft())
                                        .mapToObj(i -> EchoResponse.newBuilder().setMessage(String.valueOf(i)).build())
                                        .toArray(EchoResponse[]::new))
                                .expectComplete()
                                .verify(Duration.ofSeconds(10))))
                .collect(Collectors.toList());
        futures.forEach(f -> {
            try {
                f.get();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    private Flux<EchoRequest> buildStreamingRequest(int count) {
        return Flux.interval(Duration.ofMillis(100))
                .take(count)
                .map(id -> EchoRequest.newBuilder().setMessage(String.valueOf(id)).build());
    }
}
