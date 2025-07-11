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

import com.tencent.trpc.core.rpc.RpcContext;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * A reactive interface that supports streaming calls.
 */
public interface StreamCall extends Closeable {

    /**
     * Server-side streaming function
     *
     * @param ctx a {@link RpcContext} contains rpc params
     * @param request request data
     * @param <ReqT> request type
     * @param <RspT> response type
     * @return Reactive result for server-side streaming calls
     */
    default <ReqT, RspT> Flux<RspT> serverStream(RpcContext ctx, ReqT request) {
        return Flux.error(new UnsupportedOperationException("Fire-and-Forget"));
    }

    /**
     * Client-side streaming function
     *
     * @param ctx a {@link RpcContext} contains rpc params
     * @param requests request data
     * @param <ReqT> request type
     * @param <RspT> response type
     * @return Reactive result for client-side streaming calls
     */
    default <ReqT, RspT> Mono<RspT> clientStream(RpcContext ctx, Publisher<ReqT> requests) {
        return Mono.error(new UnsupportedOperationException("Fire-and-Forget"));
    }

    /**
     * Duplex-side streaming function
     *
     * @param ctx a {@link RpcContext} contains rpc params
     * @param requests request data
     * @param <ReqT> request type
     * @param <RspT> response type
     * @return Reactive result for duplex-side streaming calls
     */
    default <ReqT, RspT> Flux<RspT> duplexStream(RpcContext ctx, Publisher<ReqT> requests) {
        return Flux.error(new UnsupportedOperationException("Fire-and-Forget"));
    }

    /**
     * Return a close event listener
     *
     * @return return a close event listener, with a default of returning a listener that never closes
     */
    @Override
    default Mono<Void> onClose() {
        return Mono.never();
    }

    /**
     * Action to be performed on close
     */
    @Override
    default void dispose() {
    }

    /**
     * Check if this object has been closed
     *
     * @return true if this object has been closed
     */
    @Override
    default boolean isDisposed() {
        return false;
    }
}
