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

package com.tencent.trpc.proto.http.common;

import com.google.common.collect.Sets;
import com.tencent.trpc.core.rpc.RpcClientContext;
import com.tencent.trpc.core.rpc.RpcServerContext;
import com.tencent.trpc.core.rpc.RpcServerContext.NewClientContextOptions;
import com.tencent.trpc.proto.http.common.RpcServerContextWithHttp.NewClientContextOptionsWithHttp;
import java.util.Set;
import org.apache.http.HttpHeaders;
import org.junit.Assert;
import org.junit.Test;

/**
 * RpcServerContextWithHttp test cases
 */
public class RpcServerContextWithHttpTest {


    @Test
    public void testNewClientContext() {
        String removeKey = "test";
        Set<String> removeKeys = Sets.newHashSet(removeKey);
        Assert.assertNotNull(removeKeys);

        RpcServerContext rpcServerContext = new RpcServerContextWithHttp();
        rpcServerContext.getReqAttachMap().put(HttpHeaders.CONTENT_LENGTH, "10");
        rpcServerContext.getReqAttachMap().put(HttpHeaders.ACCEPT, "*/*");
        rpcServerContext.getReqAttachMap().put(HttpConstants.HTTP_HEADER_TRPC_MESSAGE_TYPE, "pb");
        rpcServerContext.getReqAttachMap().put(removeKey, removeKey);

        RpcClientContext context1 = rpcServerContext.newClientContext();
        Assert.assertFalse(context1.getReqAttachMap().containsKey(HttpHeaders.CONTENT_LENGTH));
        Assert.assertFalse(context1.getReqAttachMap().containsKey(HttpHeaders.ACCEPT));
        Assert.assertTrue(context1.getReqAttachMap().containsKey(HttpConstants.HTTP_HEADER_TRPC_MESSAGE_TYPE));
        Assert.assertTrue(context1.getReqAttachMap().containsKey(removeKey));

        RpcClientContext context2 = rpcServerContext.newClientContext(
                NewClientContextOptionsWithHttp.newInstance().setRemoveCommonHttpHeaders(false));
        Assert.assertFalse(context2.getReqAttachMap().containsKey(HttpHeaders.CONTENT_LENGTH));
        Assert.assertTrue(context2.getReqAttachMap().containsKey(HttpHeaders.ACCEPT));
        Assert.assertTrue(context2.getReqAttachMap().containsKey(HttpConstants.HTTP_HEADER_TRPC_MESSAGE_TYPE));
        Assert.assertTrue(context1.getReqAttachMap().containsKey("test"));

        RpcClientContext context3 = rpcServerContext.newClientContext(
                NewClientContextOptionsWithHttp.newInstance().setRemoveHttpHeaders(removeKeys));
        Assert.assertFalse(context3.getReqAttachMap().containsKey(HttpHeaders.CONTENT_LENGTH));
        Assert.assertFalse(context3.getReqAttachMap().containsKey(HttpHeaders.ACCEPT));
        Assert.assertTrue(context3.getReqAttachMap().containsKey(HttpConstants.HTTP_HEADER_TRPC_MESSAGE_TYPE));
        Assert.assertFalse(context3.getReqAttachMap().containsKey(removeKey));

        RpcClientContext context4 = rpcServerContext.newClientContext(NewClientContextOptions.newInstance());
        Assert.assertTrue(context4.getReqAttachMap().containsKey(HttpHeaders.CONTENT_LENGTH));
        Assert.assertTrue(context4.getReqAttachMap().containsKey(HttpHeaders.ACCEPT));
        Assert.assertTrue(context4.getReqAttachMap().containsKey(HttpConstants.HTTP_HEADER_TRPC_MESSAGE_TYPE));
        Assert.assertTrue(context4.getReqAttachMap().containsKey(removeKey));
    }

}
