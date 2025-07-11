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

package com.tencent.trpc.spring.cloud.gateway.rewriter;

import org.springframework.cloud.gateway.route.Route;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.web.server.ServerWebExchange;

public class DefaultTrpcRequestRewriter implements TrpcRequestRewriter {

    public DefaultTrpcRequestRewriter() {
    }

    public DataBuffer resolver(ServerWebExchange exchange,
            Route route,
            DataBuffer body) {
        return body;
    }

}
