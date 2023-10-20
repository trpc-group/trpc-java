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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.tencent.trpc.core.rpc.RpcContext;
import com.tencent.trpc.core.rpc.Request;
import com.tencent.trpc.core.rpc.Response;
import com.tencent.trpc.core.rpc.RpcClientContext;
import com.tencent.trpc.core.rpc.RpcContextValueKeys;
import com.tencent.trpc.core.rpc.RpcServerContext;
import com.tencent.trpc.core.rpc.def.DefRequest;
import com.tencent.trpc.core.rpc.def.DefResponse;
import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.tag.Tag;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;

public class RpcContextUtilsTest {

    @Test
    public void testGetSpan() {
        RpcContext ctx = new RpcClientContext();
        assertNull(RpcContextUtils.getSpan(ctx));

        Span span = new Span() {
            @Override
            public SpanContext context() {
                return null;
            }

            @Override
            public Span setTag(String s, String s1) {
                return null;
            }

            @Override
            public Span setTag(String s, boolean b) {
                return null;
            }

            @Override
            public Span setTag(String s, Number number) {
                return null;
            }

            @Override
            public <T> Span setTag(Tag<T> tag, T t) {
                return null;
            }

            @Override
            public Span log(Map<String, ?> map) {
                return null;
            }

            @Override
            public Span log(long l, Map<String, ?> map) {
                return null;
            }

            @Override
            public Span log(String s) {
                return null;
            }

            @Override
            public Span log(long l, String s) {
                return null;
            }

            @Override
            public Span setBaggageItem(String s, String s1) {
                return null;
            }

            @Override
            public String getBaggageItem(String s) {
                return null;
            }

            @Override
            public Span setOperationName(String s) {
                return null;
            }

            @Override
            public void finish() {
            }

            @Override
            public void finish(long l) {
            }
        };
        RpcContextUtils.putValueMapValue(ctx, RpcContextValueKeys.CTX_TRACE_SPAN, span);
        Assert.assertEquals(span, RpcContextUtils.getSpan(ctx));
    }

    @Test
    public void testGetRequestAttachValue() {
        RpcContext context = new RpcServerContext();
        assertNull(RpcContextUtils.getRequestAttachValue(context, "key"));
        context.getReqAttachMap().put("key", "value");
        assertEquals("value", RpcContextUtils.getRequestAttachValue(context, "key"));
    }

    @Test
    public void testPutRequestAttachValue() {
        RpcContext context = new RpcServerContext();
        Object value = RpcContextUtils.putRequestAttachValue(context, "key1", "value1");
        assertNull(value);
        Object value1 = RpcContextUtils.putRequestAttachValue(context, "key1", "value2");
        assertEquals("value1", new String((byte[]) value1));
    }

    @Test
    public void testPutRequestByteAttachValue() {
        RpcContext context = new RpcServerContext();
        Object value = RpcContextUtils
                .putRequestAttachValue(context, "key1", "value1".getBytes(Charsets.UTF_8));
        assertNull(value);
        Object value1 = RpcContextUtils
                .putRequestAttachValue(context, "key1", "value2".getBytes(Charsets.UTF_8));
        assertEquals("value1", new String((byte[]) value1));
    }

    @Test
    public void testReqGetAttachValue() {
        Request request = new DefRequest();
        assertNull(RpcContextUtils.getAttachValue(request, "key"));
        request.putAttachment("key", "value");
        assertEquals("value", RpcContextUtils.getAttachValue(request, "key"));
    }

    @Test
    public void testReqPutAttachValue() {
        Request request = new DefRequest();
        assertNull(RpcContextUtils.putAttachValue(request, "key", "value"));
        assertEquals("value",
                new String((byte[]) RpcContextUtils.putAttachValue(request, "key", "value1")));
    }

    @Test
    public void testReqPutByteAttachValue() {
        Request request = new DefRequest();
        assertNull(RpcContextUtils.putAttachValue(request, "key",
                "value".getBytes(Charsets.UTF_8)));
        assertEquals("value",
                new String((byte[]) RpcContextUtils.putAttachValue(request, "key",
                        "value1".getBytes(Charsets.UTF_8))));
    }

    @Test
    public void testGetResponseAttachValue() {
        RpcContext context = new RpcServerContext();
        assertNull(RpcContextUtils.getResponseAttachValue(context, "key"));
        context.getRspAttachMap().put("key", "value");
        assertEquals("value", RpcContextUtils.getResponseAttachValue(context, "key"));
    }

    @Test
    public void testPutResponseAttachValue() {
        RpcContext context = new RpcServerContext();
        assertNull(RpcContextUtils.putResponseAttachValue(context, "key", "value"));
        assertEquals("value", RpcContextUtils.getResponseAttachValue(context, "key"));
        assertEquals("value", new String(
                (byte[]) RpcContextUtils.putResponseAttachValue(context, "key", "value1")));
        assertEquals("value1", RpcContextUtils.getResponseAttachValue(context, "key"));
    }

    @Test
    public void testPutResponseByteAttachValue() {
        RpcContext context = new RpcServerContext();
        assertNull(RpcContextUtils
                .putResponseAttachValue(context, "key", "value".getBytes(Charsets.UTF_8)));
        assertEquals("value", RpcContextUtils.getResponseAttachValue(context, "key"));
        assertEquals("value", new String(
                (byte[]) RpcContextUtils.putResponseAttachValue(context, "key",
                        "value1".getBytes(Charsets.UTF_8))));
        assertEquals("value1", RpcContextUtils.getResponseAttachValue(context, "key"));
    }

    @Test
    public void testRspGetAttachValue() {
        Response response = new DefResponse();
        assertNull(RpcContextUtils.getAttachValue(response, "key"));
        response.putAttachment("key", "value");
        assertEquals("value", RpcContextUtils.getAttachValue(response, "key"));
    }

    @Test
    public void testRspPutAttachValue() {
        Response response = new DefResponse();
        assertNull(RpcContextUtils.putAttachValue(response, "key", "value"));
        assertEquals("value",
                new String((byte[]) RpcContextUtils.putAttachValue(response, "key", "value1")));
    }

    @Test
    public void testRspPutByteAttachValue() {
        Response response = new DefResponse();
        assertNull(RpcContextUtils.putAttachValue(response, "key",
                "value".getBytes(Charsets.UTF_8)));
        assertEquals("value",
                new String((byte[]) RpcContextUtils.putAttachValue(response, "key",
                        "value1".getBytes(Charsets.UTF_8))));
    }

    @Test
    public void testSetExtensionDimension() {
        RpcContext context = new RpcServerContext();
        String extDimension = "test";
        RpcContextUtils.setExtensionDimension(context, extDimension);
        String extDimensionFromContext = RpcContextUtils.getExtensionDimension(context);
        Assert.assertEquals(extDimension, extDimensionFromContext);
    }

    @Test
    public void testGetExtensionDimension() {
        RpcContext context = new RpcClientContext();
        String extDimensionFromContext = RpcContextUtils.getExtensionDimension(context);
        Assert.assertEquals("", extDimensionFromContext);
        String extDimension = "test";
        RpcContextUtils.putRequestAttachValue(context, RpcContextValueKeys.CTX_M007_EXT3, extDimension);
        extDimensionFromContext = RpcContextUtils.getExtensionDimension(context);
        Assert.assertEquals(extDimension, extDimensionFromContext);
    }
}