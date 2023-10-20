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

package com.tencent.trpc.demo.example.yaml;

import com.tencent.trpc.core.common.ConfigManager;
import com.tencent.trpc.core.common.TRpcSystemProperties;
import com.tencent.trpc.core.rpc.RpcClientContext;
import com.tencent.trpc.core.rpc.TRpcProxy;
import com.tencent.trpc.demo.proto.GreeterService2API;
import com.tencent.trpc.demo.proto.GreeterServiceAPI;
import com.tencent.trpc.demo.proto.HelloRequestProtocol;
import com.tencent.trpc.server.main.Main;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class ClientTest {

    public static void main(String[] args) throws Exception {
        try {
            String confPath = Objects.requireNonNull(ClientTest.class.getClassLoader()
                    .getResource("trpc_java_client.yaml")).getPath();
            TRpcSystemProperties.setProperties(TRpcSystemProperties.CONFIG_PATH, confPath);
            Main.main(args);

            int times = 5;

            System.out.println("=================trpc tcp test===================");
            GreeterServiceAPI api = TRpcProxy.getProxy("trpc.TestApp.TestServer.Greeter1", GreeterServiceAPI.class);
            runTests1(times, api);
            System.out.println("=================trpc tcp test done===================");

            System.out.println("=================trpc udp test===================");
            GreeterService2API api2 = TRpcProxy.getProxy("trpc.TestApp.TestServer.Greeter2", GreeterService2API.class);
            runTests2(times, api2);
            System.out.println("=================trpc udp test done===================");

            System.out.println("=================http test1===================");
            GreeterServiceAPI httpApi1 = TRpcProxy.getProxy("trpc.TestApp.TestServer.Greeter3",
                    GreeterServiceAPI.class);
            runTests1(times, httpApi1);
            System.out.println("=================http test1 done===================");

            System.out.println("=================http test2===================");
            GreeterService2API httpApi2 = TRpcProxy.getProxy("trpc.TestApp.TestServer.Greeter4",
                    GreeterService2API.class);
            runTests2(times, httpApi2);
            System.out.println("=================http test done===================");
        } finally {
            ConfigManager.getInstance().stop();
        }
    }

    private static void runTests1(int times, GreeterServiceAPI proxy) throws Exception {
        for (int i = 0; i < times; i++) {
            String message = proxy
                    .sayHello(new RpcClientContext(),
                            HelloRequestProtocol.HelloRequest.newBuilder()
                                    .setMessage("[GreeterService] tRPC-Java")
                                    .build())
                    .getMessage();
            System.out.println(Thread.currentThread().getName() + ">>>[client] receive msg: " + message);
            TimeUnit.SECONDS.sleep(1);
        }
    }

    private static void runTests2(int times, GreeterService2API proxy) throws Exception {
        for (int i = 0; i < times; i++) {
            String message = proxy
                    .sayHi(new RpcClientContext(),
                            HelloRequestProtocol.HelloRequest.newBuilder()
                                    .setMessage("[GreeterService2] tRPC-Java")
                                    .build())
                    .getMessage();
            System.out.println(Thread.currentThread().getName() + ">>>[client] receive msg: " + message);
            TimeUnit.SECONDS.sleep(1);
        }
    }

}
