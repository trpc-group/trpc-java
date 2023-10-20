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

package com.tencent.trpc.container.demo;

import com.google.protobuf.ByteString;
import com.tencent.trpc.container.demo.HelloRequestProtocol.HelloRequest;
import com.tencent.trpc.container.demo.HelloRequestProtocol.HelloResponse;
import com.tencent.trpc.core.logger.Logger;
import com.tencent.trpc.core.logger.LoggerFactory;
import com.tencent.trpc.core.rpc.RpcContext;

public class GreeterServiceImp implements GreeterService {

    private static final Logger logger = LoggerFactory.getLogger(GreeterServiceImp.class);

    public HelloResponse sayHello(RpcContext context, HelloRequest request) {
        String msg = request.getMessage().toStringUtf8();
        HelloResponse.Builder response = HelloResponse.newBuilder();
        response.setMessage(ByteString.copyFromUtf8(msg + "-"));
        logger.info("response.getMessage().toString(): " + response.getMessage().toString());
        return response.build();
    }

}
