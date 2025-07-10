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

import reactor.core.publisher.Mono;

/**
 * A client transport. Transport is used to indicate the rpc communication framework, like netty and jetty.
 */
public interface ClientTransport {

    /**
     * Create a connection with a remote server.
     *
     * @return a listener used to indicate the connection state.
     */
    Mono<RpcConnection> connect();

}
