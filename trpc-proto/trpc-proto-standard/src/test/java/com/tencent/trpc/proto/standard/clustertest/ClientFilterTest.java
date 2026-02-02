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
import java.util.concurrent.CompletionStage;
import org.junit.jupiter.api.Assertions;

public class ClientFilterTest implements Filter {

    @Override
    public CompletionStage<Response> filter(Invoker<?> filterChain, Request req) {
        req.getMeta().addMessageType(100);
        CallInfo callInfo = req.getMeta().getCallInfo();
        Assertions.assertEquals("calleeapp", callInfo.getCalleeApp());
        Assertions.assertEquals("calleeserver", callInfo.getCalleeServer());
        Assertions.assertEquals("calleemethod", callInfo.getCalleeMethod());
        Assertions.assertEquals("calleeservice", callInfo.getCalleeService());
        Assertions.assertEquals("callerapp", callInfo.getCallerApp());
        Assertions.assertEquals("callerserver", callInfo.getCallerServer());
        Assertions.assertEquals("callermethod", callInfo.getCallerMethod());
        Assertions.assertEquals("callerservice", callInfo.getCallerService());
        return filterChain.invoke(req).thenApply(r -> {
            assertEquals(200, r.getMeta().getMessageType());
            return r;
        });
    }

}
