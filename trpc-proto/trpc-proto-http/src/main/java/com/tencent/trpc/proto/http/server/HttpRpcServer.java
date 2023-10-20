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

package com.tencent.trpc.proto.http.server;

import com.tencent.trpc.core.common.config.ProtocolConfig;
import com.tencent.trpc.core.common.config.ProviderConfig;
import com.tencent.trpc.core.extension.ExtensionLoader;
import com.tencent.trpc.core.rpc.AbstractRpcServer;
import com.tencent.trpc.core.rpc.ProviderInvoker;
import com.tencent.trpc.core.utils.PreconditionUtils;
import com.tencent.trpc.transport.http.HttpServer;
import com.tencent.trpc.transport.http.spi.HttpServerFactory;
import java.util.Objects;

public class HttpRpcServer extends AbstractRpcServer {

    private final HttpServer server;
    private final DefaultHttpExecutor executor;

    public HttpRpcServer(ProtocolConfig config) {
        setConfig(config);
        HttpServerFactory serverFactory = ExtensionLoader
                .getExtensionLoader(HttpServerFactory.class)
                .getExtension(config.getTransporter());
        PreconditionUtils.checkArgument(serverFactory != null, "not suport http server factory[%s]",
                config.getTransporter());
        this.executor = new DefaultHttpExecutor(config);
        this.server = serverFactory.create(config, executor);
    }

    @Override
    protected <T> void doExport(ProviderInvoker<T> invoker) {
        executor.register(invoker);
    }

    @Override
    public <T> void unexport(ProviderConfig<T> config) {

    }

    @Override
    protected void doOpen() {
        Objects.requireNonNull(server, "server is null");
        server.open();
    }

    @Override
    protected void doClose() {
        server.close();
    }

    @Override
    protected <T> void doUnExport(ProviderConfig<T> config) {

    }
}
