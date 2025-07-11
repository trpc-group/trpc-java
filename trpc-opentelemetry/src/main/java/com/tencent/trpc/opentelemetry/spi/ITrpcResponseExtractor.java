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
import com.tencent.trpc.core.rpc.Response;
import java.util.Map;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Get the data reported in Span from the response:
 * implement this interface for customization when the default implementation
 * does not meet the requirements of the business side.
 */
public interface ITrpcResponseExtractor {

    /**
     * Get the data reported in Span from the response
     * <br>
     * Note: SPI implementations are responsible for handling exceptions,
     * and throwing exceptions in this method may affect normal business processes.
     *
     * @param request request
     * @param response response
     * @param error exception message
     * @return Returns the reported key/value pairs
     */
    Map<String, String> extract(Request request, @Nullable Response response, @Nullable Throwable error);

    /**
     * Determine if the response result was successful
     * <br>
     * Note: SPI implementations are responsible for handling exceptions,
     * and throwing exceptions in this method may affect normal business processes.
     *
     * @param request request
     * @param response response
     * @param error exception message
     * @return Whether the execution was successful or not
     */
    default boolean isSuccessful(Request request, @Nullable Response response, @Nullable Throwable error) {
        if (error != null) {
            return false;
        }
        if (response == null || response.getException() != null) {
            return false;
        }
        return true;
    }
    
}
