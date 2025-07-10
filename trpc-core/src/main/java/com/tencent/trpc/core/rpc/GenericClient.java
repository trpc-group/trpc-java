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

import java.util.concurrent.CompletionStage;

/**
 * Generic interface, mainly used for scenarios without API definitions. Set service & method through RpcClientContext.
 * In the future, consider supporting multiple parameters and types, but for now, it's sufficient for internal use.
 */
public interface GenericClient {

    /**
     * Asynchronously invoke the generic client.
     *
     * @param context the RpcClientContext
     * @param body the byte array representing the request body
     * @return a CompletionStage containing the response as a byte array
     */
    CompletionStage<byte[]> asyncInvoke(RpcClientContext context, byte[] body);

    /**
     * Synchronously invoke the generic client.
     *
     * @param context the RpcClientContext
     * @param body the byte array representing the request body
     * @return the response as a byte array
     */
    byte[] invoke(RpcClientContext context, byte[] body);

}
