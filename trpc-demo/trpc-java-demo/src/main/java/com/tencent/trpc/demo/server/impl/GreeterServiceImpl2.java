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

package com.tencent.trpc.demo.server.impl;

import com.google.protobuf.TextFormat;
import com.tencent.trpc.core.rpc.RpcContext;
import com.tencent.trpc.demo.proto.GreeterService2API;
import com.tencent.trpc.demo.proto.HelloRequestProtocol;

public class GreeterServiceImpl2 implements GreeterService2API {

    @Override
    public HelloRequestProtocol.HelloResponse sayHi(RpcContext context, HelloRequestProtocol.HelloRequest request) {
        String thread = Thread.currentThread().getName();
        System.out.println(thread + ">>>[server]receive msg[" + TextFormat.shortDebugString(request) + "]");
        String message = request.getMessage();
        HelloRequestProtocol.HelloResponse.Builder rspBuilder = HelloRequestProtocol.HelloResponse.newBuilder();
        rspBuilder.setMessage("Hi " + message);
        System.out.println(thread + ">>>[server]return msg[" + TextFormat.shortDebugString(rspBuilder) + "]");
        return rspBuilder.build();
    }

}
