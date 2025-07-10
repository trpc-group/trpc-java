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

package com.tencent.trpc.opentelemetry.spi;

import com.tencent.trpc.core.rpc.Request;

/**
 * Get the propagation data from the request,
 * implement this interface for customization when the
 * business private protocol different from the default implementation
 */
public interface ITrpcRequestGetter {

    /**
     * Get all the keys in the request and get the relevant trace information from the tRPC meta
     * <br>
     * Note: SPI implementations are responsible for handling exceptions,
     * and throwing exceptions in this method may affect normal business processes.
     *
     * @param carrier tRPC request
     * @return All data in tRPC meta
     */
    Iterable<String> keys(Request carrier);

    /**
     * Getting specific values from the request, mainly traceparent information
     * <br>
     * Note: SPI implementations are responsible for handling exceptions,
     * and throwing exceptions in this method may affect normal business processes.
     *
     * @param carrier tRPC request
     * @param key The specified key value to fetch
     * @return The corresponding value
     */
    String get(Request carrier, String key);

}
