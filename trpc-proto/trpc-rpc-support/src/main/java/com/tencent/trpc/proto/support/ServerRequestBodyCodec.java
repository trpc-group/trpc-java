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

package com.tencent.trpc.proto.support;

import com.tencent.trpc.core.rpc.Request;
import com.tencent.trpc.core.rpc.common.RpcMethodInfo;


/**
 * Codec interface implemented by tRPC servers when decoding client request.
 */
public interface ServerRequestBodyCodec {

    /**
     * Decode request data
     *
     * @param request request object before decoding
     * @param methodInfo info of corresponding rpc method
     */
    void decode(Request request, RpcMethodInfo methodInfo);
}
