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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

import com.tencent.trpc.core.rpc.RpcServerContext.NewClientContextOptions;
import com.tencent.trpc.core.utils.RpcContextUtils;
import java.nio.charset.StandardCharsets;
import org.junit.Test;

public class RpcServerContextTest {

    @Test
    public void test() {
        RpcServerContext context = new RpcServerContext();
        context.getCallInfo().setCalleeApp("serverApp");
        context.getCallInfo().setCalleeServer("server");
        context.getCallInfo().setCalleeMethod("method");
        context.getCallInfo().setCalleeService("service");
        RpcContextUtils.putValueMapValue(context, "key", "value");
        RpcContextUtils.putRequestAttachValue(context, "key", "value");
        RpcClientContext newClientContext = context.newClientContext();
        assertEquals("serverApp", newClientContext.getCallInfo().getCallerApp());
        assertEquals("server", newClientContext.getCallInfo().getCallerServer());
        assertEquals("method", newClientContext.getCallInfo().getCallerMethod());
        assertEquals("service", newClientContext.getCallInfo().getCallerService());
        assertEquals("value", RpcContextUtils.getRequestAttachValue(newClientContext, "key"));
        assertEquals("value", RpcContextUtils.getValueMapValue(newClientContext, "key"));

        newClientContext = context.clone().newClientContext();
        assertEquals("serverApp", newClientContext.getCallInfo().getCallerApp());
        assertEquals("server", newClientContext.getCallInfo().getCallerServer());
        assertEquals("method", newClientContext.getCallInfo().getCallerMethod());
        assertEquals("service", newClientContext.getCallInfo().getCallerService());
        assertEquals("value", RpcContextUtils.getRequestAttachValue(newClientContext, "key"));
        assertEquals("value", RpcContextUtils.getValueMapValue(newClientContext, "key"));

        newClientContext = context.newClientContext(
                NewClientContextOptions.newInstance().setCloneCallInfo(true)
                        .setCloneReqAttachMap(true));
        assertEquals("serverApp", newClientContext.getCallInfo().getCallerApp());
        assertEquals("server", newClientContext.getCallInfo().getCallerServer());
        assertEquals("method", newClientContext.getCallInfo().getCallerMethod());
        assertEquals("service", newClientContext.getCallInfo().getCallerService());
        assertEquals("value", RpcContextUtils.getRequestAttachValue(newClientContext, "key"));
        assertEquals("value", RpcContextUtils.getValueMapValue(newClientContext, "key"));
    }

    /**
     * Test attachment
     */
    @Test
    public void testAttachment() {
        RpcServerContext context = new RpcServerContext();
        byte[] request = "request".getBytes(StandardCharsets.UTF_8);
        byte[] response = "response".getBytes(StandardCharsets.UTF_8);
        context.setRequestUncodecDataSegment(request);
        context.setResponseUncodecDataSegment(response);
        NewClientContextOptions options = NewClientContextOptions.newInstance();
        assertFalse(options.isCloneRequestUncodecDataSegment());
        assertFalse(options.isCloneResponseUncodecDataSegment());
        RpcClientContext clientContext = context.newClientContext(
                options.setCloneRequestUncodecDataSegment(true).setCloneResponseUncodecDataSegment(true));
        assertEquals(request, clientContext.getRequestUncodecDataSegment());
        assertEquals(response, clientContext.getResponseUncodecDataSegment());

        clientContext = context.newClientContext(
                options.setCloneRequestUncodecDataSegment(false).setCloneResponseUncodecDataSegment(false));
        assertNull(clientContext.getRequestUncodecDataSegment());
        assertNull(clientContext.getResponseUncodecDataSegment());
    }
}
