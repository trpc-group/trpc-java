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

package com.tencent.trpc.proto.standard.stream.server;

import com.tencent.trpc.core.common.config.ProtocolConfig;
import com.tencent.trpc.core.common.config.ProviderConfig;
import com.tencent.trpc.core.exception.ErrorCode;
import com.tencent.trpc.core.exception.TRpcException;
import com.tencent.trpc.core.extension.ExtensionLoader;
import com.tencent.trpc.core.rpc.AbstractRpcServer;
import com.tencent.trpc.core.rpc.ProviderInvoker;
import com.tencent.trpc.core.rpc.def.DefMethodInfoRegister;
import com.tencent.trpc.core.stream.Closeable;
import com.tencent.trpc.core.stream.transport.ServerTransport;
import com.tencent.trpc.core.stream.transport.codec.FrameDecoder;
import com.tencent.trpc.core.stream.transport.spi.ServerTransportFactory;
import com.tencent.trpc.proto.standard.stream.TRpcStreamResponder;
import java.util.Objects;
import java.util.function.Supplier;
import reactor.core.publisher.Mono;

/**
 * TRPC streaming server
 */
public class TRpcStreamServer extends AbstractRpcServer {

    /**
     * Protocol frame decoder creator, used for creating a separate frame decoder for each connection.
     */
    private final Supplier<FrameDecoder> frameDecoder;
    /**
     * Service method registrar, used for registering and looking up RPC methods.
     */
    private final DefMethodInfoRegister methodInfoRegister = new DefMethodInfoRegister();
    /**
     * Closeable service instance.
     */
    private Closeable service;

    public TRpcStreamServer(ProtocolConfig protocolConfig, Supplier<FrameDecoder> frameDecoder) {
        this.protocolConfig = Objects.requireNonNull(protocolConfig, "protocolConfig is null");
        this.frameDecoder = Objects.requireNonNull(frameDecoder, "frameDecoder is null");
    }

    @Override
    protected <T> void doExport(ProviderInvoker<T> invoker) {
        // export the RPC interface.
        methodInfoRegister.register(invoker);
    }

    @Override
    protected <T> void doUnExport(ProviderConfig<T> config) {
        // unexport the RPC interface.
        methodInfoRegister.unregister(config);
    }

    @Override
    protected void doOpen() throws Exception {
        // use the plugin mechanism to create server transport.
        ServerTransport<? extends Closeable> serverTransport = ExtensionLoader
                .getExtensionLoader(ServerTransportFactory.class)
                .getExtension(protocolConfig.getTransporter())
                .create(protocolConfig, frameDecoder);
        Exception[] holder = new Exception[1];
        serverTransport.start(c -> Mono.just(new TRpcStreamResponder(protocolConfig, c, methodInfoRegister)).then())
                .doOnSuccess(svr -> {
                    this.service = svr;
                    // since reactor-netty's own threads are daemon threads, create an additional listening
                    // thread here to prevent the service from exiting prematurely.
                    Thread svrThread = new Thread(() -> svr.onClose().block(), "reactor-svr");
                    svrThread.setDaemon(false);
                    svrThread.start();
                })
                .doOnError(t -> holder[0] = TRpcException.newFrameException(ErrorCode.TRPC_SERVER_SYSTEM_ERR,
                        "create service failed", t))
                .block();

        if (holder[0] != null) {
            throw holder[0];
        }
    }

    @Override
    protected void doClose() {
        // at this point, the server is closed, and the transport will no longer be used.
        if (this.service != null) {
            this.service.dispose();
            this.service = null;
        }
    }
}
