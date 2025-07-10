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

package com.tencent.trpc.core.stream;

import com.tencent.trpc.core.rpc.RpcClientContext;
import com.tencent.trpc.core.rpc.RpcContext;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.powermock.api.mockito.PowerMockito;
import org.reactivestreams.Publisher;
import reactor.core.CoreSubscriber;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class StreamCallTest {

    private StreamCall streamCall;

    private Flux<?> receiverFlux;

    /**
     * Init streamCall & receiverFlux
     */
    @Before
    public void setUp() {
        streamCall = PowerMockito.spy(new StreamCall() {

            @Override
            public <ReqT, RspT> Flux<RspT> serverStream(RpcContext ctx, ReqT request) {
                return StreamCall.super.serverStream(ctx, request);
            }

            @Override
            public <ReqT, RspT> Mono<RspT> clientStream(RpcContext ctx, Publisher<ReqT> requests) {
                return StreamCall.super.clientStream(ctx, requests);
            }

            @Override
            public <ReqT, RspT> Flux<RspT> duplexStream(RpcContext ctx, Publisher<ReqT> requests) {
                return StreamCall.super.duplexStream(ctx, requests);
            }

            @Override
            public Mono<Void> onClose() {
                return StreamCall.super.onClose();
            }

            @Override
            public void dispose() {
                StreamCall.super.dispose();
            }

            @Override
            public boolean isDisposed() {
                return StreamCall.super.isDisposed();
            }
        });

        receiverFlux = new Flux<Object>() {
            @Override
            public void subscribe(CoreSubscriber<? super Object> coreSubscriber) {

            }
        };
    }

    @Test
    public void testServerStream() {
        RpcContext ctx = new RpcClientContext();
        Publisher<?> resp = streamCall.serverStream(ctx, receiverFlux);
        Assert.assertNotNull(resp);
    }

    @Test
    public void testClientStream() {
        RpcContext ctx = new RpcClientContext();
        Publisher<?> resp = streamCall.clientStream(ctx, receiverFlux);
        Assert.assertNotNull(resp);
    }

    @Test
    public void testDuplexStream() {
        RpcContext ctx = new RpcClientContext();
        Publisher<?> resp = streamCall.duplexStream(ctx, receiverFlux);
        Assert.assertNotNull(resp);
    }

    @Test
    public void testOnClose() {
        Mono<Void> resp = streamCall.onClose();
        Assert.assertNotNull(resp);
    }

    @Test
    public void testDispose() {
        streamCall.dispose();
    }

    @Test
    public void testIsDisposed() {
        Assert.assertFalse(streamCall.isDisposed());
    }

}
