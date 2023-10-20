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

package com.tencent.trpc.spring.cloud.gateway.rewriter;

import java.util.Map;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

public interface TrpcResponseRewriter {

    Mono<Void> write(ServerWebExchange exchange,
            Map<String, Object> metaData,
            Mono<byte[]> result);

}
