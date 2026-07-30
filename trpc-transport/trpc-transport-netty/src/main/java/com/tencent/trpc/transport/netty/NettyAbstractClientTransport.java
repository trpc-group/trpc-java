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
import com.tencent.trpc.core.transport.AbstractClientTransport;
import com.tencent.trpc.core.transport.Channel;
import com.tencent.trpc.core.transport.ChannelHandler;
import com.tencent.trpc.core.transport.codec.ClientCodec;
import com.tencent.trpc.core.utils.ConcurrentHashSet;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.concurrent.DefaultThreadFactory;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Common base for Netty-backed client transports.
 *
 * <p>Owns the shared {@link EventLoopGroup} pool model. The pool comes in two flavours —
 * a {@link NioEventLoopGroup} and an {@link EpollEventLoopGroup} — each backed by its own
 * reference counter so {@code ioThreadGroupShare=true} stays compatible with the
 * Linux/epoll path. The variant a transport joins is selected per-instance from
 * {@code Epoll.isAvailable() && config.useEpoll()}, decoupling the share-IO-pool decision
 * from the NIO/Epoll decision.</p>
 */
public abstract class NettyAbstractClientTransport extends AbstractClientTransport {

    /**
     * Shared-pool variant. {@link #NIO} is always available; {@link #EPOLL} is only used
     * on Linux when the user opts into {@code ioMode=epoll} and the netty-epoll native
     * library is loadable.
     */
    protected enum SharedGroupKind {
        NIO,
        EPOLL
    }

    private static final Object NIO_LOCK = new Object();
    private static final Object EPOLL_LOCK = new Object();

    /**
     * Reference counter for the shared {@link NioEventLoopGroup}.
     */
    protected static final AtomicInteger SHARE_NIO_USED_NUMS = new AtomicInteger(0);
    /**
     * Reference counter for the shared {@link EpollEventLoopGroup}.
     */
    protected static final AtomicInteger SHARE_EPOLL_USED_NUMS = new AtomicInteger(0);
    /**
     * Shared {@link NioEventLoopGroup}, lazily created.
     */
    protected static volatile NioEventLoopGroup SHARE_NIO_GROUP;
    /**
     * Shared {@link EpollEventLoopGroup}, lazily created.
     */
    protected static volatile EpollEventLoopGroup SHARE_EPOLL_GROUP;

    /**
     * Backwards-compatible alias for the NIO shared counter, retained for any external
     * test that reflects on the field.
     */
    protected static final AtomicInteger SHARE_EVENT_LOOP_GROUP_USED_NUMS = SHARE_NIO_USED_NUMS;
    /**
     * Backwards-compatible alias for the NIO shared group.
     */
    protected static volatile NioEventLoopGroup SHARE_EVENT_LOOP_GROUP;

    protected Bootstrap bootstrap;

    protected ConcurrentHashSet<Channel> channelSet = new ConcurrentHashSet<>();

    /**
     * The shared variant this transport joined, or {@code null} if it owns an independent
     * {@link EventLoopGroup}. Driven by {@code config.isIoThreadGroupShare()} together
     * with {@link #wantsEpoll(ProtocolConfig)}.
     */
    private final SharedGroupKind sharedKind;

    /**
     * Whether this transport has acquired a reference to the shared event loop group. Used to
     * guarantee idempotent release in {@link #doClose()} even if {@link #doOpen()} failed mid-way
     * or {@code close()} is invoked twice.
     */
    private volatile boolean shareGroupAcquired;

    public NettyAbstractClientTransport(ProtocolConfig config, ChannelHandler handler,
            ClientCodec clientCodec, String defaultThreadPoolName) {
        super(config, handler, clientCodec);
        // Acquire the shared event loop group eagerly when this transport will use it. The
        // acquisition is paired with release in doClose(). This ensures the reference counter
        // never goes negative and the group is never reclaimed while a not-yet-opened transport
        // still has a reference outstanding.
        if (Boolean.TRUE.equals(config.isIoThreadGroupShare())) {
            this.sharedKind = wantsEpoll(config) ? SharedGroupKind.EPOLL : SharedGroupKind.NIO;
            acquireSharedGroup(this.sharedKind, config.getIoThreads(), defaultThreadPoolName);
            this.shareGroupAcquired = true;
        } else {
            this.sharedKind = null;
        }
    }

    @Override
    protected void doClose() {
        if (bootstrap != null && !Boolean.TRUE.equals(config.isIoThreadGroupShare())) {
            // Independent group owned by this transport, shut it down here.
            bootstrap.config().group().shutdownGracefully();
        }
        // Release the shared group reference (idempotent: only release once per acquisition).
        if (shareGroupAcquired) {
            shareGroupAcquired = false;
            releaseSharedGroup(sharedKind);
        }
    }

    @Override
    public Set<Channel> getChannels() {
        Set<Channel> channels = new HashSet<>();
        for (Channel each : channelSet) {
            if (each.isConnected()) {
                channels.add(each);
            }
        }
        return channels;
    }

    /**
     * Whether the given config wants epoll AND the JVM has a working netty-epoll native
     * library. Subclasses use the same predicate when deciding the channel class.
     */
    protected static boolean wantsEpoll(ProtocolConfig config) {
        return Epoll.isAvailable() && config != null && config.useEpoll();
    }

    /**
     * Returns the shared event loop group this transport joined, or {@code null} when the
     * transport is not configured to share. Subclasses use this in {@code doOpen} to wire
     * the bootstrap to the right group.
     */
    protected EventLoopGroup getSharedEventLoopGroup() {
        if (sharedKind == null) {
            return null;
        }
        return sharedKind == SharedGroupKind.EPOLL ? SHARE_EPOLL_GROUP : SHARE_NIO_GROUP;
    }

    /**
     * Returns the shared variant this transport joined, or {@code null} for independent.
     */
    protected SharedGroupKind getSharedGroupKind() {
        return sharedKind;
    }

    /**
     * Acquire one reference to the requested shared group, lazily creating it. Always
     * paired with {@link #releaseSharedGroup(SharedGroupKind)} in close.
     */
    private static void acquireSharedGroup(SharedGroupKind kind, int ioThreads, String threadPoolName) {
        if (kind == SharedGroupKind.EPOLL) {
            synchronized (EPOLL_LOCK) {
                if (SHARE_EPOLL_GROUP == null) {
                    SHARE_EPOLL_GROUP = new EpollEventLoopGroup(
                            ioThreads, new DefaultThreadFactory(threadPoolName + "-Epoll")
                    );
                }
                SHARE_EPOLL_USED_NUMS.incrementAndGet();
            }
            return;
        }
        synchronized (NIO_LOCK) {
            if (SHARE_NIO_GROUP == null) {
                SHARE_NIO_GROUP = new NioEventLoopGroup(
                        ioThreads, new DefaultThreadFactory(threadPoolName)
                );
                SHARE_EVENT_LOOP_GROUP = SHARE_NIO_GROUP;
            }
            SHARE_NIO_USED_NUMS.incrementAndGet();
        }
    }

    /**
     * Release one reference to the shared group of the given variant. The group is shut
     * down only when the reference counter reaches zero. The whole
     * check-decrement-shutdown-nullify sequence is performed under the per-variant lock so
     * concurrent acquire/release calls cannot leak or double-shutdown the group.
     */
    private static void releaseSharedGroup(SharedGroupKind kind) {
        if (kind == null) {
            return;
        }
        if (kind == SharedGroupKind.EPOLL) {
            synchronized (EPOLL_LOCK) {
                int remaining = SHARE_EPOLL_USED_NUMS.decrementAndGet();
                if (remaining < 0) {
                    SHARE_EPOLL_USED_NUMS.set(0);
                    remaining = 0;
                }
                if (remaining == 0 && SHARE_EPOLL_GROUP != null) {
                    SHARE_EPOLL_GROUP.shutdownGracefully();
                    SHARE_EPOLL_GROUP = null;
                }
            }
            return;
        }
        synchronized (NIO_LOCK) {
            int remaining = SHARE_NIO_USED_NUMS.decrementAndGet();
            if (remaining < 0) {
                SHARE_NIO_USED_NUMS.set(0);
                remaining = 0;
            }
            if (remaining == 0 && SHARE_NIO_GROUP != null) {
                SHARE_NIO_GROUP.shutdownGracefully();
                SHARE_NIO_GROUP = null;
                SHARE_EVENT_LOOP_GROUP = null;
            }
        }
    }

    @Override
    public String toString() {
        return String.format("%s[remote:%s, isConnected=%b]", getClass().getName(), getRemoteAddress(), isConnected());
    }
}
