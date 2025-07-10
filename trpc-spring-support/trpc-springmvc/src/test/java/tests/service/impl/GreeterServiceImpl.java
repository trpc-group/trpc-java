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

package tests.service.impl;

import com.google.protobuf.TextFormat;
import com.tencent.trpc.core.rpc.RpcServerContext;
import org.springframework.stereotype.Service;
import tests.proto.HelloRequestProtocol;
import tests.service.GreeterService;

@Service
public class GreeterServiceImpl implements GreeterService {

    @Override
    public HelloRequestProtocol.HelloResponse sayHello(RpcServerContext context,
            HelloRequestProtocol.HelloRequest request) {
        System.out.println(">>>[server]receive msg[" + TextFormat.shortDebugString(request) + "]");
        String message = request.getMessage();
        HelloRequestProtocol.HelloResponse.Builder rspBuilder =
                HelloRequestProtocol.HelloResponse.newBuilder();
        rspBuilder.setMessage(message);
        System.out
                .println(">>>[server]return msg[" + TextFormat.shortDebugString(rspBuilder) + "]");
        return rspBuilder.build();
    }

}
