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

import com.tencent.trpc.core.logger.Logger;
import com.tencent.trpc.core.logger.LoggerFactory;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

public class DefaultTrpcResponseRewriter implements TrpcResponseRewriter {

    private static final Logger logger = LoggerFactory.getLogger(DefaultTrpcResponseRewriter.class);

    public Mono<Void> write(ServerWebExchange exchange,
            Map<String, Object> metaData,
            Mono<byte[]> result) {
        ServerHttpResponse response = exchange.getResponse();
        if (result != null) {
            return result.flatMap(bytes -> {
                DataBuffer dataBuffer = response.bufferFactory().wrap(bytes);
                response.getHeaders().add("Content-Type", MimeTypeUtils.APPLICATION_JSON_VALUE);
                return response.writeWith(Mono.just(dataBuffer));
            });
        }
        return Mono.empty();
    }

}
