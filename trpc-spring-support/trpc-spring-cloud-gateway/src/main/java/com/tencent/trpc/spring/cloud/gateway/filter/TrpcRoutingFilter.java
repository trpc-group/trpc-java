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

package com.tencent.trpc.spring.cloud.gateway.filter;

import com.google.common.collect.Maps;
import com.tencent.trpc.core.logger.Logger;
import com.tencent.trpc.core.logger.LoggerFactory;
import com.tencent.trpc.spring.cloud.gateway.client.GatewayClient;
import com.tencent.trpc.spring.cloud.gateway.client.TrpcGatewayClient;
import com.tencent.trpc.spring.cloud.gateway.rewriter.TrpcRequestRewriter;
import com.tencent.trpc.spring.cloud.gateway.rewriter.TrpcResponseRewriter;
import java.util.concurrent.ConcurrentMap;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * TrpcRoutingFilter is the core class that implements {@link GatewayFilter}.
 * <p>According to the convention, the following code in the `routes` configuration in the `yml` file will execute the
 * filter logic in {@link TrpcRoutingFilter}.</p>
 * <pre>
 * filters:
 *   - TRPC=true, {"v1","1"}
 * </pre>
 * <p>Or configure like this:</p>
 * <pre>
 * filters:
 *   - name: TRPC
 *     args:
 *       enabled: true
 *       values: {"v1","1"}
 * </pre>
 * <p></p>
 * The `- name: TRPC` is a convention in SpringGateway and will automatically load the TRPCGatewayFilterFactory.
 */
public class TrpcRoutingFilter implements GatewayFilter {

    private static final Logger logger = LoggerFactory.getLogger(TrpcRoutingFilter.class);

    private final TrpcRequestRewriter requestRewriter;
    private final TrpcResponseRewriter responseRewriter;
    private final ConcurrentMap<String, TrpcGatewayClient> cache = Maps.newConcurrentMap();
    private final TrpcGatewayFilterFactory.Config config;

    public TrpcRoutingFilter(TrpcRequestRewriter requestRewriter,
            TrpcResponseRewriter responseRewriter,
            TrpcGatewayFilterFactory.Config config) {
        this.requestRewriter = requestRewriter;
        this.responseRewriter = responseRewriter;
        this.config = config;
        logger.info("TrpcRoutingFilter initialized");
    }

    /**
     * Filter the web request and call client.asyncInvoke, see {@link GatewayClient}.
     *
     * @param exchange {@link ServerWebExchange} can get route, metadata, etc. data through exchange.
     * @param chain {@link GatewayFilterChain} Gateway framework automatically passes the `chain` parameter.
     * @return See {@link Mono}
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        logger.info("currentThread: [{}]", Thread.currentThread().getName());
        if (!config.isEnabled()) {
            return chain.filter(exchange);
        }
        Route route = exchange.getRequiredAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
        GatewayClient client = getClient(route);
        ServerHttpRequest request = exchange.getRequest();
        return DataBufferUtils.join(request.getBody())
                .map(body -> requestRewriter.resolver(exchange, route, body))
                .map(body -> {
                    java.nio.ByteBuffer byteBuffer = body.asByteBuffer();
                    byte[] bytes = new byte[byteBuffer.remaining()];
                    byteBuffer.get(bytes);
                    byteBuffer.rewind();
                    return client.asyncInvoke(request, route, bytes);
                })
                .flatMap(result -> this.responseRewriter.write(exchange, route.getMetadata(), result));
    }

    /**
     * Get the gateway client object corresponding to the specified gateway routing information.
     * If the specified gateway routing information is not found in the cache Map, create a new gateway client object
     * and add it to the cache.
     * To ensure thread safety, the `synchronized` keyword is added.
     *
     * @param route See {@link Route}
     * @return See {@link TrpcGatewayClient}
     */
    private synchronized TrpcGatewayClient getClient(Route route) {
        return cache.computeIfAbsent(route.getUri().getSchemeSpecificPart(),
                key -> new TrpcGatewayClient(route));
    }

}
