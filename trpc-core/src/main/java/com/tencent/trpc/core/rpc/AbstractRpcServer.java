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

package com.tencent.trpc.core.rpc;

import com.tencent.trpc.core.common.LifecycleBase;
import com.tencent.trpc.core.common.config.ProtocolConfig;
import com.tencent.trpc.core.common.config.ProviderConfig;
import com.tencent.trpc.core.exception.ErrorCode;
import com.tencent.trpc.core.exception.LifecycleException;
import com.tencent.trpc.core.exception.TRpcException;
import java.util.Objects;

public abstract class AbstractRpcServer implements RpcServer {

    /**
     * Protocol Instance configuration.
     */
    protected ProtocolConfig protocolConfig;
    /**
     * Built-in lifecycle.
     */
    protected LifecycleObj lifecycleObj = new LifecycleObj();
    protected CloseFuture<Void> future = new CloseFuture<Void>();

    public void setConfig(ProtocolConfig protocolConfig) {
        this.protocolConfig = Objects.requireNonNull(protocolConfig, "config");
        this.protocolConfig.init();
    }

    @Override
    public <T> void export(ProviderInvoker<T> invoker) {
        doExport(invoker);
    }

    @Override
    public <T> void unexport(ProviderConfig<T> config) {
        doUnExport(config);
    }

    @Override
    public void open() throws TRpcException {
        try {
            lifecycleObj.start();
        } catch (Exception ex) {
            Throwable parsedEx = LifecycleException.parseSourceException(ex);
            if (parsedEx instanceof TRpcException) {
                throw (TRpcException) parsedEx;
            } else {
                throw TRpcException.newFrameException(ErrorCode.TRPC_SERVER_SYSTEM_ERR,
                        "Open server(" + this.protocolConfig.toSimpleString() + ") exception",
                        parsedEx);
            }
        }
    }

    @Override
    public boolean isClosed() {
        return lifecycleObj.isFailed() || lifecycleObj.isStopping() || lifecycleObj.isStopped();
    }

    @Override
    public void close() {
        lifecycleObj.stop();
    }

    @Override
    public CloseFuture<Void> closeFuture() {
        return future;
    }

    protected abstract <T> void doExport(ProviderInvoker<T> invoker);

    protected abstract <T> void doUnExport(ProviderConfig<T> config);

    protected abstract void doOpen() throws Exception;

    protected abstract void doClose();

    @Override
    public ProtocolConfig getProtocolConfig() {
        return protocolConfig;
    }

    @Override
    public String toString() {
        return "{config=" + (protocolConfig != null ? protocolConfig.toSimpleString() : null) + "}";
    }

    protected final class LifecycleObj extends LifecycleBase {

        @Override
        protected void startInternal() throws Exception {
            super.startInternal();
            Objects.requireNonNull(protocolConfig, "config is null");
            doOpen();
        }

        @Override
        protected void stopInternal() throws Exception {
            super.stopInternal();
            try {
                doClose();
            } finally {
                RpcServerManager.remove(protocolConfig);
                future.complete(null);
            }
        }

        @Override
        public String toString() {
            return AbstractRpcServer.this.toString();
        }
    }

}
