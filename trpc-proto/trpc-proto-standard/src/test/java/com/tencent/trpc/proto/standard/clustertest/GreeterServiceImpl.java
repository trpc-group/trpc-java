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

package com.tencent.trpc.proto.standard.clustertest;

import com.google.protobuf.ByteString;
import com.google.protobuf.TextFormat;
import com.tencent.trpc.core.rpc.CallInfo;
import com.tencent.trpc.core.rpc.RpcServerContext;
import com.tencent.trpc.core.utils.Charsets;
import com.tencent.trpc.proto.standard.common.HelloRequestProtocol;
import org.junit.Assert;

public class GreeterServiceImpl implements GreeterService {

    @Override
    public HelloRequestProtocol.HelloResponse sayHello(RpcServerContext context,
            HelloRequestProtocol.HelloRequest request) {
        System.out.println(">>>[server]receive msg[" + TextFormat.shortDebugString(request) + "]");
        Assert.assertEquals("reqDyeingKey", context.getDyeingKey());
        Assert.assertEquals("reqAttachValue",
                new String((byte[]) context.getReqAttachMap().get("attachKey"), Charsets.UTF_8));

        CallInfo callInfo = context.getCallInfo();
        Assert.assertEquals("trpc.calleeapp.calleeserver.calleeservice", callInfo.getCallee());
        Assert.assertEquals("calleeapp", callInfo.getCalleeApp());
        Assert.assertEquals("calleeserver", callInfo.getCalleeServer());
        Assert.assertEquals("sayHello", callInfo.getCalleeMethod());
        Assert.assertEquals("calleeservice", callInfo.getCalleeService());
        Assert.assertEquals("trpc.callerapp.callerserver.callerservice", callInfo.getCaller());
        Assert.assertEquals("callerapp", callInfo.getCallerApp());
        Assert.assertEquals("callerserver", callInfo.getCallerServer());
        Assert.assertEquals("", callInfo.getCallerMethod());
        Assert.assertEquals("callerservice", callInfo.getCallerService());

        CallInfo clientCallInfo = context.newClientContext().getCallInfo();
        Assert.assertEquals("trpc.calleeapp.calleeserver.calleeservice",
                clientCallInfo.getCaller());
        Assert.assertEquals("calleeapp", clientCallInfo.getCallerApp());
        Assert.assertEquals("calleeserver", clientCallInfo.getCallerServer());
        Assert.assertEquals("sayHello", clientCallInfo.getCallerMethod());
        Assert.assertEquals("calleeservice", clientCallInfo.getCallerService());

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
