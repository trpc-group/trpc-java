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

package com.tencent.trpc.transport.http.spi;

import com.tencent.trpc.core.common.config.ProtocolConfig;
import com.tencent.trpc.core.extension.Extensible;
import com.tencent.trpc.transport.http.HttpExecutor;
import com.tencent.trpc.transport.http.HttpServer;

@Extensible("jetty")
public interface HttpServerFactory {

    /**
     * Create a HTTPServer.
     *
     * @param config protocol config
     * @param executor http request executor
     * @return A tRPC HTTPServer
     */
    HttpServer create(ProtocolConfig config, HttpExecutor executor);

}
