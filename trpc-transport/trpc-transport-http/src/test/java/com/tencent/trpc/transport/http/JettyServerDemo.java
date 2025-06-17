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

package com.tencent.trpc.transport.http;

import com.tencent.trpc.core.common.config.ProtocolConfig;
import com.tencent.trpc.core.exception.ErrorCode;
import com.tencent.trpc.core.exception.TRpcException;
import com.tencent.trpc.core.extension.ExtensionLoader;
import com.tencent.trpc.transport.http.spi.HttpServerFactory;
import java.io.IOException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class JettyServerDemo {

    public static void main(String[] args) throws InterruptedException {
        ProtocolConfig config = new ProtocolConfig();
        config.setProtocol("http");
        config.setIp("127.0.0.1");
        config.setPort(28888);
        config.setTransporter("jetty");
        config.setDefault();
        ExtensionLoader<HttpServerFactory> extensionLoader =
                ExtensionLoader.getExtensionLoader(HttpServerFactory.class);
        HttpServerFactory httpServer = extensionLoader.getExtension(config.getTransporter());
        if (httpServer == null) {
            httpServer = extensionLoader.getDefaultExtension();
        }

        HttpServer server = httpServer.create(config, new HttpExecutor() {
            @Override
            public void execute(HttpServletRequest request, HttpServletResponse response)
                    throws IOException {
                String name = "admin";
                String path = request.getPathInfo();
                try {
                    response.getWriter().write("adminiinfo:........");
                } catch (IOException e) {
                    throw TRpcException
                            .newFrameException(ErrorCode.TRPC_INVOKE_UNKNOWN_ERR, "unknow", e);
                }
            }
        });
        server.open();
        Thread.sleep(1000000);
    }
}
