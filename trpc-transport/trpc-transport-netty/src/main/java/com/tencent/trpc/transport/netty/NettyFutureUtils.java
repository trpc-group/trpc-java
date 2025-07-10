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

package com.tencent.trpc.transport.netty;

import com.tencent.trpc.core.common.config.ProtocolConfig;
import com.tencent.trpc.core.exception.TransportException;
import com.tencent.trpc.core.logger.Logger;
import com.tencent.trpc.core.logger.LoggerFactory;
import com.tencent.trpc.core.transport.Channel;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * Netty utility class
 */
public class NettyFutureUtils {

    private static final Logger logger = LoggerFactory.getLogger(NettyTcpClientTransport.class);

    /**
     * Create a new {@link CompletableFuture} from the {@link ChannelFuture} of Netty {@link Bootstrap#connect}
     * method.
     *
     * @param future the {@link ChannelFuture} indicating the connecting status of a Netty Client.
     * @param config the protocol config
     * @return a {@link CompletableFuture} bound to the Netty {@link ChannelFuture}
     */
    public static CompletableFuture<Channel> fromConnectingFuture(
            ChannelFuture future, ProtocolConfig config) {
        Objects.requireNonNull(future, "future must not be null");
        CompletableFuture<Channel> result = new CompletableFuture<>();
        adaptConnectingFuture(future, result, config);
        return result;
    }

    /**
     * Create a new {@link CompletableFuture} from a Netty {@link ChannelFuture} which indicates the I/O events.
     *
     * @param future a Netty {@link ChannelFuture}
     * @return a {@link CompletableFuture} bound to the Netty {@link ChannelFuture}
     */
    public static CompletableFuture<Void> from(ChannelFuture future) {
        Objects.requireNonNull(future, "future must not be null");
        CompletableFuture<Void> result = new CompletableFuture<>();
        adapt(future, result);
        return result;
    }

    /**
     * Adapt a Netty {@link ChannelFuture} to a target JDK {@link CompletableFuture}.
     *
     * @param future a Netty {@link ChannelFuture}
     * @param target a JDK {@link CompletableFuture}
     */
    public static void adapt(ChannelFuture future, CompletableFuture<Void> target) {
        if (future.isSuccess()) {
            target.complete(null);
        } else if (future.isCancelled()) {
            target.cancel(false);
        } else if (future.isDone() && !future.isSuccess()) {
            target.completeExceptionally(TransportException.trans(future.cause()));
        } else {
            future.addListener(f -> {
                if (f.isSuccess()) {
                    target.complete(null);
                } else {
                    target.completeExceptionally(TransportException.trans(f.cause()));
                }
            });
        }
    }

    /**
     * Adapt to the connection establishment event.
     * <p>Note: Bind the lifecycle of the future and target together, so that cancelling the completion stage
     * will also synchronously cancel the IO channel.</p>
     * <p>When an exception occurs in the target, trigger the close method on the ChannelFuture.</p>
     * 
     * @param future a Netty {@link ChannelFuture}
     * @param target a JDK {@link CompletableFuture}
     * @param config the protocol config
     */
    public static void adaptConnectingFuture(ChannelFuture future,
            CompletableFuture<com.tencent.trpc.core.transport.Channel> target,
            ProtocolConfig config) {
        bindLifeCycle(future, target, config);
        target.whenComplete((r, t) -> {
            try {
                // ignore normal notifications in exceptional cases.
                if (t != null) {
                    // notify the future to close the connection.
                    if (!future.isDone()) {
                        if (!future.cancel(true)) {
                            future.addListener(f -> {
                                closeChannel(future);
                            });
                        }
                    } else {
                        closeChannel(future);
                    }
                }
            } catch (Exception ex) {
                logger.error("close channel exception", ex);
            }
        });
    }

    private static void closeChannel(ChannelFuture future) {
        if (future.channel() != null) {
            future.channel().close();
        }
    }

    private static void bindLifeCycle(ChannelFuture future,
            CompletableFuture<com.tencent.trpc.core.transport.Channel> target,
            ProtocolConfig config) {

        if (future.isSuccess()) {
            target.complete(NettyChannelManager.getOrAddChannel(future.channel(), config));
            return;
        }

        if (future.isCancelled()) {
            target.cancel(true);
            return;
        }

        if (future.isDone() && !future.isSuccess()) {
            target.completeExceptionally(TransportException.trans(future.cause()));
            return;
        }

        future.addListener(f -> {
            try {
                if (f.isSuccess()) {
                    target.complete(
                            NettyChannelManager.getOrAddChannel(future.channel(), config));
                } else {
                    target.completeExceptionally(TransportException.trans(f.cause()));
                }
            } catch (Exception ex) {
                logger.error("notify connectingFuture exception", ex);
            }
        });
    }
}