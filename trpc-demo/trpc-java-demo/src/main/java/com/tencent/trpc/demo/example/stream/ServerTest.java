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

package com.tencent.trpc.demo.example.stream;

import com.tencent.trpc.core.common.ConfigManager;
import com.tencent.trpc.core.common.config.ProviderConfig;
import com.tencent.trpc.core.common.config.ServiceConfig;
import com.tencent.trpc.demo.proto.StreamGreeterServiceStreamAPI;
import com.tencent.trpc.demo.server.stream.impl.StreamGreeterServiceImpl1;

public class ServerTest {

    // TCP test port
    private static final int TCP_PORT = 12321;

    public static void main(String[] args) throws Exception {
        ConfigManager.getInstance().start();
        startServer();
        System.out.println(">>>[server] started");

        // If you want to stop server programmatically, uncomment the next line.
//        ConfigManager.getInstance().stop();
    }

    private static void startServer() {
        // setup the server interface
        ProviderConfig<StreamGreeterServiceStreamAPI> providerConfig = new ProviderConfig<>();
        providerConfig.setRef(new StreamGreeterServiceImpl1());

        // export trpc stream server
        ServiceConfig tRpcServiceConfig = getTRpcServiceConfig();
        tRpcServiceConfig.addProviderConfig(providerConfig);
        tRpcServiceConfig.export();
    }

    private static ServiceConfig getTRpcServiceConfig() {
        ServiceConfig serviceConfig = new ServiceConfig();
        serviceConfig.setIp("127.0.0.1");
        serviceConfig.setNetwork("tcp");
        serviceConfig.setPort(TCP_PORT);
        return serviceConfig;
    }

}
