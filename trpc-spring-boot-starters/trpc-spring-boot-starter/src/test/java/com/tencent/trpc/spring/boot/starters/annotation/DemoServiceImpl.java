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

package com.tencent.trpc.spring.boot.starters.annotation;

import com.tencent.trpc.core.rpc.RpcClientContext;
import com.tencent.trpc.spring.annotation.TRpcClient;
import com.tencent.trpc.spring.boot.starters.test.GreeterService;
import com.tencent.trpc.spring.boot.starters.test.HelloRequestProtocol.HelloRequest;
import com.tencent.trpc.spring.boot.starters.test.HelloRequestProtocol.HelloResponse;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class DemoServiceImpl {

    @TRpcClient(id = "trpc.TestApp.TestServer.Greeter1Naming")
    private GreeterService greeterService;
    @TRpcClient(id = "trpc.TestApp.TestServer.Greeter2Naming")
    private GreeterService greeterService2;
    @TRpcClient(id = "myTestServer")
    private GreeterService myTestServerClient;

    @Autowired
    private GreeterService myTestServer;
    @Resource(name = "trpc.TestApp.TestServer.Greeter1Naming")
    private GreeterService greeterService1Bean;
    @Autowired
    @Qualifier("trpc.TestApp.TestServer.Greeter2Naming")
    private GreeterService greeterService2Bean;

    public HelloResponse sayHello(HelloRequest request) {
        return greeterService.sayHello(new RpcClientContext(), request);
    }

    public GreeterService getGreeterService() {
        return greeterService;
    }

    public void setGreeterService(GreeterService greeterService) {
        this.greeterService = greeterService;
    }

    public GreeterService getGreeterService2() {
        return greeterService2;
    }

    public void setGreeterService2(GreeterService greeterService2) {
        this.greeterService2 = greeterService2;
    }

    public GreeterService getMyTestServerClient() {
        return myTestServerClient;
    }

    public void setMyTestServerClient(GreeterService myTestServerClient) {
        this.myTestServerClient = myTestServerClient;
    }

    public GreeterService getMyTestServer() {
        return myTestServer;
    }

    public void setMyTestServer(GreeterService myTestServer) {
        this.myTestServer = myTestServer;
    }

    public GreeterService getGreeterService1Bean() {
        return greeterService1Bean;
    }

    public void setGreeterService1Bean(GreeterService greeterService1Bean) {
        this.greeterService1Bean = greeterService1Bean;
    }

    public GreeterService getGreeterService2Bean() {
        return greeterService2Bean;
    }

    public void setGreeterService2Bean(GreeterService greeterService2Bean) {
        this.greeterService2Bean = greeterService2Bean;
    }
}
