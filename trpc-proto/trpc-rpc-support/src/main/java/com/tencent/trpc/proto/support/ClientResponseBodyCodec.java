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
import com.tencent.trpc.core.rpc.Response;
import com.tencent.trpc.core.rpc.common.RpcMethodInfo;

/**
 * Codec interface implemented by tRPC clients when decoding server response.
 */
public interface ClientResponseBodyCodec {

    /**
     * Decode response data
     *
     * @param response response object before decoding
     * @param request corresponding request object
     * @param methodInfo info of corresponding rpc method
     */
    void decode(Response response, Request request, RpcMethodInfo methodInfo);
}
