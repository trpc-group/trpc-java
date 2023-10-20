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

package com.tencent.trpc.spring.boot.starters.test;

import com.tencent.trpc.core.rpc.RpcContext;
import com.tencent.trpc.spring.boot.starters.test.HelloRequestProtocol.HelloRequest;
import com.tencent.trpc.spring.boot.starters.test.HelloRequestProtocol.HelloResponse;
import org.springframework.stereotype.Service;

@Service
public class GreeterServiceImpl implements GreeterService {

    @Override
    public HelloResponse sayHello(RpcContext context, HelloRequest request) {
        return HelloResponse.newBuilder().setMessage(request.getMessage()).build();
    }
}
