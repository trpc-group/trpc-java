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

package com.tencent.trpc.spring.demo.server;

import com.tencent.trpc.demo.proto.GreeterService2AsyncAPI;
import com.tencent.trpc.demo.proto.GreeterServiceAsyncAPI;
import com.tencent.trpc.spring.annotation.TRpcClient;
import org.springframework.stereotype.Service;

@Service
public class ProxyService {

    public static final String SERVICE_NAME1 = "trpc.TestApp.TestServer.Greeter1";
    public static final String SERVICE_NAME2 = "trpc.TestApp.TestServer.Greeter2";

    @TRpcClient(id = SERVICE_NAME1)
    private GreeterServiceAsyncAPI greeterService;

    @TRpcClient(id = SERVICE_NAME2)
    private GreeterService2AsyncAPI greeterService2;

    public GreeterServiceAsyncAPI getGreeterService() {
        return greeterService;
    }

    public GreeterService2AsyncAPI getGreeterService2() {
        return greeterService2;
    }

}
