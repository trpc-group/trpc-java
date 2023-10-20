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

package com.tencent.trpc.core.transport;

import com.tencent.trpc.core.common.LifecycleBase;
import com.tencent.trpc.core.common.config.ProtocolConfig;
import com.tencent.trpc.core.exception.LifecycleException;
import com.tencent.trpc.core.exception.TransportException;
import com.tencent.trpc.core.logger.Logger;
import com.tencent.trpc.core.logger.LoggerFactory;
import com.tencent.trpc.core.transport.codec.ServerCodec;
import java.net.InetSocketAddress;
import java.util.Objects;

public abstract class AbstractServerTransport implements ServerTransport {

    private static final Logger logger = LoggerFactory.getLogger(AbstractServerTransport.class);

    /**
     * Class name, used for logging.
     */
    protected final String name;
    /**
     * Service instance configuration information.
     */
    protected final ProtocolConfig config;
    /**
     * Service bind port information.
     */
    protected final InetSocketAddress bindAddress;
    /**
     * Channel event handler.
     */
    protected final ChannelHandler channelHandler;
    /**
     * Encoding plugin.
     */
    protected final ServerCodec codec;
    /**
     * Built-in lifecycle control.
     **/
    protected LifecycleObj lifecycleObj = new LifecycleObj();

    public AbstractServerTransport(ProtocolConfig config, ChannelHandler channelHandler,
            ServerCodec serverCodec) throws TransportException {
        Objects.requireNonNull(config, "protocolConfig").init();
        this.name = getClass().getName();
        this.bindAddress = config.toInetSocketAddress();
        this.config = Objects.requireNonNull(config, "config is null");
        this.channelHandler = Objects.requireNonNull(channelHandler, "channelHandler is null");
        this.codec = serverCodec;
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
                        "Open server transport(" + config.toSimpleString() + ") exception",
                        parsedEx);
            }
        }
    }

    @Override
    public void close() {
        try {
            lifecycleObj.stop();
        } catch (Exception ex) {
            logger.error("Close server transport exception", ex);
        }
    }

    @Override
    public boolean isClosed() {
        return lifecycleObj.isFailed() || lifecycleObj.isStopping() || lifecycleObj.isStopped();
    }

    protected abstract void doOpen() throws Exception;

    protected abstract void doClose();

    @Override
    public InetSocketAddress getLocalAddress() {
        return bindAddress;
    }

    @Override
    public ChannelHandler getChannelHandler() {
        return channelHandler;
    }

    @Override
    public ProtocolConfig getProtocolConfig() {
        return config;
    }

    @Override
    public String toString() {
        return "{name=" + name + ", config=" + config + ", bindAddress=" + bindAddress + "}";
    }

    protected final class LifecycleObj extends LifecycleBase {

        @Override
        protected void initInternal() throws Exception {
            super.initInternal();
        }

        @Override
        protected void startInternal() throws Exception {
            super.startInternal();
            logger.info(">>>Server transport binding(name={},ip={})", name, bindAddress);
            doOpen();
            logger.info(">>>Server transport binded(name={},ip={})", name, bindAddress);
        }

        @Override
        protected void stopInternal() throws Exception {
            super.stopInternal();
            logger.info(">>>Server transport closing(name={},serverip={})", name, bindAddress);
            try {
                doClose();
            } catch (Throwable e) {
                logger.error(String.format("Server transport(name=%s,ip=%s), close failed", name,
                                bindAddress),
                        e);
            }
            try {
                if (channelHandler != null) {
                    channelHandler.destroy();
                }
            } catch (Throwable e) {
                logger.error(
                        String.format(
                                "Server transport(name=%s,ip=%s,channel=%s), channel destroy "
                                        + "exception",
                                name, bindAddress, channelHandler),
                        e);
            }
            logger.info("<<<Server transport closed(name={},serverip={})", name, bindAddress);
        }

        @Override
        public String toString() {
            return AbstractServerTransport.this.toString();
        }
    }

}
