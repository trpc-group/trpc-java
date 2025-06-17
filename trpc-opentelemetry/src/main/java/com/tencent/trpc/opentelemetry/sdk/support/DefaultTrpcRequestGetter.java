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

package com.tencent.trpc.opentelemetry.sdk.support;

import com.tencent.trpc.core.rpc.Request;
import com.tencent.trpc.opentelemetry.spi.ITrpcRequestGetter;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import jakarta.annotation.Nullable;

/**
 * Default implementation of {@link ITrpcRequestGetter}
 */
public class DefaultTrpcRequestGetter implements ITrpcRequestGetter {

    /**
     * Get all keys of request, get trace info from tRPC meta
     *
     * @param carrier tRPC request
     * @return all keys in meta info
     */
    @Override
    public Iterable<String> keys(Request carrier) {
        if (carrier == null || carrier.getAttachments() == null) {
            return Collections.emptySet();
        }
        return carrier.getAttachments().keySet();
    }

    /**
     * Getting specific values from the request, mainly traceparent information
     *
     * @param carrier tRPC request object
     * @param key The specified key value to fetch
     * @return corresponding value
     */
    @Nullable
    @Override
    public String get(@Nullable Request carrier, String key) {
        if (carrier == null || carrier.getAttachments() == null) {
            return null;
        }
        if (carrier.getAttachments().containsKey(key)) {
            Object value = carrier.getAttachments().get(key);
            if (value instanceof String) {
                return (String) value;
            } else if (value.getClass() == byte[].class) {
                return new String((byte[]) value, StandardCharsets.UTF_8);
            }
        }
        return null;
    }
    
}
