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

package com.tencent.trpc.proto.standard.clustertest;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.tencent.trpc.core.filter.spi.Filter;
import com.tencent.trpc.core.rpc.CallInfo;
import com.tencent.trpc.core.rpc.Invoker;
import com.tencent.trpc.core.rpc.Request;
import com.tencent.trpc.core.rpc.Response;
import com.tencent.trpc.proto.standard.common.TRPCProtocol.TrpcMessageType;
import java.util.concurrent.CompletionStage;

public class ServerFilterTest implements Filter {

    @Override
    public CompletionStage<Response> filter(Invoker<?> filterChain, Request req) {
        assertEquals(100 | TrpcMessageType.TRPC_DYEING_MESSAGE_VALUE,
                req.getMeta().getMessageType());

        CallInfo callInfo = req.getMeta().getCallInfo();
        assertEquals("calleeapp", callInfo.getCalleeApp());
        assertEquals("calleeserver", callInfo.getCalleeServer());
        assertEquals("sayHello", callInfo.getCalleeMethod());
        assertEquals("calleeservice", callInfo.getCalleeService());
        assertEquals("callerapp", callInfo.getCallerApp());
        assertEquals("callerserver", callInfo.getCallerServer());
        assertEquals("", callInfo.getCallerMethod());
        assertEquals("callerservice", callInfo.getCallerService());

        return filterChain.invoke(req).thenApply(r -> {
            r.getMeta().addMessageType(200);
            return r;
        });
    }

}
