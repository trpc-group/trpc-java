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

package com.tencent.trpc.spring.cloud.gateway.filter.test.server;

import com.tencent.trpc.core.logger.Logger;
import com.tencent.trpc.core.logger.LoggerFactory;
import com.tencent.trpc.core.rpc.RpcContext;
import com.tencent.trpc.core.rpc.RpcServerContext;
import com.tencent.trpc.spring.cloud.gateway.filter.test.server.Hello.HelloReq;
import com.tencent.trpc.spring.cloud.gateway.filter.test.server.Hello.HelloRsp;
import org.springframework.stereotype.Service;

@Service
public class HelloServiceImpl implements HelloAPI {

    private static final Logger logger = LoggerFactory.getLogger(HelloServiceImpl.class);

    @Override
    public HelloRsp sayHello(RpcContext context, HelloReq request) {
        RpcServerContext serverContext = (RpcServerContext) context;
        logger.info(getClass().getName() + " receive:{}, context:{}", request, serverContext);
        Hello.HelloRsp.Builder rspBuilder = Hello.HelloRsp.newBuilder();
        rspBuilder.setMsg(request.getMsg());
        rspBuilder.setId(request.getId());
        return rspBuilder.build();
    }

}
