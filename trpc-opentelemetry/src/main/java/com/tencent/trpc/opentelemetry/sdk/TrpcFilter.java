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

import com.tencent.trpc.core.extension.Extension;
import com.tencent.trpc.core.filter.spi.Filter;
import com.tencent.trpc.core.logger.Logger;
import com.tencent.trpc.core.logger.LoggerFactory;
import com.tencent.trpc.core.rpc.Invoker;
import com.tencent.trpc.core.rpc.Request;
import com.tencent.trpc.core.rpc.Response;
import com.tencent.trpc.core.rpc.RpcServerContext;
import com.tencent.trpc.core.utils.RpcContextUtils;
import com.tencent.trpc.opentelemetry.sdk.support.DefaultTrpcRequestExtractor;
import com.tencent.trpc.opentelemetry.spi.ITrpcRequestExtractor;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.ServiceLoader;
import java.util.concurrent.CompletionStage;

/**
 * The tRPC interceptor implementation, which needs to be loaded at the instrumentation's advice,
 * and needs to find entry points that are loaded in higher order
 */
@Extension(TrpcFilter.NAME)
public class TrpcFilter implements Filter {

    public static final String NAME = "opentelemetry";
    private static final Logger logger = LoggerFactory.getLogger(TrpcFilter.class);

    private static ITrpcRequestExtractor EXTRACTOR = getSpiExtractor();

    /**
     * Setting the property value information,
     * adding the content of the request parameter
     *
     * @param request tRPC request
     */
    private static void setAttributeWithArg(Request request) {
        AttributesBuilder builder = Attributes.builder();
        Map<String, String> reportData = EXTRACTOR.extract(request);
        reportData.forEach((key, value) -> builder.put(AttributeKey.stringKey(key), value));
        Span.current().addEvent(Constants.SENT_KEY, builder.build());
    }

    /**
     * System default implementation is taken when there is no user implementation.
     *
     * @return Implementation of {@link ITrpcRequestExtractor}
     */
    private static ITrpcRequestExtractor getSpiExtractor() {
        ITrpcRequestExtractor spiExtractor = null;
        try {
            ServiceLoader<ITrpcRequestExtractor> serviceLoader = ServiceLoader.load(ITrpcRequestExtractor.class);
            spiExtractor = serviceLoader.iterator().next();
        } catch (NoSuchElementException ignored) {
            logger.warn("get spi extractor error: ", ignored);
        } catch (Exception e) {
            logger.error("get spi extractor error: ", e);
        }

        if (spiExtractor == null) {
            spiExtractor = new DefaultTrpcRequestExtractor();
        }
        return spiExtractor;
    }

    @Override
    public int getOrder() {
        return Filter.HIGHEST_PRECEDENCE + 300;
    }

    @Override
    public CompletionStage<Response> filter(Invoker<?> invoker, Request request) {
        if (request.getContext() instanceof RpcServerContext) {
            return serverFilter(invoker, request);
        }
        return clientFilter(invoker, request);
    }

    /**
     * Service-side interception processing logic
     *
     * @param invoker invoker
     * @param request request
     * @return response
     */
    public CompletionStage<Response> serverFilter(Invoker<?> invoker, Request request) {
        Context context = TrpcTracing.getInstant().getServer().start(Context.current(), request);
        RpcContextUtils.putValueMapValue(request.getContext(), Constants.CTX_TELEMETRY_CONTEXT, context);

        CompletionStage<Response> response;
        try (Scope ignored = context.makeCurrent()) {
            setAttributeWithArg(request);
            response = invoker.invoke(request);
        } catch (Throwable e) {
            TrpcTracing.getInstant().getServer().end(context, request, null, e);
            throw e;
        }
        // Asynchronous response, need to be aware of thread switching
        return response.whenComplete((r, t) -> {
            try {
                TrpcTracing.getInstant().getServer().end(context, request, r, null);
            } catch (RuntimeException e) {
                logger.error("server filter error: ", e);
            }
        });
    }

    /**
     * Client-side interception processing logic that
     * obtains the link information in the current request body
     * and injects it into the request
     *
     * @param invoker invoker
     * @param request request
     * @return response
     */
    public CompletionStage<Response> clientFilter(Invoker<?> invoker, Request request) {
        CompletionStage<Response> response;
        // First fetch the trace from the context of the request,
        // then fetch the trace in the current thread if it can't be fetched.
        Context parentContext = RpcContextUtils.getValueMapValue(request.getContext(), Constants.CTX_TELEMETRY_CONTEXT);

        // When switching threads, the trace may not be retrieved, using the cluster filter for enhancement
        if (parentContext == null) {
            parentContext = Context.current();
        }
        Context context = TrpcTracing.getInstant().getClient().start(parentContext, request);
        // Set the generated context information to the client's request.
        RpcContextUtils.putValueMapValue(request.getContext(), Constants.CTX_TELEMETRY_CONTEXT, context);
        try (Scope ignored = context.makeCurrent()) {
            setAttributeWithArg(request);
            response = invoker.invoke(request);
        } catch (Throwable e) {
            TrpcTracing.getInstant().getClient().end(context, request, null, e);
            throw e;
        }
        return response.whenComplete((r, t) -> {
            try {
                TrpcTracing.getInstant().getClient().end(context, request, r, null);
            } catch (RuntimeException e) {
                logger.error("client filter error: ", e);
            }
        });
    }

}
