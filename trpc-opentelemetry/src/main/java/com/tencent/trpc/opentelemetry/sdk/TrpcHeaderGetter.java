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
import com.tencent.trpc.opentelemetry.sdk.support.DefaultTrpcRequestGetter;
import com.tencent.trpc.opentelemetry.spi.ITrpcRequestGetter;
import io.opentelemetry.context.propagation.TextMapGetter;
import java.util.NoSuchElementException;
import java.util.ServiceLoader;
import jakarta.annotation.Nullable;

/**
 * Service request header trace parsing adaptation class
 */
public final class TrpcHeaderGetter implements TextMapGetter<Request> {

    private static final Logger logger = LoggerFactory.getLogger(TrpcHeaderGetter.class);

    public static final TrpcHeaderGetter GETTER = new TrpcHeaderGetter();
    private static final ITrpcRequestGetter AGENT = getSpiInstance();

    private static ITrpcRequestGetter getSpiInstance() {
        ITrpcRequestGetter spiInst = null;
        try {
            ServiceLoader<ITrpcRequestGetter> serviceLoader = ServiceLoader.load(ITrpcRequestGetter.class);
            spiInst = serviceLoader.iterator().next();
        } catch (NoSuchElementException ignored) {
            logger.warn("get spi instance error: ", ignored);
        } catch (Exception e) {
            logger.error("get spi instance error: ", e);
        }

        if (spiInst == null) {
            spiInst = new DefaultTrpcRequestGetter();
        }
        return spiInst;
    }

    @Override
    public Iterable<String> keys(Request carrier) {
        return AGENT.keys(carrier);
    }

    @Nullable
    @Override
    public String get(@Nullable Request carrier, String key) {
        return AGENT.get(carrier, key);
    }

}
