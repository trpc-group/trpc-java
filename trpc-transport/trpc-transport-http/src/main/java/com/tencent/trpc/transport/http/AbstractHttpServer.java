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

package com.tencent.trpc.transport.http;

import com.tencent.trpc.core.common.LifecycleBase;
import com.tencent.trpc.core.common.config.ProtocolConfig;
import com.tencent.trpc.core.exception.LifecycleException;
import com.tencent.trpc.core.exception.TransportException;
import com.tencent.trpc.core.logger.Logger;
import com.tencent.trpc.core.logger.LoggerFactory;
import java.net.InetSocketAddress;
import java.util.Objects;

public abstract class AbstractHttpServer implements HttpServer {

    private static final Logger logger = LoggerFactory.getLogger(AbstractHttpServer.class);
    /**
     * Class name, used for logging.
     */
    protected final String name;
    /**
     * The local ip of this service.
     */
    protected final InetSocketAddress bindAddress;
    /**
     * Used for inner lifecycle control.
     **/
    protected LifecycleObj lifecycleObj = new LifecycleObj();
    private ProtocolConfig config;
    private HttpExecutor executor;

    public AbstractHttpServer(ProtocolConfig config, HttpExecutor executor) {
        this.name = getClass().getName();
        this.bindAddress = config.toInetSocketAddress();
        this.config = Objects.requireNonNull(config, "config is null");
        this.executor = executor;
    }

    @Override
    public void open() throws TransportException {
        try {
            lifecycleObj.start();
        } catch (Exception ex) {
            Throwable parsedEx = LifecycleException.parseSourceException(ex);
            if (parsedEx instanceof TransportException) {
                throw (TransportException) parsedEx;
            } else {
                throw new TransportException(
                        "Open http server transport(" + config.toSimpleString() + ") exception",
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
        try {
            lifecycleObj.stop();
        } catch (Exception ex) {
            logger.error("Close server transport exception", ex);
        }
    }

    protected abstract void doOpen() throws Exception;

    protected abstract void doClose();

    @Override
    public String toString() {
        return "{name=" + name + ", config=" + config + ", bindip=" + bindAddress + "}";
    }

    // set & get
    @Override
    public ProtocolConfig getConfig() {
        return config;
    }

    public void setConfig(ProtocolConfig config) {
        this.config = config;
    }

    public InetSocketAddress getLocalAddress() {
        return bindAddress;
    }

    @Override
    public HttpExecutor getExecutor() {
        return executor;
    }

    public void setExecutor(HttpExecutor executor) {
        this.executor = executor;
    }

    protected class LifecycleObj extends LifecycleBase {

        @Override
        protected void initInternal() throws Exception {
            super.initInternal();
        }

        @Override
        protected void startInternal() throws Exception {
            super.startInternal();
            logger.info(">>>Http server transport binding(name={},ip={})", name, bindAddress);
            doOpen();
            logger.info(">>>Http server transport binded(name={},ip={})", name, bindAddress);
        }

        @Override
        protected void stopInternal() throws Exception {
            super.stopInternal();
            logger.info(">>>Http server transport closing(name={},serverip={})", name, bindAddress);
            try {
                doClose();
            } catch (Throwable e) {
                logger.error(
                        String.format("Http server transport(name=%s,ip=%s), close failed", name,
                                bindAddress),
                        e);
            }
            logger.info("<<<Server transport closed(name={},serverip={})", name, bindAddress);
        }

        @Override
        public String toString() {
            return AbstractHttpServer.this.toString();
        }
    }

}
