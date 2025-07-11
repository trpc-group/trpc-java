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

package com.tencent.trpc.core.rpc;

import com.tencent.trpc.core.common.LifecycleBase;
import com.tencent.trpc.core.common.config.ProtocolConfig;
import com.tencent.trpc.core.exception.ErrorCode;
import com.tencent.trpc.core.exception.LifecycleException;
import com.tencent.trpc.core.exception.TRpcException;
import com.tencent.trpc.core.exception.TransportException;
import java.util.Objects;

public abstract class AbstractRpcClient implements RpcClient {

    /**
     * Protocol Instance configuration.
     */
    protected ProtocolConfig protocolConfig;
    /**
     * Built-in lifecycle.
     */
    protected LifecycleObj lifecycleObj = new LifecycleObj();
    protected CloseFuture<Void> future = new CloseFuture<>();

    public void setConfig(ProtocolConfig config) throws TRpcException {
        this.protocolConfig = config;
    }

    @Override
    public void open() throws TRpcException {
        try {
            lifecycleObj.start();
        } catch (Exception ex) {
            Throwable parsedEx = LifecycleException.parseSourceException(ex);
            if (parsedEx instanceof TransportException) {
                throw TRpcException.newFrameException(ErrorCode.TRPC_CLIENT_CONNECT_ERR,
                        "Open server(" + this.protocolConfig.toSimpleString() + ") exception",
                        parsedEx);
            } else if (parsedEx instanceof TRpcException) {
                throw (TRpcException) parsedEx;
            } else {
                throw TRpcException.newFrameException(ErrorCode.TRPC_INVOKE_UNKNOWN_ERR,
                        "Open server(" + this.protocolConfig.toSimpleString() + ") exception",
                        parsedEx);
            }
        }
    }

    /**
     * Check if RpcClient is closed.
     *
     * @see com.tencent.trpc.core.rpc.RpcClient#isClosed()
     */
    @Override
    public boolean isClosed() {
        return lifecycleObj.isFailed() || lifecycleObj.isStopping() || lifecycleObj.isStopped();
    }

    /**
     * Check if RpcClient is available.
     *
     * @see com.tencent.trpc.core.rpc.RpcClient#isAvailable()
     */
    @Override
    public boolean isAvailable() {
        return lifecycleObj.isStarted();
    }

    /**
     * Close RpcClient.
     *
     * @see com.tencent.trpc.core.rpc.RpcClient#close()
     */
    @Override
    public void close() {
        lifecycleObj.stop();
    }

    @Override
    public CloseFuture<Void> closeFuture() {
        return future;
    }

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
            Objects.requireNonNull(getProtocolConfig(), "config is null");
            doOpen();
        }

        @Override
        protected void stopInternal() throws Exception {
            super.stopInternal();
            try {
                doClose();
            } finally {
                future.complete(null);
            }
        }

        @Override
        public String toString() {
            return AbstractRpcClient.this.toString();
        }
    }

}
