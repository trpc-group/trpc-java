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

package com.tencent.trpc.core.utils;

import com.tencent.trpc.core.rpc.Request;
import com.tencent.trpc.core.rpc.Response;
import com.tencent.trpc.core.rpc.RpcContext;
import com.tencent.trpc.core.rpc.RpcContextValueKeys;
import com.tencent.trpc.core.telemetry.SpanContext;
import io.opentracing.Span;
import io.opentracing.Tracer;
import java.util.Map;
import java.util.Objects;

/**
 * Utility class for setting attachment information in the TRPC protocol.
 */
public class RpcContextUtils {

    /**
     * Deprecated, switch to {@link RpcContextUtils#getParentSpan(RpcContext)}
     *
     * @param context Rpc context
     * @return the OpenTracing span information
     */
    @Deprecated
    public static Span getSpan(RpcContext context) {
        return (Span) context.getValueMap().get(RpcContextValueKeys.CTX_TRACE_SPAN);
    }

    /**
     * Deprecated, OpenTracing related plugins can switch to tpstelemetry plugin, no need to get this object anymore.
     *
     * @param context Rpc context
     * @return the OpenTracing tracer object
     */
    @Deprecated
    public static Tracer getTracer(RpcContext context) {
        return (Tracer) context.getValueMap().get(RpcContextValueKeys.CTX_TRACER);
    }

    /**
     * Global method for getting link span information, for plugins that need link information.
     *
     * @param context TRPC call context
     * @return span context
     */
    public static SpanContext getParentSpan(RpcContext context) {
        return (SpanContext) context.getValueMap().get(RpcContextValueKeys.CTX_TELEMETRY_TRACE_SPAN);
    }

    /**
     * Global method for setting link span information, mainly provided for telemetry plugin settings.
     *
     * @param context TRPC call context
     * @param spanContext span context information
     */
    public static void setParentSpan(RpcContext context, SpanContext spanContext) {
        putValueMapValue(context, RpcContextValueKeys.CTX_TELEMETRY_TRACE_SPAN, spanContext);
    }


    @SuppressWarnings("unchecked")
    public static <T> T getValueMapValue(RpcContext context, String key) {
        return (T) context.getValueMap().get(key);
    }

    public static Object putValueMapValue(RpcContext context, String key, Object value) {
        return context.getValueMap().put(key, value);
    }

    public static String getRequestAttachValue(RpcContext context, String key) {
        return getStringAttachValue(context.getReqAttachMap(), key);
    }

    public static Object putRequestAttachValue(RpcContext context, String key, String value) {
        return putStringAttachValue(context.getReqAttachMap(), key, value);
    }

    public static Object putRequestAttachValue(RpcContext context, String key, byte[] value) {
        return putStringAttachValue(context.getReqAttachMap(), key, value);
    }

    public static String getAttachValue(Request request, String key) {
        return getStringAttachValue(request.getAttachments(), key);
    }

    public static String getAttachValue(Response response, String key) {
        return getStringAttachValue(response.getAttachments(), key);
    }


    public static Object putAttachValue(Request request, String key, String value) {
        return putStringAttachValue(request.getAttachments(), key, value);
    }

    public static Object putAttachValue(Request request, String key, byte[] value) {
        return putStringAttachValue(request.getAttachments(), key, value);
    }

    public static Object putAttachValue(Response response, String key, String value) {
        return putStringAttachValue(response.getAttachments(), key, value);
    }

    public static Object putAttachValue(Response response, String key, byte[] value) {
        return putStringAttachValue(response.getAttachments(), key, value);
    }

    public static String getResponseAttachValue(RpcContext context, String key) {
        return getStringAttachValue(context.getRspAttachMap(), key);
    }

    public static Object putResponseAttachValue(RpcContext context, String key, String value) {
        return putStringAttachValue(context.getRspAttachMap(), key, value);
    }

    public static Object putResponseAttachValue(RpcContext context, String key, byte[] value) {
        return putStringAttachValue(context.getRspAttachMap(), key, value);
    }

    private static String getStringAttachValue(Map<String, Object> map, String key) {
        if (map == null) {
            return null;
        }
        Object object = map.get(key);
        if (object == null) {
            return null;
        }
        if (object instanceof String) {
            return (String) object;
        }
        if (object instanceof byte[]) {
            return new String((byte[]) object, Charsets.UTF_8);
        }
        throw new IllegalArgumentException(
                "key[" + key + "](" + object.getClass() + ") is not string");
    }

    private static Object putStringAttachValue(Map<String, Object> map, String key, Object value) {
        Objects.requireNonNull(map, "map");
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(value, "value");
        if (value instanceof String) {
            return map.put(key, ((String) value).getBytes(Charsets.UTF_8));
        }
        if (value instanceof byte[]) {
            return map.put(key, value);
        }
        throw new IllegalArgumentException(
                "key[" + key + "](" + value.getClass() + ") is not string/bytes");
    }

    /**
     * Set extension dimension 3 to the context. If the valueMap in the context is null, the value will not be set.
     *
     * @param context the context
     * @param value the value to be set in the context
     */
    public static void setExtensionDimension(RpcContext context, String value) {
        putRequestAttachValue(context, RpcContextValueKeys.CTX_M007_EXT3, value);
    }

    /**
     * Get the value of extension dimension 3 from the context.
     *
     * @param context the context
     * @return the value of extension dimension 3
     */
    public static String getExtensionDimension(RpcContext context) {
        String attachValue = getRequestAttachValue(context, RpcContextValueKeys.CTX_M007_EXT3);
        return attachValue == null ? "" : attachValue;
    }

}
