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

package com.tencent.trpc.core.rpc;

import com.tencent.trpc.core.common.config.ConsumerConfig;
import com.tencent.trpc.core.common.config.ProtocolConfig;
import com.tencent.trpc.core.exception.TRpcException;

/**
 * RpcClient abstract interface, in point-to-point scenarios, it is used to generate invoker that shields the underlying
 * communication.Main functions: bind ports according to ProtocolConfig, generate stubs for remote services based
 * on ConsumerConfig combined with ProxyFactory.
 */
public interface RpcClient {

    /**
     * Establish connection.
     */
    void open() throws TRpcException;

    /**
     * Generate invoker, delegate to proxy to generate proxy objects, chain processing of invoker is wrapped in the
     * outer layer.
     *
     * @param consumerConfig method caller's configuration related to the interface, such as: timeout, filter
     *         configuration, etc.
     */
    <T> ConsumerInvoker<T> createInvoker(ConsumerConfig<T> consumerConfig);

    /**
     * Close connection.
     */
    void close();

    /**
     * Register close event callback.
     */
    CloseFuture<Void> closeFuture();

    /**
     * Indicates whether the Client is available.
     */
    boolean isAvailable();

    /**
     * Whether it is closed.
     */
    boolean isClosed();

    /**
     * Get the protocol configuration object.
     */
    ProtocolConfig getProtocolConfig();

    /**
     * Get the number of requests which are still in flight on this client.
     *
     * <p>It is used by the idle client cleaner to skip a client which still has in-flight requests,
     * otherwise closing the client would forcibly fail those requests with {@code Client(...) stop}.</p>
     *
     * <p>A {@code default} implementation is provided to keep binary compatibility with the existing
     * third-party implementations, which simply reports "no in-flight request" and thus keeps the old
     * cleaning behavior.</p>
     *
     * @return the number of in-flight requests, 0 if unknown
     */
    default int getPendingRequestCount() {
        return 0;
    }

}
