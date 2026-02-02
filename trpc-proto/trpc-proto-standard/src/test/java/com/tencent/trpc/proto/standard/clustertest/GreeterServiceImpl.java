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

import com.google.protobuf.ByteString;
import com.google.protobuf.TextFormat;
import com.tencent.trpc.core.rpc.CallInfo;
import com.tencent.trpc.core.rpc.RpcServerContext;
import com.tencent.trpc.core.utils.Charsets;
import com.tencent.trpc.proto.standard.common.HelloRequestProtocol;
import org.junit.jupiter.api.Assertions;

public class GreeterServiceImpl implements GreeterService {

    @Override
    public HelloRequestProtocol.HelloResponse sayHello(RpcServerContext context,
            HelloRequestProtocol.HelloRequest request) {
        System.out.println(">>>[server]receive msg[" + TextFormat.shortDebugString(request) + "]");
        Assertions.assertEquals("reqDyeingKey", context.getDyeingKey());
        Assertions.assertEquals("reqAttachValue",
                new String((byte[]) context.getReqAttachMap().get("attachKey"), Charsets.UTF_8));

        CallInfo callInfo = context.getCallInfo();
        Assertions.assertEquals("trpc.calleeapp.calleeserver.calleeservice", callInfo.getCallee());
        Assertions.assertEquals("calleeapp", callInfo.getCalleeApp());
        Assertions.assertEquals("calleeserver", callInfo.getCalleeServer());
        Assertions.assertEquals("sayHello", callInfo.getCalleeMethod());
        Assertions.assertEquals("calleeservice", callInfo.getCalleeService());
        Assertions.assertEquals("trpc.callerapp.callerserver.callerservice", callInfo.getCaller());
        Assertions.assertEquals("callerapp", callInfo.getCallerApp());
        Assertions.assertEquals("callerserver", callInfo.getCallerServer());
        Assertions.assertEquals("", callInfo.getCallerMethod());
        Assertions.assertEquals("callerservice", callInfo.getCallerService());

        CallInfo clientCallInfo = context.newClientContext().getCallInfo();
        Assertions.assertEquals("trpc.calleeapp.calleeserver.calleeservice",
                clientCallInfo.getCaller());
        Assertions.assertEquals("calleeapp", clientCallInfo.getCallerApp());
        Assertions.assertEquals("calleeserver", clientCallInfo.getCallerServer());
        Assertions.assertEquals("sayHello", clientCallInfo.getCallerMethod());
        Assertions.assertEquals("calleeservice", clientCallInfo.getCallerService());

        String message = request.getMessage().toStringUtf8();
        HelloRequestProtocol.HelloResponse.Builder rspBuilder =
                HelloRequestProtocol.HelloResponse.newBuilder();
        rspBuilder.setMessage(ByteString.copyFromUtf8(message));
        context.getRspAttachMap().put("attachKey", "rspAttachValue".getBytes());
        System.out
                .println(">>>[server]return msg[" + TextFormat.shortDebugString(rspBuilder) + "]");
        return rspBuilder.build();
    }
}
