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

package com.tencent.trpc.core.stream.transport;

import com.tencent.trpc.core.stream.Closeable;
import java.util.function.Function;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

/**
 * Server-side transport
 */
public interface ServerTransport<T extends Closeable> {

    /**
     * Starting the server transport, returns a closable service
     *
     * @param acceptor initialization operations for the server after receiving a connection request
     * @return return a {@link Closeable} service
     */
    Mono<T> start(ConnectionAcceptor acceptor);

    interface ConnectionAcceptor extends Function<RpcConnection, Publisher<Void>> {

        /**
         * Called when a connection is established, it can perform some asynchronous operations.
         * The returned Mono&lt;Void&gt; is used to determine if the asynchronous operation is completed.
         *
         * @param connection the connection to be initialized.
         * @return an asynchronous operation that indicates the initializing state.
         */
        @Override
        Mono<Void> apply(RpcConnection connection);
    }

}
