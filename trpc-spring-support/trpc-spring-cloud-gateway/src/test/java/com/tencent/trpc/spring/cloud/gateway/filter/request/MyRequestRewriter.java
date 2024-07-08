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

package com.tencent.trpc.spring.cloud.gateway.filter.request;

import com.tencent.trpc.spring.cloud.gateway.rewriter.TrpcRequestRewriter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.web.server.ServerWebExchange;

public class MyRequestRewriter implements TrpcRequestRewriter {

    @Override
    public DataBuffer resolver(ServerWebExchange exchange, Route route, DataBuffer body) {
        return null;
    }
}
