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

package com.tencent.trpc.demo.example.config;

import com.tencent.trpc.core.common.ConfigManager;
import com.tencent.trpc.core.common.config.ProviderConfig;
import com.tencent.trpc.core.common.config.ServiceConfig;
import com.tencent.trpc.demo.proto.GreeterServiceAPI;
import com.tencent.trpc.demo.server.impl.GreeterServiceImpl;

public class ServerTest {

    // TCP test port
    public static final int TCP_PORT = 12321;
    // Http test port
    public static final int HTTP_PORT = 12322;

    public static void main(String[] args) {
        ConfigManager.getInstance().start();
        startServer();
        System.out.println(">>>[server] started");

        // If you want to stop the server programmatically, uncomment the next line.
//        ConfigManager.getInstance().stop();
    }

    private static void startServer() {
        // setup the server interface
        ProviderConfig<GreeterServiceAPI> providerConfig = new ProviderConfig<>();
        providerConfig.setRef(new GreeterServiceImpl());

        // export trpc server
        ServiceConfig tRpcServiceConfig = getTRpcServiceConfig();
        tRpcServiceConfig.addProviderConfig(providerConfig);
        tRpcServiceConfig.export();

        // export http server
        ServiceConfig httpServiceConfig = getHttpServiceConfig();
        httpServiceConfig.addProviderConfig(providerConfig);
        httpServiceConfig.export();
    }

    private static ServiceConfig getTRpcServiceConfig() {
        ServiceConfig serviceConfig = new ServiceConfig();
        serviceConfig.setIp("127.0.0.1");
        serviceConfig.setNetwork("tcp");
        serviceConfig.setPort(TCP_PORT);
        return serviceConfig;
    }

    private static ServiceConfig getHttpServiceConfig() {
        ServiceConfig serviceConfig = new ServiceConfig();
        serviceConfig.setIp("127.0.0.1");
        serviceConfig.setNetwork("tcp");
        serviceConfig.setPort(HTTP_PORT);
        serviceConfig.setProtocol("http");
        serviceConfig.setTransporter("jetty");
        return serviceConfig;
    }

}
