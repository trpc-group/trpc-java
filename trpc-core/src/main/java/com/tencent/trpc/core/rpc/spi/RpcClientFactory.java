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

package com.tencent.trpc.core.rpc.spi;

import com.tencent.trpc.core.common.config.ProtocolConfig;
import com.tencent.trpc.core.exception.TRpcException;
import com.tencent.trpc.core.extension.Extensible;
import com.tencent.trpc.core.rpc.RpcClient;

/**
 * TRPC client factory class.
 */
@Extensible("trpc")
public interface RpcClientFactory {

    /**
     * The client's startup timing is determined by the outer layer, call start before using the client.
     *
     * @param config protocol configuration
     * @return the created RpcClient
     * @throws TRpcException if there's an exception while creating the RpcClient
     */
    RpcClient createRpcClient(ProtocolConfig config) throws TRpcException;

}
