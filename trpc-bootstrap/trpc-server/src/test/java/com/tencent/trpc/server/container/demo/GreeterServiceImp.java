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

package com.tencent.trpc.server.container.demo;

import com.google.protobuf.ByteString;
import com.tencent.trpc.core.rpc.RpcContext;
import com.tencent.trpc.server.container.demo.HelloRequestProtocol.HelloRequest;
import com.tencent.trpc.server.container.demo.HelloRequestProtocol.HelloResponse;

public class GreeterServiceImp implements GreeterService {

    public HelloResponse sayHello(RpcContext context, HelloRequest request) {
        String msg = request.getMessage().toStringUtf8();
        HelloResponse.Builder response = HelloResponse.newBuilder();
        response.setMessage(ByteString.copyFromUtf8("echo:::::::::" + msg));
        System.out.println("response.getMessage().toString(): "
                + response.getMessage().toString());
        return response.build();
    }

}
