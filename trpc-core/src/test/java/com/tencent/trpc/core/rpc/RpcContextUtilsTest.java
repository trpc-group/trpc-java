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

package com.tencent.trpc.core.rpc;

import static org.junit.Assert.assertEquals;

import com.tencent.trpc.core.rpc.def.DefRequest;
import com.tencent.trpc.core.rpc.def.DefResponse;
import com.tencent.trpc.core.telemetry.SpanContext;
import com.tencent.trpc.core.utils.Charsets;
import com.tencent.trpc.core.utils.RpcContextUtils;
import io.opentracing.noop.NoopSpan;
import org.junit.Test;

public class RpcContextUtilsTest {

    @Test
    public void testRequest() {
        Request request = new DefRequest();
        RpcContextUtils.putAttachValue(request, "string", "string_value");
        RpcContextUtils.putAttachValue(request, "byte", "byte_value".getBytes(Charsets.UTF_8));

        assertEquals("string_value", RpcContextUtils.getAttachValue(request, "string"));
        assertEquals("byte_value", RpcContextUtils.getAttachValue(request, "byte"));
    }

    @Test
    public void testResponse() {
        Response response = new DefResponse();
        RpcContextUtils.putAttachValue(response, "string", "string_value");
        RpcContextUtils.putAttachValue(response, "byte", "byte_value".getBytes(Charsets.UTF_8));

        assertEquals("string_value", RpcContextUtils.getAttachValue(response, "string"));
        assertEquals("byte_value", RpcContextUtils.getAttachValue(response, "byte"));
    }

    @Test
    public void testContext() {
        RpcContext context = new RpcClientContext();
        RpcContextUtils.putRequestAttachValue(context, "string", "string_value");
        RpcContextUtils.putRequestAttachValue(context, "byte", "byte_value".getBytes(Charsets.UTF_8));

        assertEquals("string_value", RpcContextUtils.getRequestAttachValue(context, "string"));
        assertEquals("byte_value", RpcContextUtils.getRequestAttachValue(context, "byte"));

        RpcContextUtils.putResponseAttachValue(context, "string", "string_value1");
        RpcContextUtils.putResponseAttachValue(context, "byte", "byte_value1".getBytes(Charsets.UTF_8));

        assertEquals("string_value1", RpcContextUtils.getResponseAttachValue(context, "string"));
        assertEquals("byte_value1", RpcContextUtils.getResponseAttachValue(context, "byte"));
    }

    @Test
    public void testGetSpan() {
        RpcContext context = new RpcServerContext();
        RpcContextUtils.putValueMapValue(context, RpcContextValueKeys.CTX_TRACE_SPAN, NoopSpan.INSTANCE);
        assertEquals(NoopSpan.INSTANCE, RpcContextUtils.getSpan(context));

        RpcContextUtils.setParentSpan(context, SpanContext.INVALID);
        assertEquals(SpanContext.INVALID, RpcContextUtils.getParentSpan(context));
    }
}
