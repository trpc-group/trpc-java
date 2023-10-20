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

package com.tencent.trpc.proto.http.server;


import static org.junit.Assert.assertEquals;
import static org.powermock.api.mockito.PowerMockito.doReturn;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

import com.tencent.trpc.core.rpc.RpcInvocation;
import com.tencent.trpc.core.rpc.common.RpcMethodInfo;
import com.tencent.trpc.proto.http.common.HttpConstants;
import javax.servlet.http.HttpServletRequest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;


@PowerMockIgnore({"javax.management.*"})
@RunWith(PowerMockRunner.class)
@PrepareForTest(AbstractHttpExecutor.class)
public class AbstractHttpExecutorTest {


    @Test
    public void buildRpcInvocation_shouldSuccess() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        RpcMethodInfo methodInfo = mock(RpcMethodInfo.class);
        AbstractHttpExecutor abstractHttpExecutor = mock(AbstractHttpExecutor.class);
        doReturn(null).when(abstractHttpExecutor, "parseRpcParams", request, methodInfo);
        doReturn("trpc.demo.server").when(request).getAttribute(HttpConstants.REQUEST_ATTRIBUTE_TRPC_SERVICE);
        doReturn("hello").when(request).getAttribute(HttpConstants.REQUEST_ATTRIBUTE_TRPC_METHOD);
        when(abstractHttpExecutor, "buildRpcInvocation", request, methodInfo).thenCallRealMethod();
        RpcInvocation rpcInvocation = Whitebox.invokeMethod(abstractHttpExecutor, "buildRpcInvocation", request,
                methodInfo);
        assertEquals(rpcInvocation.getFunc(), "/trpc.demo.server/hello");
    }
}