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

import com.google.protobuf.Message;
import com.tencent.trpc.core.utils.JsonUtils;
import com.tencent.trpc.core.utils.ProtoJsonConverter;
import java.util.Map;

/**
 * TRPC Related Tools
 */
public class TrpcTool {

    private static final String TRPC_MESSAGE_DISABLE = "trpc.message.disable";
    private static final String ENV_TRPC_MESSAGE_DISABLE = "TRPC_MESSAGE_DISABLE";
    private static final String TRUE = "true";

    /**
     * Objects converted to text
     *
     * @param obj object to be converted
     * @return json text
     */
    public static String toMessage(Object obj) {
        String disable = System.getProperty(TRPC_MESSAGE_DISABLE, System.getenv(ENV_TRPC_MESSAGE_DISABLE));
        if (TRUE.equals(disable)) {
            return String.format("tRPC message disabled, enable it by environment [%s] or property [%s]",
                    ENV_TRPC_MESSAGE_DISABLE, TRPC_MESSAGE_DISABLE);
        }
        if (obj == null) {
            return "{}";
        }
        if (obj instanceof String) {
            return (String) obj;
        }

        try {
            if (obj instanceof Message) {
                return ProtoJsonConverter.messageToJson((Message) obj);
            }
            if (obj instanceof Map) {
                return JsonUtils.toJson(obj);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return obj.toString();
    }

}
