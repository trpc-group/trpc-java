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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.collect.ImmutableMap;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.Test;

public class RpcContextTest {

    @Test
    public void test() {
        RpcClientContext clientCtx = new RpcClientContext();
        clientCtx.setOneWay(true);
        assertTrue(clientCtx.isOneWay);
        clientCtx.setHashVal("hashVal");
        clientCtx.setRpcMethodName("rpcMethodName");
        clientCtx.setRpcServiceName("rpcServiceName");
        clientCtx.setTimeoutMills(10000);
        clientCtx.setDyeingKey("a");
        assertTrue(clientCtx.isDyeing());
        assertFalse(clientCtx.isServerContext());
        CallInfo callInfo = new CallInfo();
        callInfo.setCallee("callee");
        clientCtx.setCallInfo(callInfo);
        assertEquals(clientCtx.getHashVal(), "hashVal");
        assertEquals(clientCtx.getRpcMethodName(), "rpcMethodName");
        assertEquals(clientCtx.getRpcServiceName(), "rpcServiceName");
        assertEquals(clientCtx.getTimeoutMills(), 10000);
        assertEquals(clientCtx.getCallInfo().getCallee(), "callee");

        clientCtx.getReqAttachMap()
                .putAll(new ConcurrentHashMap<String, Object>(ImmutableMap.of("aa", "bb")));
        clientCtx.getRspAttachMap()
                .putAll(new ConcurrentHashMap<String, Object>(ImmutableMap.of("aa", "cc")));

        assertEquals(clientCtx.getReqAttachMap().get("aa"), "bb");
        assertEquals(clientCtx.getRspAttachMap().get("aa"), "cc");
        clientCtx.toString();
        assertNotNull(clientCtx.toClientContext());
        RpcClientContext clone = clientCtx.clone();
        clone.setHashVal("hashVal");
        clone.setRpcMethodName("rpcMethodName");
        clone.setRpcServiceName("rpcServiceName");
        clone.setTimeoutMills(10000);
        assertEquals(clone.getCallInfo().getCallee(), "callee");

        RpcContext serverContext = new RpcServerContext();
        assertNotNull(serverContext.toServerContext());
        assertTrue(serverContext.isServerContext());
    }
}
