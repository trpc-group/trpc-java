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

package com.tencent.trpc.proto.standard.common;

import com.google.protobuf.ByteString;
import com.tencent.trpc.core.rpc.RpcContext;

public class HelloServiceImpl implements HelloService {

    @Override
    public HelloRequestProtocol.HelloResponse sayHello(RpcContext context, HelloRequestProtocol.HelloRequest request) {
        HelloRequestProtocol.HelloResponse.Builder response = HelloRequestProtocol.HelloResponse.newBuilder();
        response.setMessage(ByteString.copyFromUtf8("hello response"));
        return response.build();
    }

    @Override
    public HelloRequestProtocol.HelloResponse doDefaultMethod(RpcContext context,
            HelloRequestProtocol.HelloRequest request) {
        HelloRequestProtocol.HelloResponse.Builder response = HelloRequestProtocol.HelloResponse.newBuilder();
        String resMsg = "this is default method";
        response.setMessage(ByteString.copyFromUtf8(resMsg));
        return response.build();
    }

    @Override
    public byte[] doGenericMethod(RpcContext context, byte[] request) {
        HelloRequestProtocol.HelloResponse.Builder response = HelloRequestProtocol.HelloResponse.newBuilder();
        response.setMessage(ByteString.copyFromUtf8("this is generic method"));
        return response.build().toByteArray();
    }
}
