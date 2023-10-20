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

import com.tencent.trpc.core.rpc.InvokeMode;
import com.tencent.trpc.core.rpc.RpcContext;
import com.tencent.trpc.core.stream.StreamCall;
import com.tencent.trpc.core.utils.RpcUtils;
import java.lang.reflect.Method;
import java.util.Objects;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Encapsulates method call information.
 */
@SuppressWarnings("unchecked")
public class StreamServiceInvoker implements StreamCall {

    /**
     * Service instance.
     */
    final Object service;
    /**
     * Service method.
     */
    final Method method;
    /**
     * Method type.
     */
    final InvokeMode invokeMode;

    public StreamServiceInvoker(Object service, Method method) {
        this.service = Objects.requireNonNull(service, "service is null");
        this.method = Objects.requireNonNull(method, "method is null");
        this.invokeMode = RpcUtils.parseInvokeMode(method);
    }

    @Override
    public <ReqT, RspT> Flux<RspT> serverStream(RpcContext ctx, ReqT request) {
        try {
            checkInvokeMode(InvokeMode.SERVER_STREAM);
            return (Flux<RspT>) method.invoke(service, ctx, request);
        } catch (Throwable t) {
            return Flux.error(t);
        }
    }

    @Override
    public <ReqT, RspT> Mono<RspT> clientStream(RpcContext ctx, Publisher<ReqT> requests) {
        try {
            checkInvokeMode(InvokeMode.CLIENT_STREAM);
            return (Mono<RspT>) method.invoke(service, ctx, requests);
        } catch (Throwable t) {
            return Mono.error(t);
        }
    }

    @Override
    public <ReqT, RspT> Flux<RspT> duplexStream(RpcContext ctx, Publisher<ReqT> requests) {
        try {
            checkInvokeMode(InvokeMode.DUPLEX_STREAM);
            return (Flux<RspT>) method.invoke(service, ctx, requests);
        } catch (Throwable t) {
            return Flux.error(t);
        }
    }

    /**
     * Check if the method supports the current call invokeMode.
     *
     * @param invokeMode expected method call invokeMode
     */
    private void checkInvokeMode(InvokeMode invokeMode) {
        if (this.invokeMode != invokeMode) {
            throw new UnsupportedOperationException("method " + method + " does not support " + invokeMode);
        }
    }
}
