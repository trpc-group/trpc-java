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

package tests.service.impl1;

import com.tencent.trpc.core.logger.Logger;
import com.tencent.trpc.core.logger.LoggerFactory;
import com.tencent.trpc.core.rpc.RpcContext;
import tests.service.GreeterService;
import tests.service.HelloRequestProtocol.HelloRequest;
import tests.service.HelloRequestProtocol.HelloResponse;
import tests.service.TestBeanConvertWithGetMethodReq;
import tests.service.TestBeanConvertWithGetMethodRsp;

public class GreeterServiceImpl1 implements GreeterService {

    private static final Logger logger = LoggerFactory.getLogger(GreeterServiceImpl1.class);

    @Override
    public HelloResponse sayHello(RpcContext context, HelloRequest request) {
        String message = request.getMessage();
        logger.info("got hello request, message is '{}'", message);
        HelloResponse.Builder rspBuilder = HelloResponse.newBuilder();
        rspBuilder.setMessage("Hello, " + message);
        return rspBuilder.build();
    }

    @Override
    public String sayBlankHello(RpcContext context, HelloRequest request) {
        logger.info("got hello request, message is blank");
        return "";
    }

    @Override
    public TestBeanConvertWithGetMethodRsp sayHelloNonPbType(RpcContext context,
            TestBeanConvertWithGetMethodReq request) {
        String message = request.getMessage();
        int status = request.getStatus();
        String[] comments = request.getComments();
        logger.info("got hello request, message is '{}', status is {}", message, status);
        return new TestBeanConvertWithGetMethodRsp("Hello," + message, status, comments);
    }
}
