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

package com.tencent.trpc.spring.demo;

import com.google.protobuf.Message;
import com.tencent.trpc.GreeterService3API;
import com.tencent.trpc.GreeterService3API.RequestParameterizedBean;
import com.tencent.trpc.core.rpc.RpcClientContext;
import com.tencent.trpc.core.rpc.RpcContext;
import com.tencent.trpc.demo.proto.GreeterService2AsyncAPI;
import com.tencent.trpc.demo.proto.GreeterServiceAsyncAPI;
import com.tencent.trpc.demo.proto.HelloRequestProtocol;
import com.tencent.trpc.spring.boot.starters.annotation.EnableTRpc;
import com.tencent.trpc.spring.demo.server.ProxyService;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

@EnableTRpc
@SpringBootApplication
public class ClientApplication {

    public static void main(String[] args) throws Exception {
        try (ConfigurableApplicationContext context = new SpringApplicationBuilder()
                .sources(ClientApplication.class).run(args)) {

            ProxyService demoService = context.getBean(ProxyService.class);
            GreeterServiceAsyncAPI service1 = context.getBean(ProxyService.SERVICE_NAME1,
                    GreeterServiceAsyncAPI.class);
            GreeterService2AsyncAPI service2 = context.getBean(ProxyService.SERVICE_NAME2,
                    GreeterService2AsyncAPI.class);

            GreeterService3API service3 = context.getBean(ProxyService.SERVICE_NAME3,
                    GreeterService3API.class);

            RpcContext ctx = new RpcClientContext();
            HelloRequestProtocol.HelloRequest request = HelloRequestProtocol.HelloRequest.newBuilder()
                    .setMessage("tRPC-Java")
                    .build();
            RequestParameterizedBean<String> parameterizedBean = RequestParameterizedBean.of("message",
                    "hello parameterizedBean");

            int times = 5;

            for (int i = 0; i < times; i++) {
                System.out.println("service1>>>>" + syncGetMessage(service1.sayHello(ctx, request)));
                System.out.println("service2>>>>" + syncGetMessage(service2.sayHi(ctx, request)));
                System.out.println("service3>>>>" + service3.sayHelloParameterized(ctx, parameterizedBean));
                System.out.println("demo service1>>>>"
                        + syncGetMessage(demoService.getGreeterService().sayHello(ctx, request)));
                System.out.println("demo service2>>>>"
                        + syncGetMessage(demoService.getGreeterService2().sayHi(ctx, request)));
                TimeUnit.SECONDS.sleep(1);
            }

        }
    }

    private static <T extends Message> T syncGetMessage(CompletionStage<T> completionStage) throws Exception {
        return completionStage.toCompletableFuture().get();
    }

}
