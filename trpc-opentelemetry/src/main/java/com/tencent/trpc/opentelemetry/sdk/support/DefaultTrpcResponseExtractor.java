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

package com.tencent.trpc.opentelemetry.sdk.support;

import com.tencent.trpc.core.rpc.Request;
import com.tencent.trpc.core.rpc.Response;
import com.tencent.trpc.opentelemetry.sdk.Constants;
import com.tencent.trpc.opentelemetry.sdk.TrpcTool;
import com.tencent.trpc.opentelemetry.spi.ITrpcResponseExtractor;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;

/**
 * Default implementation of {@link ITrpcResponseExtractor}
 */
public class DefaultTrpcResponseExtractor implements ITrpcResponseExtractor {

    @Override
    public Map<String, String> extract(Request request, @Nullable Response response, @Nullable Throwable error) {
        Map<String, String> reportData = new HashMap<>();
        // If an exception occurs in the execution, the exception result is reported
        if (error != null) {
            reportData.put(Constants.DETAIL_KEY, error.getMessage());
            return reportData;
        }
        if (response == null) {
            // Returns null if the response body is empty
            reportData.put(Constants.DETAIL_KEY, "{}");
        } else if (response.getException() != null) {
            // If an exception occurs in the execution, the exception result is reported
            reportData.put(Constants.DETAIL_KEY, response.getException().getMessage());
        } else {
            reportData.put(Constants.DETAIL_KEY, TrpcTool.toMessage(response.getValue()));
        }
        return reportData;
    }

}
