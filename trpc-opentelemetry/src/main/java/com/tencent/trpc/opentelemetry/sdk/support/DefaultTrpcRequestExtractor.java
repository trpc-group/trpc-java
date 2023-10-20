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
import com.tencent.trpc.opentelemetry.sdk.Constants;
import com.tencent.trpc.opentelemetry.sdk.TrpcTool;
import com.tencent.trpc.opentelemetry.spi.ITrpcRequestExtractor;
import java.util.HashMap;
import java.util.Map;

/**
 * Default implementation of {@link ITrpcRequestExtractor}
 */
public class DefaultTrpcRequestExtractor implements ITrpcRequestExtractor {

    @Override
    public Map<String, String> extract(Request request) {
        Map<String, String> reportData = new HashMap<>();
        reportData.put(Constants.ATTACH_KEY, TrpcTool.toMessage(request.getAttachments()));
        reportData.put(Constants.META_KEY, TrpcTool.toMessage(request.getMeta().getMap()));
        Object[] arguments = request.getInvocation().getArguments();
        for (int i = 0; i < arguments.length; i++) {
            reportData.put(Constants.BODY_KEY + i, TrpcTool.toMessage(arguments[i]));
        }
        return reportData;
    }
    
}
