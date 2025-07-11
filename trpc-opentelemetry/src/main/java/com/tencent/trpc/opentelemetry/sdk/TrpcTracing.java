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

package com.tencent.trpc.opentelemetry.sdk;

import com.tencent.trpc.core.rpc.Request;
import com.tencent.trpc.core.rpc.Response;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;

/**
 * Generate tRPC trace instrumenter
 */
public final class TrpcTracing {

    private static final TrpcTracing INSTANT = TrpcTracing.create(GlobalOpenTelemetry.get());
    private final Instrumenter<Request, Response> server;
    private final Instrumenter<Request, Response> client;

    public TrpcTracing(Instrumenter<Request, Response> serverInstrument,
            Instrumenter<Request, Response> clientInstrument) {
        this.server = serverInstrument;
        this.client = clientInstrument;
    }

    public static TrpcTracing create(OpenTelemetry openTelemetry) {
        return newBuilder(openTelemetry).build();
    }

    public static TrpcTracingBuilder newBuilder(OpenTelemetry openTelemetry) {
        return new TrpcTracingBuilder(openTelemetry);
    }

    public static TrpcTracing getInstant() {
        return INSTANT;
    }

    /**
     * Get the instance on the server side
     *
     * @return Instrumenter of server side
     */
    public Instrumenter<Request, Response> getServer() {
        return server;
    }

    /**
     * Get the instance on the client side
     *
     * @return Instrumenter of client side
     */
    public Instrumenter<Request, Response> getClient() {
        return client;
    }

}
