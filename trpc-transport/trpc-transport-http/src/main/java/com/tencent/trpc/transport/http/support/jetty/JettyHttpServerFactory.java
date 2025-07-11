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

package com.tencent.trpc.transport.http.support.jetty;

import static com.tencent.trpc.transport.http.common.Constants.HTTP2C_SCHEME;
import static com.tencent.trpc.transport.http.common.Constants.HTTP2_SCHEME;
import static com.tencent.trpc.transport.http.common.Constants.HTTPS_SCHEME;

import com.tencent.trpc.core.common.config.ProtocolConfig;
import com.tencent.trpc.core.extension.Extension;
import com.tencent.trpc.transport.http.HttpExecutor;
import com.tencent.trpc.transport.http.HttpServer;
import com.tencent.trpc.transport.http.spi.HttpServerFactory;
import com.tencent.trpc.transport.http.util.HttpUtils;

@Extension(JettyHttpServer.NAME)
public class JettyHttpServerFactory implements HttpServerFactory {

    @Override
    public HttpServer create(ProtocolConfig config, HttpExecutor executor) {
        switch (HttpUtils.getScheme(config)) {
            case HTTP2C_SCHEME:
                return new JettyHttp2cServer(config, executor);
            case HTTP2_SCHEME:
                return new JettyHttp2Server(config, executor);
            case HTTPS_SCHEME:
                return new JettyHttpsServer(config, executor);
            // Default using http
            default:
                return new JettyHttpServer(config, executor);
        }

    }


}
