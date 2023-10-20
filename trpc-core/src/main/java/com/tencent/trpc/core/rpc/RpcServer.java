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

package com.tencent.trpc.core.rpc;

import com.tencent.trpc.core.common.config.ProtocolConfig;
import com.tencent.trpc.core.common.config.ProviderConfig;
import com.tencent.trpc.core.exception.TRpcException;
import java.net.URI;

/**
 * RpcServer abstract interface, main functions: bind ports according to ProtocolConfig, expose related services
 * according to ProviderConfig.
 * Note: Is it necessary to convert {@link ProtocolConfig} to {@link URI}?
 */
public interface RpcServer {

    /**
     * Establish connection.
     */
    void open() throws TRpcException;

    /**
     * Expose the service to the corresponding protocol of RpcServer, chain processing of invoker is wrapped in the
     * outer layer.
     *
     * @param invoker ProviderInvoker
     */
    <T> void export(ProviderInvoker<T> invoker);

    /**
     * Unregister the service.
     */
    <T> void unexport(ProviderConfig<T> config);

    /**
     * Close the server.
     */
    void close();

    /**
     * Register close event callback.
     */
    CloseFuture<Void> closeFuture();

    /**
     * Whether the server is closed.
     */
    boolean isClosed();

    /**
     * Get the protocol configuration object.
     */
    ProtocolConfig getProtocolConfig();

}
