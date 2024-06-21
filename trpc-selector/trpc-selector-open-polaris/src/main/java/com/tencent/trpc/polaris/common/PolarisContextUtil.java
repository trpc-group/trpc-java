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

package com.tencent.trpc.polaris.common;

import com.fasterxml.jackson.core.type.TypeReference;
import com.tencent.polaris.metadata.core.MessageMetadataContainer;
import com.tencent.polaris.metadata.core.MetadataType;
import com.tencent.polaris.metadata.core.TransitiveType;
import com.tencent.polaris.metadata.core.manager.MetadataContext;
import com.tencent.trpc.core.rpc.Request;
import com.tencent.trpc.core.rpc.RpcClientContext;
import com.tencent.trpc.core.utils.JsonUtils;
import com.tencent.trpc.core.utils.RpcContextUtils;
import com.tencent.trpc.core.utils.StringUtils;
import java.util.HashMap;
import java.util.Map;

public class PolarisContextUtil {

    /**
     * It is mainly used for users to insert the MetadataContext of polarismesh into RpcClientContext,
     * and realize the transfer of polarismesh metadata between microservice frameworks.
     *
     * @param metadataContext {@link MetadataContext}
     * @param context {@link RpcClientContext}
     */
    public static void putRpcContext(RpcClientContext context, MetadataContext metadataContext) {
        RpcContextUtils.putValueMapValue(context, PolarisConstant.RPC_CONTEXT_POALRIS_METADATA, metadataContext);
    }

    /**
     * After the request initiator obtains the tag that needs to be transmitted from MetadataContext,
     * it stores it in Request and passes it to the downstream.
     *
     * @param metadataContext {@link MetadataContext}
     * @param request {@link Request}
     */
    public static void putAttachValue(Request request, MetadataContext metadataContext) {
        MessageMetadataContainer callerContainer = metadataContext.getMetadataContainer(
                MetadataType.MESSAGE, true);
        Map<String, String> transitiveHeaders = new HashMap<>(callerContainer.getTransitiveHeaders());

        MessageMetadataContainer calleeContainer = metadataContext.getMetadataContainer(
                MetadataType.MESSAGE, false);
        transitiveHeaders.putAll(calleeContainer.getTransitiveHeaders());

        RpcContextUtils.putAttachValue(request, PolarisConstant.RPC_CONTEXT_TRANSITIVE_METADATA,
                JsonUtils.toJson(transitiveHeaders));
    }

    /**
     * After the request recipient obtains the tag that needs to be transmitted from the Request information,
     * it is stored in the MetadataContext
     *
     * @param request {@link Request}
     * @return {@link MetadataContext}
     */
    public static MetadataContext getMetadataContext(Request request) {
        String metadataValue = RpcContextUtils.getRequestAttachValue(request.getContext(),
                PolarisConstant.RPC_CONTEXT_TRANSITIVE_METADATA);
        if (StringUtils.isEmpty(metadataValue)) {
            metadataValue = new String((byte[]) request.getAttachment(PolarisConstant.RPC_CONTEXT_TRANSITIVE_METADATA));
            if (StringUtils.isEmpty(metadataValue)) {
                return new MetadataContext(MetadataContext.DEFAULT_TRANSITIVE_PREFIX);
            }
        }
        Map<String, String> transitiveHeaders = JsonUtils.fromJson(metadataValue,
                new TypeReference<Map<String, String>>() {
                });

        MetadataContext metadataContext = new MetadataContext();
        MessageMetadataContainer callerContainer = metadataContext.getMetadataContainer(
                MetadataType.MESSAGE, true);
        transitiveHeaders.forEach((headerKey, headerValue) -> {
            if (headerKey.startsWith(metadataContext.getTransitivePrefix())) {
                headerKey = headerKey.replaceAll(metadataContext.getTransitivePrefix(), "");
                callerContainer.setHeader(headerKey, headerValue, TransitiveType.PASS_THROUGH);
            } else {
                callerContainer.setHeader(headerKey, headerValue, TransitiveType.NONE);
            }
        });
        return metadataContext;
    }

}
