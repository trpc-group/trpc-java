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

package com.tencent.trpc.core.transport;

import com.google.common.collect.Lists;
import com.tencent.trpc.core.common.LifecycleBase;
import com.tencent.trpc.core.common.config.ProtocolConfig;
import com.tencent.trpc.core.exception.LifecycleException;
import com.tencent.trpc.core.exception.TransportException;
import com.tencent.trpc.core.logger.Logger;
import com.tencent.trpc.core.logger.LoggerFactory;
import com.tencent.trpc.core.transport.codec.ClientCodec;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Abstract Client Transport, implements the skeleton methods for the client transport.
 *
 * <p>Abstracts doOpen, doClose, make, useChannelPool methods for subclasses to implement.</p>
 */
public abstract class AbstractClientTransport implements ClientTransport {

    private static final Logger logger = LoggerFactory.getLogger(AbstractClientTransport.class);
    /**
     * Class name, used for logging.
     */
    protected final String name;
    /**
     * Service instance configuration information.
     */
    protected final ProtocolConfig config;
    /**
     * Encoding plugin.
     */
    protected final ClientCodec codec;
    /**
     * Channel event handler.
     */
    protected final ChannelHandler handler;
    /**
     * Remote address.
     */
    protected InetSocketAddress remoteAddress;
    /**
     * Connection lock.
     */
    protected ReentrantLock connLock = new ReentrantLock();
    /**
     * Index of the polled channel.
     */
    protected AtomicInteger channelIdx = new AtomicInteger(0);
    /**
     * List of channels.
     */
    protected List<ChannelFutureItem> channels;
    /**
     * Built-in lifecycle control.
     **/
    protected LifecycleObj lifecycleObj = new LifecycleObj();

    public AbstractClientTransport(ProtocolConfig config, ChannelHandler handler,
            ClientCodec clientCodec) {
        Objects.requireNonNull(config, "protocolConfig").init();
        this.name = getClass().getName();
        this.remoteAddress = config.toInetSocketAddress();
        this.config = Objects.requireNonNull(config, "config is null");
        this.handler = Objects.requireNonNull(handler, "handler is null");
        this.codec = clientCodec;
        this.channels = Lists.newArrayListWithExpectedSize(config.getConnsPerAddr());
    }

    /**
     * Initialize the client transport.
     *
     * @throws TransportException if the transport fails to start
     * @see com.tencent.trpc.core.transport.ClientTransport#open()
     */
    @Override
    public void open() throws TransportException {
        try {
            lifecycleObj.start();
        } catch (Exception ex) {
            Throwable parsedEx = LifecycleException.parseSourceException(ex);
            String message = "Client transport(class=" + getClass().getName()
                    + ") start failed, connect to server [" + remoteAddress + "]";
            throw TransportException.trans(parsedEx, message);
        }
    }

    /**
     * If at least one connection is established, the connection is considered successful, or if the initial state is
     * also considered ok.
     *
     * @return true if connected, false if not connected
     * @see com.tencent.trpc.core.transport.ClientTransport#isConnected()
     */
    @Override
    public boolean isConnected() {
        boolean isAllNotYetConnect = true;
        boolean isConn = false;
        for (ChannelFutureItem each : channels) {
            if (!each.isNotYetConnect()) {
                isAllNotYetConnect = false;
            }
            if (each.isAvailable()) {
                isConn = true;
            }
        }
        return isAllNotYetConnect || isConn;
    }

    /**
     * Check if the clientTransport is closed.
     *
     * @return true if closed, false if not closed
     * @see com.tencent.trpc.core.transport.ClientTransport#isClosed()
     */
    @Override
    public boolean isClosed() {
        return lifecycleObj.isFailed() || lifecycleObj.isStopping() || lifecycleObj.isStopped();
    }

    @Override
    public void close() {
        try {
            lifecycleObj.stop();
        } catch (Exception ex) {
            logger.error("Close client transport exception", ex);
        }
    }

    /**
     * Related to opening the transport.
     */
    protected abstract void doOpen() throws Exception;

    /**
     * Create a new channel. When the future is canceled, ensure that the underlying channel future is also canceled.
     *
     * @return a CompletableFuture of the created Channel
     * @throws Exception if an error occurs while creating a channel
     */
    protected abstract CompletableFuture<Channel> make() throws Exception;

    /**
     * Related to closing the transport.
     */
    protected abstract void doClose();

    /**
     * Check if the channel connection pool is used.
     *
     * @return true if the channel connection pool is used, false otherwise
     */
    protected abstract boolean useChannelPool();

    /**
     * Asynchronously send a packet.
     *
     * @param msg the message to be sent
     * @return a CompletionStage of type Void
     * @throws TransportException if the transport is not available or closed
     */
    @Override
    public CompletionStage<Void> send(Object msg) throws TransportException {
        if (isClosed()) {
            throw TransportException.create(String.format(
                    "Client transport(transport=%s, class=%s, msg=%s) send fail, due to transport"
                            + " is not available or close",
                    this, name, msg));
        }
        return getChannel0().thenCompose(f -> f.send(msg));
    }

    /**
     * Get the channel by polling.
     */
    @Override
    public CompletionStage<Channel> getChannel() throws TransportException {
        if (isClosed()) {
            throw TransportException.create(String.format(
                    "Client transport(transport=%s, class=%s) get channel fail, due to transport "
                            + "is not available or close",
                    this, name));
        }
        return getChannel0();
    }

    protected CompletionStage<Channel> getChannel0() throws TransportException {
        if (useChannelPool()) {
            int chIndex = Math.abs(channelIdx.getAndIncrement() % channels.size());
            try {
                ensureChannelActive(chIndex);
            } catch (Exception e) {
                throw TransportException.trans(e);
            }
            Objects.requireNonNull(channels.get(chIndex), "channel is null");// 理论上不会发生
            return channels.get(chIndex).getChannelFuture();
        }
        return createChannel();
    }

    protected CompletionStage<Channel> createChannel() throws TransportException {
        CompletionStage<Channel> make = null;
        try {
            make = make();
        } catch (Exception ex) {
            throw TransportException.trans(ex,
                    "Failed to create channel, remote server(addr="
                            + getRemoteAddress() + ")");
        }
        // add logging & callback check to prevent connection leakage
        make.whenComplete((r, t) -> {
            InetSocketAddress rAddr = getRemoteAddress();
            if (t != null) {
                logger.error("Client transport({}) create connection exception", this.toString(), t);
            } else {
                logger.debug("Client transport(remote addr={}, network={}) create connection success",
                        rAddr, config.getNetwork());
            }
            // to prevent connection leakage, do one more check: when the connection is successfully established, if
            // it is found to be closed, close the established connection directly
            if (r != null) {
                if (isClosed()) {
                    logger.error("Client transport({}) create connection success, but transport is "
                            + "close, will close the channel", this.toString());
                    try {
                        r.close();
                    } catch (Exception ex) {
                        logger.error("close channel exception", ex);
                    }
                }
            }
        });
        return make;
    }

    protected void ensureChannelActive(int chIndex) {
        ChannelFutureItem curChannelItem = channels.get(chIndex);
        // initiate a new connection creation action when the connection is not yet established or the connection
        // is broken
        boolean futureIsDoneAndNotConnected =
                !curChannelItem.isAvailable() && !curChannelItem.isConnecting();
        // not available and not establishing a connection
        if (futureIsDoneAndNotConnected || curChannelItem.isNotYetConnect()) {
            connLock.lock();
            try {
                channels.set(chIndex, new ChannelFutureItem(createChannel().toCompletableFuture(), config));
                try {
                    curChannelItem.close();
                } catch (Exception ex) {
                    logger.error("close " + curChannelItem + " exception", ex);
                }
            } finally {
                connLock.unlock();
            }
        }
    }

    @Override
    public InetSocketAddress getRemoteAddress() {
        return remoteAddress;
    }

    @Override
    public ChannelHandler getChannelHandler() {
        return handler;
    }

    @Override
    public ProtocolConfig getProtocolConfig() {
        return config;
    }

    @Override
    public String toString() {
        return "Client transport[name=" + name + ", config="
                + (config == null ? "<null>" : config.toSimpleString()) + ", remoteAddress="
                + remoteAddress
                + "]";
    }

    public static class ChannelFutureItem {

        private CompletableFuture<Channel> channelFuture;

        private ProtocolConfig config;

        public ChannelFutureItem(CompletableFuture<Channel> channelFuture, ProtocolConfig config) {
            super();
            this.channelFuture = channelFuture;
            this.config = config;
        }

        /**
         * State 1: Connection not initiated.
         *
         * @return true if the connection is not initiated, false otherwise
         */
        private boolean isNotYetConnect() {
            return channelFuture == null;
        }

        /**
         * State 2: Establishing connection.
         *
         * @return true if the connection is being established, false otherwise
         */
        private boolean isConnecting() {
            return channelFuture != null && !channelFuture.isDone();
        }

        /**
         * Check if the channel is available: if the future is [successfully established], it is considered available.
         * Note: not available when isConnecting.
         *
         * @return true if the channel is available, false otherwise
         */
        private boolean isAvailable() {
            if (channelFuture != null) {
                if (!channelFuture.isDone()) {
                    return false;
                } else if (channelFuture.isCompletedExceptionally()) {
                    return false;
                } else {
                    Channel join = channelFuture.join();
                    if (join != null) {
                        return join.isConnected();
                    }
                    return false;
                }
            }
            return false;
        }

        private void close() {
            if (channelFuture != null) {
                if (channelFuture.isDone()) {
                    if (!channelFuture.isCompletedExceptionally()) {
                        Channel join = channelFuture.join();
                        if (join != null) {
                            join.close();
                        }
                    }
                } else {
                    if (!channelFuture.cancel(true)) {
                        channelFuture.whenComplete((r, t) -> {
                            if (r != null) {
                                r.close();
                            }
                        });
                    }
                }
            }
        }

        public CompletableFuture<Channel> getChannelFuture() {
            return channelFuture;
        }

        @Override
        public String toString() {
            return "ChannelFtureItem [isNotYetConnet:" + isNotYetConnect() + ", remote:"
                    + (config == null ? "<null>" : config.toSimpleString()) + "]";
        }
    }

    protected class LifecycleObj extends LifecycleBase {

        @Override
        protected void initInternal() throws Exception {
            super.initInternal();
        }

        @Override
        protected void startInternal() throws Exception {
            super.startInternal();
            logger.info(">>>Client transport starting open(name={},ip={},channlePool={})", name,
                    remoteAddress, useChannelPool());
            doOpen();
            // do not use channel connection pool for short connections
            if (useChannelPool()) {
                for (int i = 0; i < config.getConnsPerAddr(); i++) {
                    if (config.isLazyinit()) {
                        channels.add(new ChannelFutureItem(null, config));
                    } else {
                        channels.add(new ChannelFutureItem(make().toCompletableFuture(), config));
                    }
                }
            }
            logger.info("<<<Client transport started open(name={},ip={})", name, remoteAddress);
        }

        @Override
        protected void stopInternal() throws Exception {
            super.stopInternal();
            logger.info(">>>Client transport closing(name={},remoteip={})", name, remoteAddress);
            for (ChannelFutureItem each : channels) {
                try {
                    each.close();
                } catch (Throwable ex) {
                    logger.error(String.format("Client transport(%s) destroy failed", each), ex);
                }
            }
            try {
                doClose();
            } catch (Throwable ex) {
                logger.error(String.format("Client transport(%s) destroy failed", getRemoteAddress(),
                        ex));
            }
            try {
                if (handler != null) {
                    handler.destroy();
                }
            } catch (Throwable ex) {
                logger.error(String.format(
                        "Client transport(name=%s,ip=%s) channel handler destroy exception",
                        name, getRemoteAddress()), ex);
            }
            logger.info("<<<Client transport closed(name={},remoteip={})", name, remoteAddress);
        }

        @Override
        public String toString() {
            return AbstractClientTransport.this.toString();
        }
    }

}
