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

package com.tencent.trpc.opentelemetry.sdk;

import com.tencent.trpc.core.rpc.Request;
import com.tencent.trpc.core.rpc.Response;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.api.instrumenter.AttributesExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;
import io.opentelemetry.instrumentation.api.instrumenter.InstrumenterBuilder;
import io.opentelemetry.instrumentation.api.instrumenter.rpc.RpcClientAttributesExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.rpc.RpcServerAttributesExtractor;
import java.util.ArrayList;
import java.util.List;

/**
 * Builder class of tRPC trace
 */
public final class TrpcTracingBuilder {

    private static final String INSTRUMENTATION_SERVER_NAME = "trpc-opentelemetry-plugin-server";
    private static final String INSTRUMENTATION_CLIENT_NAME = "trpc-opentelemetry-plugin-client";

    private final OpenTelemetry openTelemetry;

    private final List<AttributesExtractor<? super Request, ? super Response>> additionalExtractors = new ArrayList<>();

    TrpcTracingBuilder(OpenTelemetry openTelemetry) {
        this.openTelemetry = openTelemetry;
    }

    /**
     * Instance generation class for generating trace information
     *
     * @return TrpcTracing instance
     */
    public TrpcTracing build() {
        InstrumenterBuilder<Request, Response> builder = Instrumenter.builder(openTelemetry,
                INSTRUMENTATION_SERVER_NAME, new TrpcSpanNameExtractor());
        builder.setSpanStatusExtractor(new TrpcSpanStatusExtractor())
                .addAttributesExtractor(RpcServerAttributesExtractor.create(new TrpcAttributesGetter()))
                .addAttributesExtractor(new TrpcRequestExtractor()).addOperationMetrics(TrpcServerMetrics.get())
                .addAttributesExtractors(additionalExtractors);
        Instrumenter<Request, Response> server = builder.buildServerInstrumenter(TrpcHeaderGetter.GETTER);

        InstrumenterBuilder<Request, Response> clientBuilder = Instrumenter.builder(openTelemetry,
                INSTRUMENTATION_CLIENT_NAME, new TrpcSpanNameExtractor());
        clientBuilder.setSpanStatusExtractor(new TrpcSpanStatusExtractor())
                .addAttributesExtractor(RpcClientAttributesExtractor.create(new TrpcAttributesGetter()))
                .addAttributesExtractor(new TrpcRequestExtractor()).addOperationMetrics(TrpcClientMetrics.get())
                .addAttributesExtractors(additionalExtractors);
        Instrumenter<Request, Response> client = clientBuilder.buildClientInstrumenter(TrpcHeaderSetter.SETTER);
        return new TrpcTracing(server, client);
    }

}
