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

package com.tencent.trpc.core.rpc.common;

import static org.junit.Assert.assertEquals;

import com.tencent.trpc.core.common.RpcResult;
import com.tencent.trpc.core.rpc.GenericClient;
import com.tencent.trpc.core.rpc.InvokeMode;
import com.tencent.trpc.core.rpc.RpcClientContext;
import java.lang.reflect.Method;
import org.junit.Test;

public class RpcMethodInfoTest {

    @Test
    public void testNormalResult() throws NoSuchMethodException, SecurityException {
        Method method = GenericClient.class.getDeclaredMethod("invoke", RpcClientContext.class, byte[].class);
        RpcMethodInfo methodInfo = new RpcMethodInfo(GenericClient.class, method);
        assertEquals(methodInfo.getServiceInterface(), GenericClient.class);
        assertEquals(methodInfo.getMethod(), method);
        assertEquals(methodInfo.getParamsTypes()[0], RpcClientContext.class);
        assertEquals(methodInfo.getReturnType(), byte[].class);
        assertEquals(methodInfo.getServiceInterface(), GenericClient.class);
        assertEquals(InvokeMode.SYNC, methodInfo.getInvokeMode());
        assertEquals(method.getGenericReturnType(), methodInfo.getActualReturnType());
    }

    @Test
    public void testCommonResult() throws NoSuchMethodException, SecurityException {
        Method method = CommonResultClient.class.getDeclaredMethod("hello");
        RpcMethodInfo methodInfo = new RpcMethodInfo(CommonResultClient.class, method);
        assertEquals(methodInfo.getServiceInterface(), CommonResultClient.class);
        assertEquals(methodInfo.getMethod(), method);
        assertEquals(methodInfo.getReturnType(), RpcResult.class);
        assertEquals(methodInfo.getInvokeMode(), InvokeMode.SYNC);
        assertEquals(methodInfo.getActualReturnType(), Object.class);
    }
}
