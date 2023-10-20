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

import com.tencent.trpc.core.logger.Logger;
import com.tencent.trpc.core.logger.LoggerFactory;
import com.tencent.trpc.core.rpc.Request;
import com.tencent.trpc.core.rpc.Response;
import com.tencent.trpc.opentelemetry.sdk.support.DefaultTrpcAttributesExtractor;
import com.tencent.trpc.opentelemetry.spi.ITrpcAttributesExtractor;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.context.Context;
import io.opentelemetry.instrumentation.api.instrumenter.AttributesExtractor;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.ServiceLoader;
import javax.annotation.Nullable;

/**
 * TRPC request parser for parsing and adding additional request information to the context
 */
public class TrpcRequestExtractor implements AttributesExtractor<Request, Response> {

    private static final Logger logger = LoggerFactory.getLogger(TrpcRequestExtractor.class);

    private static ITrpcAttributesExtractor EXTRACTOR = getSpiExactor();

    private static ITrpcAttributesExtractor getSpiExactor() {
        ITrpcAttributesExtractor spiExtractor = null;
        try {
            ServiceLoader<ITrpcAttributesExtractor> serviceLoader = ServiceLoader.load(ITrpcAttributesExtractor.class);
            spiExtractor = serviceLoader.iterator().next();
        } catch (NoSuchElementException ignored) {
            logger.warn("get spi extractor error: ", ignored);
        } catch (Exception e) {
            logger.error("get spi extractor error: ", e);
        }

        if (spiExtractor == null) {
            spiExtractor = new DefaultTrpcAttributesExtractor();
        }
        return spiExtractor;
    }

    @Override
    public void onStart(AttributesBuilder attributes, Context parentContext, Request request) {
        Map<String, String> reportData = EXTRACTOR.extract(request);
        reportData.forEach((key, value) -> attributes.put(AttributeKey.stringKey(key), value));
    }

    @Override
    public void onEnd(AttributesBuilder attributes, Context context, Request request, @Nullable Response response,
            @Nullable Throwable error) {
    }

}
