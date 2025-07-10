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

import com.tencent.trpc.core.logger.Logger;
import com.tencent.trpc.core.logger.LoggerFactory;
import com.tencent.trpc.core.rpc.Request;
import com.tencent.trpc.core.rpc.Response;
import com.tencent.trpc.core.utils.RpcContextUtils;
import com.tencent.trpc.opentelemetry.sdk.support.DefaultTrpcResponseExtractor;
import com.tencent.trpc.opentelemetry.spi.ITrpcResponseExtractor;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.context.Context;
import io.opentelemetry.instrumentation.api.instrumenter.SpanStatusBuilder;
import io.opentelemetry.instrumentation.api.instrumenter.SpanStatusExtractor;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.ServiceLoader;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Span status extractor
 */
public class TrpcSpanStatusExtractor implements SpanStatusExtractor<Request, Response> {

    private static final Logger logger = LoggerFactory.getLogger(TrpcSpanStatusExtractor.class);

    private static ITrpcResponseExtractor EXTRACTOR = getSpiExtractor();

    private static ITrpcResponseExtractor getSpiExtractor() {
        ITrpcResponseExtractor spiExtractor = null;
        try {
            ServiceLoader<ITrpcResponseExtractor> serviceLoader = ServiceLoader.load(ITrpcResponseExtractor.class);
            spiExtractor = serviceLoader.iterator().next();
        } catch (NoSuchElementException ignored) {
            logger.warn("get spi extractor error: ", ignored);
        } catch (Exception e) {
            logger.error("get spi extractor error: ", e);
        }

        if (spiExtractor == null) {
            spiExtractor = new DefaultTrpcResponseExtractor();
        }
        return spiExtractor;
    }

    @Override
    public void extract(SpanStatusBuilder spanStatusBuilder, Request request, @Nullable Response response,
            @Nullable Throwable throwable) {
        Context parentContext = RpcContextUtils.getValueMapValue(request.getContext(), Constants.CTX_TELEMETRY_CONTEXT);
        Span span = Span.fromContextOrNull(parentContext);
        if (span == null) {
            spanStatusBuilder.setStatus(StatusCode.ERROR);
            return;
        }
        AttributesBuilder builder = Attributes.builder();
        Map<String, String> reportData = EXTRACTOR.extract(request, response, throwable);
        reportData.forEach((key, value) -> builder.put(AttributeKey.stringKey(key), value));
        span.addEvent(Constants.RECEIVED_KEY, builder.build());
        if (EXTRACTOR.isSuccessful(request, response, throwable)) {
            spanStatusBuilder.setStatus(StatusCode.OK);
            return;
        }
        spanStatusBuilder.setStatus(StatusCode.ERROR);
    }
    
}
