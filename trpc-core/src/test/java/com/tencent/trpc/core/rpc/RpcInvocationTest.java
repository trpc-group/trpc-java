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

package com.tencent.trpc.core.rpc;

import com.tencent.trpc.core.rpc.common.RpcMethodInfo;
import com.tencent.trpc.core.utils.RpcUtilsTest.SimpleGenericClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Created by youngwwang on 2020/6/2.
 */
public class RpcInvocationTest {


    @Test
    public void testIsGeneric() throws NoSuchMethodException {
        RpcInvocation invocation = new RpcInvocation();
        invocation.setFirstArgument(new Object());
        invocation.setFunc("/a/b");
        Assertions.assertNotNull(invocation.getFirstArgument());
        Assertions.assertNull(invocation.getInvokeMode());
        Assertions.assertEquals("/a/b", invocation.getFunc());
        Assertions.assertFalse(invocation.isGeneric());

        invocation.setRpcMethodInfo(
                new RpcMethodInfo(Request.class, Request.class.getMethod("getRequestId")));

        Assertions.assertFalse(invocation.isGeneric());

        invocation.setRpcMethodInfo(new RpcMethodInfo(SimpleGenericClient.class,
                SimpleGenericClient.class
                        .getMethod("asyncInvoke", RpcClientContext.class, byte[].class)));

        Assertions.assertTrue(invocation.isGeneric());

    }
}
