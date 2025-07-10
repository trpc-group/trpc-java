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

package com.tencent.trpc.proto.standard.concurrenttest;

import com.google.protobuf.ByteString;
import com.tencent.trpc.core.rpc.RpcServerContext;
import com.tencent.trpc.proto.standard.common.HelloRequestProtocol;

public class ConcurrentTestServiceImpl implements ConcurrentTestService {

    @Override
    public HelloRequestProtocol.HelloResponse sayHello(RpcServerContext context,
            HelloRequestProtocol.HelloRequest request) {
        String message = request.getMessage().toStringUtf8();
        HelloRequestProtocol.HelloResponse.Builder rspBuilder = HelloRequestProtocol.HelloResponse.newBuilder();
        rspBuilder.setMessage(ByteString.copyFromUtf8(message));
        context.getRspAttachMap().putAll(context.getReqAttachMap());
        return rspBuilder.build();
    }
}
