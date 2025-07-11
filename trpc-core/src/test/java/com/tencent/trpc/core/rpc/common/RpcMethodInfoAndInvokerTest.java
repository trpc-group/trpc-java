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

package com.tencent.trpc.core.rpc.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import com.tencent.trpc.core.rpc.GenericClient;
import com.tencent.trpc.core.rpc.RpcClientContext;
import java.lang.reflect.Method;
import org.junit.Test;

public class RpcMethodInfoAndInvokerTest {

    @Test
    public void test() throws NoSuchMethodException, SecurityException {

        Method method = GenericClient.class.getDeclaredMethod("invoke", RpcClientContext.class, byte[].class);
        RpcMethodInfo methodInfo = new RpcMethodInfo(GenericClient.class, method);
        RpcMethodInfoAndInvoker invoker = new RpcMethodInfoAndInvoker(methodInfo, null, null);
        invoker.setMethodInfo(methodInfo);
        invoker.setInvoker(null);
        invoker.setMethodRouterKey(new MethodRouterKey("a", "b"));
        assertNull(invoker.getInvoker());
        assertEquals(invoker.getMethodInfo(), methodInfo);
        assertEquals("a", invoker.getMethodRouterKey().getRpcServiceName());
        assertEquals("b", invoker.getMethodRouterKey().getRpcMethodName());
        RpcMethodInfoAndInvoker rpcMethodInfoAndInvoker = new RpcMethodInfoAndInvoker();
        assertNotNull(rpcMethodInfoAndInvoker);
    }
}
