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

import com.tencent.trpc.core.rpc.common.RpcMethodInfo;
import com.tencent.trpc.core.utils.RpcUtilsTest.SimpleGenericClient;
import org.junit.Assert;
import org.junit.Test;

/**
 * Created by youngwwang on 2020/6/2.
 */
public class RpcInvocationTest {


    @Test
    public void testIsGeneric() throws NoSuchMethodException {
        RpcInvocation invocation = new RpcInvocation();
        invocation.setFirstArgument(new Object());
        invocation.setFunc("/a/b");
        Assert.assertNotNull(invocation.getFirstArgument());
        Assert.assertNull(invocation.getInvokeMode());
        Assert.assertEquals("/a/b", invocation.getFunc());
        Assert.assertFalse(invocation.isGeneric());

        invocation.setRpcMethodInfo(
                new RpcMethodInfo(Request.class, Request.class.getMethod("getRequestId")));

        Assert.assertFalse(invocation.isGeneric());

        invocation.setRpcMethodInfo(new RpcMethodInfo(SimpleGenericClient.class,
                SimpleGenericClient.class
                        .getMethod("asyncInvoke", RpcClientContext.class, byte[].class)));

        Assert.assertTrue(invocation.isGeneric());

    }
}
