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

import com.tencent.trpc.core.filter.spi.Filter;
import com.tencent.trpc.core.rpc.Invoker;
import com.tencent.trpc.core.rpc.Request;
import com.tencent.trpc.core.rpc.Response;
import java.util.concurrent.CompletionStage;
import javax.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

public class AutoInjectTestFilter implements Filter {

    @Resource(name = "trpc.TestApp.TestServer.Greeter1Naming")
    private GreeterService greeterService;
    @Autowired
    private GreeterService myTestServer;

    private GreeterService greeterService2;

    public GreeterService getGreeterService() {
        return greeterService;
    }

    public GreeterService getMyTestServer() {
        return myTestServer;
    }

    public GreeterService getGreeterService2() {
        return greeterService2;
    }

    @Autowired
    @Qualifier("trpc.TestApp.TestServer.Greeter2Naming")
    public void setGreeterService2(GreeterService greeterService2) {
        this.greeterService2 = greeterService2;
    }

    @Override
    public CompletionStage<Response> filter(Invoker<?> filterChain, Request req) {
        return filterChain.invoke(req);
    }
}
