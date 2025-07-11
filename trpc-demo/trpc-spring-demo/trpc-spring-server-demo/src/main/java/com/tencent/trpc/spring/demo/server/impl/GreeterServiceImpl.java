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

package com.tencent.trpc.spring.demo.server.impl;

import com.google.protobuf.TextFormat;
import com.tencent.trpc.core.logger.Logger;
import com.tencent.trpc.core.logger.LoggerFactory;
import com.tencent.trpc.core.rpc.RpcContext;
import com.tencent.trpc.demo.proto.GreeterServiceAPI;
import com.tencent.trpc.demo.proto.HelloRequestProtocol;
import org.springframework.stereotype.Service;

@Service
public class GreeterServiceImpl implements GreeterServiceAPI {

    private static final Logger logger = LoggerFactory.getLogger(GreeterServiceImpl.class);

    @Override
    public HelloRequestProtocol.HelloResponse sayHello(RpcContext context, HelloRequestProtocol.HelloRequest request) {
        logger.info(">>>[server]receive msg:{}", TextFormat.shortDebugString(request));
        String message = request.getMessage();
        HelloRequestProtocol.HelloResponse.Builder rspBuilder = HelloRequestProtocol.HelloResponse.newBuilder();
        rspBuilder.setMessage("Hello " + message);
        logger.info(">>>[server]return msg:{}", TextFormat.shortDebugString(rspBuilder));
        return rspBuilder.build();
    }

}
