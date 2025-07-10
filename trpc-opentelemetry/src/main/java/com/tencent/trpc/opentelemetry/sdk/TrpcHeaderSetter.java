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
import com.tencent.trpc.opentelemetry.sdk.support.DefaultTrpcRequestSetter;
import com.tencent.trpc.opentelemetry.spi.ITrpcRequestSetter;
import io.opentelemetry.context.propagation.TextMapSetter;
import java.util.NoSuchElementException;
import java.util.ServiceLoader;

/**
 * Client header trace information injection settings
 */
public final class TrpcHeaderSetter implements TextMapSetter<Request> {

    private static final Logger logger = LoggerFactory.getLogger(TrpcHeaderGetter.class);

    public static final TrpcHeaderSetter SETTER = new TrpcHeaderSetter();
    private static final ITrpcRequestSetter AGENT = getSpiInstance();

    private static ITrpcRequestSetter getSpiInstance() {
        ITrpcRequestSetter spiInst = null;
        try {
            ServiceLoader<ITrpcRequestSetter> serviceLoader = ServiceLoader.load(ITrpcRequestSetter.class);
            spiInst = serviceLoader.iterator().next();
        } catch (NoSuchElementException ignored) {
            logger.warn("get spi instance error: ", ignored);
        } catch (Exception e) {
            logger.error("get spi instance error: ", e);
        }

        if (spiInst == null) {
            spiInst = new DefaultTrpcRequestSetter();
        }
        return spiInst;
    }

    @Override
    public void set(Request carrier, String key, String value) {
        AGENT.set(carrier, key, value);
    }

}
