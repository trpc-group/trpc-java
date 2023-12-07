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

package com.tencent.trpc.proto.standard.stream;

import com.tencent.trpc.core.common.ConfigManager;
import com.tencent.trpc.core.common.config.BackendConfig;
import com.tencent.trpc.core.common.config.ConsumerConfig;
import com.tencent.trpc.core.common.config.ProviderConfig;
import com.tencent.trpc.core.common.config.ServiceConfig;
import com.tencent.trpc.core.compressor.support.SnappyCompressor;
import com.tencent.trpc.core.exception.TRpcException;
import com.tencent.trpc.core.rpc.RpcClientContext;
import com.tencent.trpc.core.rpc.RpcContext;
import com.tencent.trpc.core.utils.NetUtils;
import com.tencent.trpc.core.utils.RpcContextUtils;
import com.tencent.trpc.proto.standard.common.GreeterServiceImp;
import com.tencent.trpc.proto.standard.stream.common.HelloRequestProtocol.HelloRequest;
import com.tencent.trpc.proto.standard.stream.common.HelloRequestProtocol.HelloResponse;
import com.tencent.trpc.proto.standard.stream.config.TRpcStreamConstants;
import com.tencent.trpc.proto.standard.stream.server.StreamGreeterService;
import com.tencent.trpc.proto.standard.stream.server.StreamGreeterService2;
import com.tencent.trpc.proto.standard.stream.server.StreamGreeterService4;
import com.tencent.trpc.proto.standard.stream.server.impl.StreamGreeterServiceImpl1;
import com.tencent.trpc.proto.standard.stream.server.impl.StreamGreeterServiceImpl3;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.reactivestreams.Subscription;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class StreamTests {

    ServiceConfig serviceConfig;
    StreamGreeterServiceImpl1 streamGreeterService = new StreamGreeterServiceImpl1();

    @Before
    public void before() {
        ConfigManager.stopTest();
        ConfigManager.startTest();
        startServer();
        this.streamGreeterService.reset();
    }

    @After
    public void stop() {
        ConfigManager.stopTest();
        if (serviceConfig != null) {
            serviceConfig.unExport();
            serviceConfig = null;
        }
    }

    @Test
    public void testDuplexStream() throws InterruptedException {
        StreamGreeterService proxy = getServiceProxy();

        int times = 2;
        int batch = 30;

        final AtomicInteger count = new AtomicInteger();
        // duplex stream test
        for (int i = 0; i < times; i++) {
            CountDownLatch countDownLatch = new CountDownLatch(2);

            Flux<HelloResponse> responseFlux = proxy.allSayHellos(new RpcClientContext(),
                    Flux.interval(Duration.ofMillis(100))
                            .take(batch)
                            .map(id -> HelloRequest.newBuilder().setMessage("duplex hello1-" + id)
                                    .build()));
            responseFlux
                    .doOnError(t -> System.out.println("response has error: " + t.getMessage()))
                    .doOnNext(resp -> {
                        System.out.println(">>>[client1]receive msg:" + resp.getMessage());
                        count.incrementAndGet();
                    })
                    .doFinally(s -> countDownLatch.countDown())
                    .subscribe();

            Flux<HelloResponse> responseFlux2 = proxy.allSayHellos(new RpcClientContext(),
                    Flux.interval(Duration.ofMillis(100))
                            .take(batch)
                            .map(id -> HelloRequest.newBuilder().setMessage("duplex hello2-" + id)
                                    .build()));
            responseFlux2
                    .doOnError(t -> System.out.println("response has error: " + t.getMessage()))
                    .doOnNext(resp -> {
                        System.out.println(">>>[client2]receive msg:" + resp.getMessage());
                        count.incrementAndGet();
                    })
                    .doFinally(s -> countDownLatch.countDown())
                    .subscribe();

            countDownLatch.await(1, TimeUnit.MINUTES);
        }

        Assert.assertEquals(2 * times * batch, count.get());
    }

    @Test
    public void testClientStream() throws InterruptedException {
        StreamGreeterService proxy = getServiceProxy();

        int times = 2;
        int batch = 30;

        final AtomicInteger count = new AtomicInteger();
        // client stream test
        for (int i = 0; i < times; i++) {
            CountDownLatch countDownLatch = new CountDownLatch(2);

            Mono<HelloResponse> responseMono = proxy.clientSayHellos(new RpcClientContext(),
                    Flux.interval(Duration.ofMillis(100))
                            .take(batch)
                            .map(id -> HelloRequest.newBuilder().setMessage("hello1-" + id)
                                    .build()));
            responseMono
                    .doOnError(t -> System.out.println("response has error: " + t.getMessage()))
                    .doOnNext(resp -> {
                        System.out.println(">>>[client1]receive msg:" + resp.getMessage());
                        count.incrementAndGet();
                    })
                    .doFinally(s -> countDownLatch.countDown())
                    .subscribe();

            Mono<HelloResponse> responseMono2 = proxy.clientSayHellos(new RpcClientContext(),
                    Flux.interval(Duration.ofMillis(100))
                            .take(batch)
                            .map(id -> HelloRequest.newBuilder().setMessage("hello2-" + id)
                                    .build()));
            responseMono2
                    .doOnError(t -> System.out.println("response has error: " + t.getMessage()))
                    .doOnNext(resp -> {
                        System.out.println(">>>[client2]receive msg:" + resp.getMessage());
                        count.incrementAndGet();
                    })
                    .doFinally(s -> countDownLatch.countDown())
                    .subscribe();

            countDownLatch.await(1, TimeUnit.MINUTES);
        }

        Assert.assertEquals(2 * times, count.get());
    }

    @Test
    public void testServerStream() throws InterruptedException {
        StreamGreeterService proxy = getServiceProxy();
        HelloRequest helloRequest = HelloRequest.newBuilder().setMessage("hello").build();

        int times = 2;
        int batch = 30;

        final AtomicInteger count = new AtomicInteger();
        // server stream test
        for (int i = 0; i < times; i++) {
            CountDownLatch countDownLatch = new CountDownLatch(2);

            Flux<HelloResponse> responseFlux = proxy
                    .serverSayHellos(new RpcClientContext(), helloRequest);
            responseFlux
                    .doOnError(t -> System.out.println("response has error: " + t.getMessage()))
                    .doOnNext(resp -> {
                        System.out.println(">>>[client1]receive msg:" + resp.getMessage());
                        count.incrementAndGet();
                    })
                    .doFinally(s -> countDownLatch.countDown())
                    .subscribe();

            Flux<HelloResponse> responseFlux2 = proxy
                    .serverSayHellos(new RpcClientContext(), helloRequest);
            responseFlux2
                    .doOnError(t -> System.out.println("response has error: " + t.getMessage()))
                    .doOnNext(resp -> {
                        System.out.println(">>>[client2]receive msg:" + resp.getMessage());
                        count.incrementAndGet();
                    })
                    .doFinally(s -> countDownLatch.countDown())
                    .subscribe();

            countDownLatch.await(1, TimeUnit.MINUTES);
        }
        Assert.assertEquals(2 * times * batch, count.get());

        final AtomicInteger failedCount = new AtomicInteger();
        Flux<HelloResponse> resp = proxy
                .serverSayHellos(new RpcClientContext(), HelloRequest.newBuilder().setMessage("exception").build());
        resp
                .doOnNext(rsp -> Assert.fail("should not got response " + resp))
                .doOnError(t -> {
                    Assert.assertNotNull("exception is null", t);
                    System.out.println("got expected exception: " + t.getMessage());
                    failedCount.incrementAndGet();
                })
                .then(Mono.<HelloResponse>never())
                .onErrorReturn(HelloResponse.newBuilder().build())
                .block();
        Assert.assertEquals(1, failedCount.get());
    }

    @Test
    public void testStreamBuildTimeout() throws Exception {
        StreamGreeterService proxy = getServiceProxy();
        HelloRequest helloRequest = HelloRequest.newBuilder().setMessage("timeout").build();

        final AtomicInteger rspCount = new AtomicInteger();
        final AtomicInteger errCount = new AtomicInteger();

        int times = 100;
        int timeout = 200; // ms, only used to test the stream build timeout
        CountDownLatch latch = new CountDownLatch(times);

        for (int i = 0; i < times; i++) {
            RpcClientContext ctx = new RpcClientContext();
            ctx.setTimeoutMills(timeout);

            proxy
                    .timeoutSayHellos(ctx, Mono.just(helloRequest))
                    .doFinally(signal -> latch.countDown())
                    .subscribe(rsp -> {
                        System.out.println("got unexpected resp: " + rsp.toString());
                        rspCount.incrementAndGet();
                    }, t -> {
                        System.out.println("got expected exception: " + t.toString());
                        errCount.incrementAndGet();
                    });
        }

        latch.await(1, TimeUnit.MINUTES);

        Assert.assertEquals(0, rspCount.get());
        Assert.assertEquals(times, errCount.get());
    }

    @Test
    public void testRemoteNotExist() throws Exception {
        StreamGreeterService2 proxy = getNotExistServiceProxy(serviceConfig.getPort());
        StreamGreeterService2 proxy2 = getNotExistServiceProxy(serviceConfig.getPort() + 1);
        HelloRequest helloRequest = HelloRequest.newBuilder().setMessage("hello").build();

        final AtomicInteger rspCount = new AtomicInteger();
        final AtomicInteger errCount = new AtomicInteger();

        CountDownLatch latch = new CountDownLatch(2);
        proxy
                .serverSayHellos(new RpcClientContext(), helloRequest)
                .doFinally(signal -> latch.countDown())
                .subscribe(rsp -> {
                    System.out.println("got unexpected resp: " + rsp.toString());
                    rspCount.incrementAndGet();
                }, t -> {
                    System.out.println("got expected exception: " + t.toString());
                    errCount.incrementAndGet();
                });

        proxy2
                .serverSayHellos(new RpcClientContext(), helloRequest)
                .doFinally(signal -> latch.countDown())
                .subscribe(rsp -> {
                    System.out.println("got unexpected resp: " + rsp.toString());
                    rspCount.incrementAndGet();
                }, t -> {
                    System.out.println("got expected exception: " + t.toString());
                    errCount.incrementAndGet();
                });

        latch.await(1, TimeUnit.MINUTES);

        Assert.assertEquals(0, rspCount.get());
        Assert.assertEquals(2, errCount.get());
    }

    @Test
    public void testCompressedMessage() throws Exception {
        StreamGreeterService proxy = getCompressedServiceProxy();
        HelloRequest helloRequest = HelloRequest.newBuilder().setMessage("hello").build();

        final AtomicInteger rspCount = new AtomicInteger();
        final AtomicInteger errCount = new AtomicInteger();

        int batch = 30;
        CountDownLatch latch = new CountDownLatch(1);
        proxy
                .serverSayHellos(new RpcClientContext(), helloRequest)
                .doFinally(signal -> latch.countDown())
                .subscribe(rsp -> {
                    System.out.println("got expected resp: " + rsp.toString());
                    rspCount.incrementAndGet();
                }, t -> {
                    System.out.println("got unexpected exception: " + t.toString());
                    errCount.incrementAndGet();
                });

        latch.await(1, TimeUnit.MINUTES);

        Assert.assertEquals(batch, rspCount.get());
        Assert.assertEquals(0, errCount.get());
    }

    @Test
    public void testNotValidStreamMethod() throws Exception {
        StreamGreeterService proxy = getServiceProxy();
        HelloRequest helloRequest = HelloRequest.newBuilder().setMessage("hello").build();

        int times = 100;
        CountDownLatch latch = new CountDownLatch(times);

        final AtomicInteger rspCount = new AtomicInteger();
        final AtomicInteger errCount = new AtomicInteger();

        for (int i = 0; i < times; i++) {
            RpcContext ctx = new RpcClientContext();
            proxy.throwExceptionSayHellos(ctx, Mono.just(helloRequest))
                    .doFinally(signal -> latch.countDown())
                    .subscribe(rsp -> {
                        System.out.println("got unexpected resp: " + rsp.toString());
                        rspCount.incrementAndGet();
                    }, t -> {
                        System.out.println("got expected exception: " + t.toString());
                        errCount.incrementAndGet();
                    });
        }

        latch.await(1, TimeUnit.MINUTES);

        Assert.assertEquals(0, rspCount.get());
        Assert.assertEquals(times, errCount.get());
    }

    @Test
    public void testMultiProtocolTypeService() {
        try {
            ServiceConfig serviceConfig = getTRpcServiceConfig(this.serviceConfig.getPort() + 1);

            ProviderConfig<?> providerConfig = new ProviderConfig<>();
            providerConfig.setRefClazz(StreamGreeterServiceImpl3.class.getCanonicalName());

            startServer(serviceConfig, Collections.singletonList(providerConfig));
            Assert.fail("do not support service with multi protocol types");
        } catch (Exception e) {
            e.printStackTrace();
            Assert.assertTrue(e instanceof IllegalArgumentException);
        }

        try {
            ServiceConfig serviceConfig = getTRpcServiceConfig(this.serviceConfig.getPort() + 2);

            ProviderConfig<?> providerConfig1 = new ProviderConfig<>();
            providerConfig1.setRefClazz(GreeterServiceImp.class.getCanonicalName()); // standard
            ProviderConfig<?> providerConfig2 = new ProviderConfig<>();
            providerConfig2.setRefClazz(StreamGreeterServiceImpl1.class.getCanonicalName()); // stream

            startServer(serviceConfig, Arrays.asList(providerConfig1, providerConfig2));
            Assert.fail("do not support services with different protocol types");
        } catch (Exception e) {
            e.printStackTrace();
            Assert.assertTrue(e instanceof IllegalArgumentException);
        }
    }

    @Test
    public void testMultiProtocolTypeClient() {
        try {
            BackendConfig backendConfig = new BackendConfig();
            backendConfig.setNamingUrl("ip://127.0.0.1:" + serviceConfig.getPort());
            ConsumerConfig<StreamGreeterService4> consumerConfig = new ConsumerConfig<>();
            consumerConfig.setServiceInterface(StreamGreeterService4.class);
            StreamGreeterService4 proxy = backendConfig.getProxy(consumerConfig);
            proxy.sayHello(new RpcClientContext(), HelloRequest.newBuilder().build());
            Assert.fail("do not support service with multi protocol types");
        } catch (Exception e) {
            e.printStackTrace();
            Assert.assertTrue(e instanceof TRpcException);
        }
    }

    @Test
    public void testNoBindDuplexStream() throws Exception {
        StreamGreeterService proxy = getServiceProxy();
        RpcClientContext context = new RpcClientContext();

        String flag = StreamGreeterServiceImpl1.TEST_FLAG_NOBIND;
        RpcContextUtils.putRequestAttachValue(context, StreamGreeterServiceImpl1.TEST_FLAG, flag);

        int count = 100;
        Flux<HelloResponse> responseFlux = proxy.allSayHellos(context, Flux.range(1, count).map(id ->
                HelloRequest.newBuilder().setMessage("id-" + id).build()));

        CountDownLatch connectedLatch = new CountDownLatch(1);
        CountDownLatch resultLatch = new CountDownLatch(1);
        AtomicReference<HelloResponse> responseHolder = new AtomicReference<>();
        AtomicInteger responseCount = new AtomicInteger();
        responseFlux
                .doOnError(Throwable::printStackTrace)
                .doOnNext(rsp -> {
                    responseCount.incrementAndGet();
                    if (responseHolder.compareAndSet(null, rsp)) {
                        connectedLatch.countDown();
                    }
                })
                .doFinally(s -> resultLatch.countDown())
                .subscribe();

        // waiting for the stream has connected
        connectedLatch.await(1, TimeUnit.MINUTES);
        HelloResponse helloResponse = responseHolder.get();
        System.out.println(Thread.currentThread().getName() + ">>>>got response: " + helloResponse);
        Assert.assertNotNull(helloResponse);
        Assert.assertEquals(flag, helloResponse.getMessage());

        // check the unbound server stream consumer stats
        CountDownLatch latch = streamGreeterService.getLatch(flag);
        Assert.assertNotNull(latch);
        latch.await(1, TimeUnit.MINUTES);
        int stat = streamGreeterService.getStat(flag);
        Assert.assertEquals(count, stat);

        // check the response stream stats
        resultLatch.await(1, TimeUnit.MINUTES);
        Assert.assertEquals(1, responseCount.get());
    }

    @Test
    public void testTransferError() throws Exception {
        StreamGreeterService proxy = getServiceProxy();
        RpcClientContext context = new RpcClientContext();

        String flag = StreamGreeterServiceImpl1.TEST_FLAG_EXCEPTION;
        RpcContextUtils.putRequestAttachValue(context, StreamGreeterServiceImpl1.TEST_FLAG, flag);

        RuntimeException error = new RuntimeException("testTransferError");
        Flux<HelloResponse> responseFlux = proxy.allSayHellos(context, Flux.error(error));

        CountDownLatch latch = new CountDownLatch(1);
        Throwable[] exHolder = new Throwable[1];
        responseFlux
                .doFinally(s -> latch.countDown())
                .subscribe(null, ex -> exHolder[0] = ex);

        latch.await(1, TimeUnit.MINUTES);
        Assert.assertNotNull(exHolder[0]);
        System.out.println(">>>>got transfered error: " + exHolder[0].getMessage());
        Assert.assertTrue(exHolder[0].getMessage().contains(error.toString()));
    }

    @Test
    public void testFlowControl() throws Throwable {
        StreamGreeterService proxy = getServiceProxy();
        RpcClientContext context = new RpcClientContext();

        String flag = StreamGreeterServiceImpl1.TEST_FLAG_OVERFLOW;
        RpcContextUtils.putRequestAttachValue(context, StreamGreeterServiceImpl1.TEST_FLAG, flag);

        int count = 100;
        String msg = RandomStringUtils.randomAlphanumeric(StreamGreeterServiceImpl1.LEN_10K);
        Flux<HelloResponse> responseFlux = proxy.allSayHellos(context,
                Flux.range(1, count).map(id -> {
                    System.out.println(Thread.currentThread().getName() + ">>>>send request: " + id);
                    return HelloRequest.newBuilder().setMessage(msg).build();
                }));

        CountDownLatch latch = new CountDownLatch(count);
        AtomicInteger counter = new AtomicInteger();
        int baseCount = TRpcStreamConstants.DEFAULT_STREAM_WINDOW_SIZE / StreamGreeterServiceImpl1.LEN_10K + 1;
        AtomicReference<Throwable> exception = new AtomicReference<>();

        responseFlux
                .subscribe(new BaseSubscriber<HelloResponse>() {
                    @Override
                    protected void hookOnSubscribe(Subscription subscription) {
                        subscription.request(1);
                    }

                    @Override
                    protected void hookOnNext(HelloResponse value) {
                        latch.countDown();
                        int c = counter.incrementAndGet();
                        int maxExpect = baseCount + 2 * (c / 2);
                        int actual = streamGreeterService.getStat(flag);
                        System.out.println(Thread.currentThread().getName() + ">>>>got response: "
                                + StringUtils.substring(value.getMessage(), 0, 10)
                                + ", len: " + value.getMessage().length()
                                + ", count: " + c
                                + ", maxExpect: " + maxExpect
                                + ", actual: " + actual);

                        try {
                            Assert.assertTrue("expect less than " + maxExpect + " got " + actual,
                                    actual <= maxExpect);
                        } catch (Throwable t) {
                            exception.compareAndSet(null, t);
                        }

                        // delay request
                        try {
                            Thread.sleep(20);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        request(1);
                    }
                });
        latch.await(1, TimeUnit.MINUTES);

        if (exception.get() != null) {
            throw exception.get();
        }
    }

    private StreamGreeterService getServiceProxy() {
        BackendConfig backendConfig = new BackendConfig();
        backendConfig.setNamingUrl("ip://127.0.0.1:" + serviceConfig.getPort());
        ConsumerConfig<StreamGreeterService> consumerConfig = new ConsumerConfig<>();
        consumerConfig.setServiceInterface(StreamGreeterService.class);
        return backendConfig.getProxy(consumerConfig);
    }

    private StreamGreeterService2 getNotExistServiceProxy(int port) {
        BackendConfig backendConfig = new BackendConfig();
        backendConfig.setNamingUrl("ip://127.0.0.1:" + port);
        ConsumerConfig<StreamGreeterService2> consumerConfig = new ConsumerConfig<>();
        consumerConfig.setServiceInterface(StreamGreeterService2.class);
        return backendConfig.getProxy(consumerConfig);
    }

    private StreamGreeterService getCompressedServiceProxy() {
        BackendConfig backendConfig = new BackendConfig();
        backendConfig.setNamingUrl("ip://127.0.0.1:" + serviceConfig.getPort());
        backendConfig.setCompressor(SnappyCompressor.NAME); // 压缩数据测试
        ConsumerConfig<StreamGreeterService> consumerConfig = new ConsumerConfig<>();
        consumerConfig.setServiceInterface(StreamGreeterService.class);
        return backendConfig.getProxy(consumerConfig);
    }

    private void startServer() {
        ProviderConfig<StreamGreeterService> providerConfig = new ProviderConfig<>();
        providerConfig.setRef(this.streamGreeterService);
        int port = NetUtils.getAvailablePort();
        ServiceConfig serviceConfig = getTRpcServiceConfig(port);
        startServer(serviceConfig, Collections.singletonList(providerConfig));
        this.serviceConfig = serviceConfig;
    }

    private void startServer(ServiceConfig serviceConfig, List<ProviderConfig<?>> providerConfigs) {
        serviceConfig.setRequestTimeout(10000000);
        providerConfigs.forEach(serviceConfig::addProviderConfig);
        serviceConfig.setCompressor(SnappyCompressor.NAME); // server side using compressed data during all these tests
        serviceConfig.export();
    }

    private ServiceConfig getTRpcServiceConfig(int port) {
        ServiceConfig serviceConfig = new ServiceConfig();
        serviceConfig.setIp("127.0.0.1");
        serviceConfig.setNetwork("tcp");
        serviceConfig.setPort(port);
        return serviceConfig;
    }

}
