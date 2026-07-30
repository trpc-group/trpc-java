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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import com.tencent.trpc.core.common.config.ProtocolConfig;
import com.tencent.trpc.core.transport.Channel;
import com.tencent.trpc.core.transport.handler.ChannelHandlerAdapter;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.Epoll;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Coverage for {@link NettyAbstractClientTransport}'s shared-group reference-count model:
 * the two-slot (NIO / Epoll) pool, idempotent close, the {@code shareGroupAcquired} guard
 * and the {@code wantsEpoll} predicate.
 *
 * <p>The tests use a no-op subclass that does not actually open any sockets, so the shared
 * group is acquired in the constructor and released in {@code doClose} without any Netty
 * channel activity in between.</p>
 */
public class NettyAbstractClientTransportTest {

    /**
     * Ensure each test starts from a clean shared-group state. The pool is a process-wide
     * singleton so leftovers from earlier tests in the JVM (or interleaved tests) would
     * otherwise distort the reference counter assertions below.
     */
    @Before
    public void setUp() throws Exception {
        resetSharedState();
    }

    @After
    public void tearDown() throws Exception {
        resetSharedState();
    }

    /**
     * NIO shared group: counter increments on construction, decrements on close, group is
     * lazily created and torn down only when the counter returns to zero.
     */
    @Test
    public void testNioSharedGroupReferenceCounting() {
        final ProtocolConfig c1 = newConfig(true, false);
        NoopTransport t1 = new NoopTransport(c1);
        t1.open();
        assertEquals(1, NettyAbstractClientTransport.SHARE_NIO_USED_NUMS.get());
        assertNotNull("first constructor must lazily create the NIO group",
                NettyAbstractClientTransport.SHARE_NIO_GROUP);
        EventLoopGroup grp = NettyAbstractClientTransport.SHARE_NIO_GROUP;

        final ProtocolConfig c2 = newConfig(true, false);
        NoopTransport t2 = new NoopTransport(c2);
        t2.open();
        assertEquals(2, NettyAbstractClientTransport.SHARE_NIO_USED_NUMS.get());
        assertSame("second constructor must reuse the existing group",
                grp, NettyAbstractClientTransport.SHARE_NIO_GROUP);

        t1.close();
        // First close drops the counter but keeps the group alive — t2 still references it.
        assertEquals(1, NettyAbstractClientTransport.SHARE_NIO_USED_NUMS.get());
        assertSame(grp, NettyAbstractClientTransport.SHARE_NIO_GROUP);

        t2.close();
        // Second close drops to zero — group must be released and slot nulled out.
        assertEquals(0, NettyAbstractClientTransport.SHARE_NIO_USED_NUMS.get());
        assertNull("group must be released when refcount returns to zero",
                NettyAbstractClientTransport.SHARE_NIO_GROUP);
    }

    /**
     * Independent (non-shared) mode: the shared counter is never touched, and {@code close()}
     * does not interact with the shared slots.
     */
    @Test
    public void testIndependentModeDoesNotTouchSharedCounter() {
        ProtocolConfig c = newConfig(false, false);
        int nioBefore = NettyAbstractClientTransport.SHARE_NIO_USED_NUMS.get();
        int epollBefore = NettyAbstractClientTransport.SHARE_EPOLL_USED_NUMS.get();

        NoopTransport t = new NoopTransport(c);
        t.open();
        assertEquals("independent mode must not touch NIO counter",
                nioBefore, NettyAbstractClientTransport.SHARE_NIO_USED_NUMS.get());
        assertEquals("independent mode must not touch EPOLL counter",
                epollBefore, NettyAbstractClientTransport.SHARE_EPOLL_USED_NUMS.get());

        t.close();
        assertEquals(nioBefore, NettyAbstractClientTransport.SHARE_NIO_USED_NUMS.get());
        assertEquals(epollBefore, NettyAbstractClientTransport.SHARE_EPOLL_USED_NUMS.get());
    }

    /**
     * The {@code shareGroupAcquired} guard makes {@code close()} idempotent: a second close
     * must NOT decrement the counter again. Without the guard the pool would be torn down
     * while another transport still holds a logical reference.
     */
    @Test
    public void testDoubleCloseIsIdempotent() {
        ProtocolConfig c = newConfig(true, false);
        NoopTransport t = new NoopTransport(c);
        t.open();
        assertEquals(1, NettyAbstractClientTransport.SHARE_NIO_USED_NUMS.get());

        t.close();
        assertEquals(0, NettyAbstractClientTransport.SHARE_NIO_USED_NUMS.get());

        // Second close is a no-op for the shared counter.
        t.close();
        assertEquals("idempotent close must not double-decrement",
                0, NettyAbstractClientTransport.SHARE_NIO_USED_NUMS.get());
    }

    /**
     * {@code wantsEpoll} guards: null config and non-epoll {@code ioMode} must both yield
     * false. The Linux/native availability check is JVM-environment-dependent so we only
     * assert the negative branches deterministically.
     */
    @Test
    public void testWantsEpollNegativeBranches() {
        assertFalse("null config must yield wantsEpoll=false",
                NoopTransport.wantsEpollPublic(null));
        ProtocolConfig nio = newConfig(true, false);
        assertFalse("ioMode=nio must yield wantsEpoll=false",
                NoopTransport.wantsEpollPublic(nio));
    }

    /**
     * {@code getSharedEventLoopGroup} returns null for independent transports, the NIO
     * instance for NIO-shared transports, and (when epoll is available) the EPOLL instance
     * for epoll-shared transports.
     */
    @Test
    public void testGetSharedEventLoopGroupRoutes() {
        // Independent — no shared group.
        NoopTransport indep = new NoopTransport(newConfig(false, false));
        indep.open();
        assertNull("independent transport must report no shared group",
                indep.getSharedEventLoopGroupPublic());
        indep.close();

        // NIO-shared.
        NoopTransport nio = new NoopTransport(newConfig(true, false));
        nio.open();
        EventLoopGroup nioGrp = nio.getSharedEventLoopGroupPublic();
        assertNotNull(nioGrp);
        assertSame(NettyAbstractClientTransport.SHARE_NIO_GROUP, nioGrp);
        nio.close();

        // EPOLL-shared (only when the JVM has the native lib loaded — otherwise we skip).
        if (Epoll.isAvailable()) {
            NoopTransport epoll = new NoopTransport(newConfig(true, true));
            epoll.open();
            EventLoopGroup epollGrp = epoll.getSharedEventLoopGroupPublic();
            assertNotNull(epollGrp);
            assertSame(NettyAbstractClientTransport.SHARE_EPOLL_GROUP, epollGrp);
            epoll.close();
            assertEquals(0, NettyAbstractClientTransport.SHARE_EPOLL_USED_NUMS.get());
        }
    }

    /**
     * The release path tolerates a counter that has somehow been driven below zero (e.g. by
     * an external test reset or a buggy manual close): it must clamp at zero rather than
     * leak a negative value into the next acquire cycle.
     */
    @Test
    public void testReleasePathClampsNegativeCounter() throws Exception {
        // Build then close one transport — this drives the counter through 1 → 0 normally.
        NoopTransport t = new NoopTransport(newConfig(true, false));
        t.open();
        t.close();
        assertEquals(0, NettyAbstractClientTransport.SHARE_NIO_USED_NUMS.get());

        // Force a stray release on a transport whose guard is artificially flipped: the
        // releaseSharedGroup path must clamp the counter at zero rather than let it dip
        // negative.
        NoopTransport t2 = new NoopTransport(newConfig(true, false));
        t2.open();
        Field acquired = NettyAbstractClientTransport.class
                .getDeclaredField("shareGroupAcquired");
        acquired.setAccessible(true);
        // Flip the guard so the next close still calls releaseSharedGroup, then close once
        // more so it tries to decrement from a counter that's already 0.
        t2.close();
        assertEquals(0, NettyAbstractClientTransport.SHARE_NIO_USED_NUMS.get());
        // Manually re-arm the guard and call doClose via reflection to drive the negative
        // path. doClose is protected — accessible directly because we're in the same package.
        acquired.setBoolean(t2, true);
        Method doClose = NettyAbstractClientTransport.class.getDeclaredMethod("doClose");
        doClose.setAccessible(true);
        doClose.invoke(t2);
        assertTrue("counter must never be negative",
                NettyAbstractClientTransport.SHARE_NIO_USED_NUMS.get() >= 0);
    }

    /**
     * Reset the shared state back to zero so each test sees a deterministic baseline.
     */
    private static void resetSharedState() throws Exception {
        EventLoopGroup nio = NettyAbstractClientTransport.SHARE_NIO_GROUP;
        if (nio != null) {
            nio.shutdownGracefully();
        }
        NettyAbstractClientTransport.SHARE_NIO_GROUP = null;
        NettyAbstractClientTransport.SHARE_EVENT_LOOP_GROUP = null;
        NettyAbstractClientTransport.SHARE_NIO_USED_NUMS.set(0);

        EventLoopGroup epoll = NettyAbstractClientTransport.SHARE_EPOLL_GROUP;
        if (epoll != null) {
            epoll.shutdownGracefully();
            NettyAbstractClientTransport.SHARE_EPOLL_GROUP = null;
        }
        NettyAbstractClientTransport.SHARE_EPOLL_USED_NUMS.set(0);
    }

    private static ProtocolConfig newConfig(boolean shared, boolean epoll) {
        ProtocolConfig config = new ProtocolConfig();
        config.setIp("127.0.0.1");
        config.setPort(65000);
        config.setNetwork("tcp");
        config.setIoThreadGroupShare(shared);
        config.setIoMode(epoll ? "epoll" : "nio");
        config.setIoThreads(1);
        return config;
    }

    /**
     * No-op transport that exercises the base-class constructor / close paths without
     * actually opening any sockets. {@code make()} is never called from these tests.
     */
    static class NoopTransport extends NettyAbstractClientTransport {

        NoopTransport(ProtocolConfig config) {
            super(config, new ChannelHandlerAdapter(), null, "Netty-Test-NoopTransport");
        }

        // Convenience pass-throughs to the protected helpers under test.
        EventLoopGroup getSharedEventLoopGroupPublic() {
            return getSharedEventLoopGroup();
        }

        static boolean wantsEpollPublic(ProtocolConfig config) {
            return wantsEpoll(config);
        }

        @Override
        protected void doOpen() {
            // Intentionally empty: tests only exercise constructor + close.
        }

        @Override
        protected CompletableFuture<Channel> make() {
            return new CompletableFuture<>();
        }

        @Override
        protected boolean useChannelPool() {
            return false;
        }

        @Override
        public Set<Channel> getChannels() {
            return null;
        }
    }
}
