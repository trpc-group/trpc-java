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
import java.util.Map;

/**
 * Get the data to be reported in Span from the request,
 * when the default implementation does not meet the requirements of the business side,
 * implement this interface for customization
 */
public interface ITrpcRequestExtractor {

    /**
     * Get the data reported in Span from the request
     * <br>
     * Note: SPI implementations are responsible for handling exceptions,
     * and throwing exceptions in this method may affect normal business processes.
     *
     * @param request tRPC request
     * @return Reported key/value pairs.
     */
    Map<String, String> extract(Request request);

}
