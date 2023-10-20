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

package com.tencent.trpc.spring.cloud.gateway.client;

import com.tencent.trpc.core.rpc.GenericClient;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.http.server.reactive.ServerHttpRequest;
import reactor.core.publisher.Mono;

/**
 * This is a GatewayClient for downstream in routing transfer. The default implementation is {@link TrpcGatewayClient}.
 * When implementing, be careful to avoid causing multi-threading safety issues.
 */
public interface GatewayClient {

    /**
     * Create a generic Client {@link GenericClient}.
     *
     * @param route gateway routing configuration information
     */
    void open(Route route);

    /**
     * AsyncInvoke supports asynchronous calls. To adapt to the SpringGateway standard, reactive programming is used.
     *
     * @param request the HTTP request
     * @param route route information, including metadata information. It can help to implement the requirements
     *         of
     *         different configurations corresponding to different contexts.
     * @param body the specific data for the call
     * @return Mono, to adapt to the SpringGateway standard. The return type is {@link Mono}.
     */
    Mono<byte[]> asyncInvoke(ServerHttpRequest request, Route route, byte[] body);

}
