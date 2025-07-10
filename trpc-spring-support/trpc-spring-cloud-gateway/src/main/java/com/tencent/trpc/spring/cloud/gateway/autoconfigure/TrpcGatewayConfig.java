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

package com.tencent.trpc.spring.cloud.gateway.autoconfigure;

import com.tencent.trpc.spring.cloud.gateway.rewriter.DefaultTrpcRequestRewriter;
import com.tencent.trpc.spring.cloud.gateway.rewriter.DefaultTrpcResponseRewriter;
import com.tencent.trpc.spring.cloud.gateway.rewriter.TrpcRequestRewriter;
import com.tencent.trpc.spring.cloud.gateway.rewriter.TrpcResponseRewriter;
import com.tencent.trpc.spring.context.TRpcConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Prioritize loading `bean` managed by the `trpc` container at startup, see {@link TRpcConfiguration} for details.
 */
@Configuration
@Import(TRpcConfiguration.class)
public class TrpcGatewayConfig {

    /**
     * Default implementation of `TrpcRequestRewriter`, see {@link DefaultTrpcRequestRewriter}.
     * Use {@link ConditionalOnMissingBean} and {@link TrpcRequestRewriter} interface to extend parameter rewriting
     * logic.
     *
     * @return TrpcRequestRewriter instance
     */
    @Bean
    @ConditionalOnMissingBean(TrpcRequestRewriter.class)
    public TrpcRequestRewriter getTRPCArgumentResolver() {
        return new DefaultTrpcRequestRewriter();
    }

    /**
     * Default implementation of `TrpcResponseRewriter`, see {@link DefaultTrpcResponseRewriter}.
     * Use {@link ConditionalOnMissingBean} and {@link TrpcResponseRewriter} interface to rewrite response packet logic.
     *
     * @return DefaultTrpcResponseRewriter instance
     */
    @Bean
    @ConditionalOnMissingBean(TrpcResponseRewriter.class)
    public TrpcResponseRewriter getTRPCMessageWriter() {
        return new DefaultTrpcResponseRewriter();
    }

}
