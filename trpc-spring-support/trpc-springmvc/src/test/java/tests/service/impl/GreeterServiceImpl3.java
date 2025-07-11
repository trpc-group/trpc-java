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
import tests.service.GreeterService3;

@Service
public class GreeterServiceImpl3 implements GreeterService3 {

    @Override
    public HelloRequestProtocol.HelloResponse sayHello(RpcServerContext context,
            HelloRequestProtocol.HelloRequest request) {
        System.out.println(">>>[server]receive msg[" + TextFormat.shortDebugString(request) + "]");
        System.out.println(">>>[server]received transInfo: " + context.getReqAttachMap());
        String message = request.getMessage();
        HelloRequestProtocol.HelloResponse.Builder rspBuilder =
                HelloRequestProtocol.HelloResponse.newBuilder();
        rspBuilder.setMessage(message);
        System.out
                .println(">>>[server]return msg[" + TextFormat.shortDebugString(rspBuilder) + "]");

        try {
            Thread.sleep(1000);  // for timeout test
        } catch (Exception ignored) {
            ignored.printStackTrace();
        }

        return rspBuilder.build();
    }

}
