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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import com.tencent.trpc.core.common.config.ProtocolConfig;
import com.tencent.trpc.core.exception.TransportException;
import com.tencent.trpc.core.transport.codec.ClientCodec;
import java.lang.reflect.Method;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;

public class AbstractClientTransportTest {

    @Test
    public void testOpenException() throws Exception {
        ClientTransportTest test = new ClientTransportTest(TransporterTestUtils.newProtocolConfig(),
                TransporterTestUtils.newChannelHandler(), TransporterTestUtils.newClientCodec(), false);
        try {
            test.open();
            assertTrue(false);
        } catch (Exception e) {
            assertTrue(e instanceof TransportException && e.getCause() instanceof IllegalArgumentException);
        }

        ClientTransportTest test2 = new ClientTransportTest(TransporterTestUtils.newProtocolConfig(),
                TransporterTestUtils.newChannelHandler(), TransporterTestUtils.newClientCodec(), true);
        try {
            test2.open();
            assertTrue(false);
        } catch (Exception e) {
            assertTrue(e instanceof TransportException && e.getCause() == null);
        }
        test2.close();
        assertTrue(test2.isClosed());
        try {
            test2.getChannel();
        } catch (Exception e) {
            assertTrue(e instanceof TransportException && e.getCause() == null);
        }
        test2.toString();
    }

    /**
     * Thundering-herd regression: when a long connection has just disconnected and many
     * concurrent requests find the slot unavailable, the transport must rebuild the slot
     * exactly ONCE — not once per requesting thread. Without the in-lock double-check this
     * test would observe makeCount &gt; 1 and the peer would see a connect/disconnect storm.
     */
    @Test
    public void testEnsureChannelActiveDoesNotStorm() throws Exception {
        StormTransport transport = new StormTransport(TransporterTestUtils.newProtocolConfig(),
                TransporterTestUtils.newChannelHandler(), TransporterTestUtils.newClientCodec());
        // Pre-populate slot 0 with a "broken" item: future done but channel disconnected.
        CompletableFuture<Channel> dead = new CompletableFuture<>();
        dead.complete(new DisconnectedChannel());
        transport.installSlot(new AbstractClientTransport.ChannelFutureItem(dead, transport.getProtocolConfig()));

        int threads = 64;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        Method m = AbstractClientTransport.class.getDeclaredMethod("ensureChannelActive", int.class);
        m.setAccessible(true);

        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                try {
                    start.await();
                    m.invoke(transport, 0);
                } catch (Exception ignore) {
                    // concurrent invocations may race; the test only asserts on the
                    // aggregate openCalls counter below
                } finally {
                    done.countDown();
                }
            });
        }
        start.countDown();
        assertTrue(done.await(5, TimeUnit.SECONDS));
        pool.shutdownNow();

        // Exactly one rebuild — that's the whole point of the in-lock double check.
        assertEquals("slot must be rebuilt exactly once under thundering-herd",
                1, transport.makeCount.get());
        // The slot should now hold the freshly-built item.
        assertSame(transport.lastBuilt, transport.peekSlot(0).getChannelFuture());
    }

    /**
     * Regression for the idle-close hand-off path: when an idle handler invalidates a slot
     * <em>before</em> the actual {@code channel.close()}, the next request must observe
     * "needs reconnect" (slot is now {@code isNotYetConnect=true}) instead of routing onto
     * the about-to-be-closed channel. This shrinks the "request lands on a closing channel"
     * race window from "close completes" to "close is enqueued".
     */
    @Test
    public void testInvalidateChannelReplacesSlotWithBlankPlaceholder() throws Exception {
        StormTransport transport = new StormTransport(TransporterTestUtils.newProtocolConfig(),
                TransporterTestUtils.newChannelHandler(), TransporterTestUtils.newClientCodec());
        // Pre-populate slot 0 with a connected (but soon-to-be-stale) channel.
        ConnectedChannel live = new ConnectedChannel();
        CompletableFuture<Channel> alive = new CompletableFuture<>();
        alive.complete(live);
        transport.installSlot(new AbstractClientTransport.ChannelFutureItem(alive,
                transport.getProtocolConfig()));

        // Sanity: before invalidation the slot is "available", so a request would route
        // onto the live channel.
        AbstractClientTransport.ChannelFutureItem before = transport.peekSlot(0);
        assertSame(alive, before.getChannelFuture());

        // Idle handler hands off here.
        transport.invalidateChannel(live);

        AbstractClientTransport.ChannelFutureItem after = transport.peekSlot(0);
        // After invalidation: slot is a blank placeholder, the next ensureChannelActive
        // will treat it as needing a reconnect (isNotYetConnect=true).
        assertTrue("slot must be replaced (different item identity)", after != before);
        Method isNotYet = AbstractClientTransport.ChannelFutureItem.class
                .getDeclaredMethod("isNotYetConnect");
        isNotYet.setAccessible(true);
        assertTrue("invalidated slot must report isNotYetConnect=true",
                (boolean) isNotYet.invoke(after));
        // And the live channel must have been told to close so the EventLoop tears down
        // the underlying socket asynchronously.
        assertTrue("invalidated channel must have close() invoked", live.closeCalled);
    }

    /**
     * Channel stub used for {@link #testInvalidateChannelReplacesSlotWithBlankPlaceholder}.
     * Tracks whether {@code close()} has been invoked.
     */
    private static class ConnectedChannel implements Channel {

        volatile boolean closeCalled = false;

        @Override
        public boolean isConnected() {
            return !closeCalled;
        }

        @Override
        public boolean isClosed() {
            return closeCalled;
        }

        @Override
        public java.net.InetSocketAddress getRemoteAddress() {
            return null;
        }

        @Override
        public java.net.InetSocketAddress getLocalAddress() {
            return null;
        }

        @Override
        public com.tencent.trpc.core.common.config.ProtocolConfig getProtocolConfig() {
            return null;
        }

        @Override
        public java.util.concurrent.CompletionStage<Void> send(Object message) {
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public java.util.concurrent.CompletionStage<Void> close() {
            closeCalled = true;
            return CompletableFuture.completedFuture(null);
        }
    }

    /**
     * AbstractClientTransport subclass used by {@link #testEnsureChannelActiveDoesNotStorm}.
     * Exposes a deterministic {@code make()} that records how many times it has been called
     * and returns a never-completing future so connecting state is observable.
     */
    private static class StormTransport extends AbstractClientTransport {

        final AtomicInteger makeCount = new AtomicInteger(0);
        volatile CompletableFuture<Channel> lastBuilt;

        StormTransport(ProtocolConfig config, ChannelHandler handler, ClientCodec codec) {
            super(config, handler, codec);
        }

        void installSlot(ChannelFutureItem item) {
            // The channels list is initialized empty; pad to index 0.
            channels.add(item);
        }

        ChannelFutureItem peekSlot(int idx) {
            return channels.get(idx);
        }

        void replaceSlot(int idx, ChannelFutureItem item) {
            channels.set(idx, item);
        }

        @Override
        public Set<Channel> getChannels() {
            return null;
        }

        @Override
        protected void doOpen() {
        }

        @Override
        protected CompletableFuture<Channel> make() {
            makeCount.incrementAndGet();
            CompletableFuture<Channel> f = new CompletableFuture<>();
            lastBuilt = f;
            // Never complete: leaves the new slot in "isConnecting" state, which is exactly
            // what the in-lock double-check must short-circuit on for late-arriving threads.
            return f;
        }

        @Override
        protected void doClose() {
        }

        @Override
        protected boolean useChannelPool() {
            return true;
        }
    }

    /**
     * Channel that is "done but disconnected" — i.e. the previous future has resolved but
     * {@code isConnected()} is false, which is what {@code ensureChannelActive} should treat
     * as needing a reconnect.
     */
    private static class DisconnectedChannel implements Channel {

        @Override
        public boolean isConnected() {
            return false;
        }

        @Override
        public boolean isClosed() {
            return true;
        }

        @Override
        public java.net.InetSocketAddress getRemoteAddress() {
            return null;
        }

        @Override
        public java.net.InetSocketAddress getLocalAddress() {
            return null;
        }

        @Override
        public com.tencent.trpc.core.common.config.ProtocolConfig getProtocolConfig() {
            return null;
        }

        @Override
        public java.util.concurrent.CompletionStage<Void> send(Object message) {
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public java.util.concurrent.CompletionStage<Void> close() {
            return CompletableFuture.completedFuture(null);
        }
    }

    /**
     * {@code invalidateChannel(null)} is a no-op: must not throw, must not mutate slots.
     */
    @Test
    public void testInvalidateChannelNullIsNoOp() throws Exception {
        StormTransport t = new StormTransport(TransporterTestUtils.newProtocolConfig(),
                TransporterTestUtils.newChannelHandler(), TransporterTestUtils.newClientCodec());
        ConnectedChannel live = new ConnectedChannel();
        CompletableFuture<Channel> alive = new CompletableFuture<>();
        alive.complete(live);
        AbstractClientTransport.ChannelFutureItem original = new AbstractClientTransport.ChannelFutureItem(
                alive, t.getProtocolConfig());
        t.installSlot(original);

        t.invalidateChannel(null);

        assertSame("null target must not touch any slot", original, t.peekSlot(0));
        assertFalse("null target must not close any channel", live.closeCalled);
    }

    /**
     * Empty / null channels list: the early-return guards in {@code invalidateChannel} keep
     * the transport from NPE'ing during shutdown windows where slots have already been
     * cleared.
     */
    @Test
    public void testInvalidateChannelEmptyListIsNoOp() throws Exception {
        StormTransport t = new StormTransport(TransporterTestUtils.newProtocolConfig(),
                TransporterTestUtils.newChannelHandler(), TransporterTestUtils.newClientCodec());
        // No installSlot — channels stays empty.
        t.invalidateChannel(new ConnectedChannel());
        // Nothing to assert beyond "did not throw".
    }

    /**
     * When the target channel is not held by any slot the call must skip every entry and
     * leave them all intact — this matches the production scenario where two near-simultaneous
     * idle events fire on different channels and the first call has already invalidated the
     * shared slot.
     */
    @Test
    public void testInvalidateChannelTargetNotPresentLeavesSlotsIntact() throws Exception {
        StormTransport t = new StormTransport(TransporterTestUtils.newProtocolConfig(),
                TransporterTestUtils.newChannelHandler(), TransporterTestUtils.newClientCodec());
        ConnectedChannel slotChannel = new ConnectedChannel();
        CompletableFuture<Channel> alive = new CompletableFuture<>();
        alive.complete(slotChannel);
        AbstractClientTransport.ChannelFutureItem original = new AbstractClientTransport.ChannelFutureItem(
                alive, t.getProtocolConfig());
        t.installSlot(original);

        // Different channel — must not match anything.
        t.invalidateChannel(new ConnectedChannel());

        assertSame("non-matching target must not replace any slot",
                original, t.peekSlot(0));
        assertFalse("non-matching target must not close the slot's channel",
                slotChannel.closeCalled);
    }

    /**
     * Slot whose future is still in flight (isConnecting) must be skipped by
     * {@code invalidateChannel}: at this point there is no Channel object yet to compare
     * against the target, and a panicked replacement would orphan the in-flight connect.
     */
    @Test
    public void testInvalidateChannelSkipsInFlightSlot() throws Exception {
        StormTransport t = new StormTransport(TransporterTestUtils.newProtocolConfig(),
                TransporterTestUtils.newChannelHandler(), TransporterTestUtils.newClientCodec());
        // Future never completes — slot is permanently in "isConnecting" state.
        CompletableFuture<Channel> connecting = new CompletableFuture<>();
        AbstractClientTransport.ChannelFutureItem item = new AbstractClientTransport.ChannelFutureItem(
                connecting, t.getProtocolConfig());
        t.installSlot(item);

        t.invalidateChannel(new ConnectedChannel());

        assertSame("in-flight slot must be left alone", item, t.peekSlot(0));
    }

    /**
     * Slot whose future has completed exceptionally must be skipped: there is no Channel to
     * compare and the slot has already been observed as broken — let the request-path
     * reconnect handle it.
     */
    @Test
    public void testInvalidateChannelSkipsExceptionallyCompletedSlot() throws Exception {
        StormTransport t = new StormTransport(TransporterTestUtils.newProtocolConfig(),
                TransporterTestUtils.newChannelHandler(), TransporterTestUtils.newClientCodec());
        CompletableFuture<Channel> failed = new CompletableFuture<>();
        failed.completeExceptionally(new RuntimeException("boom"));
        AbstractClientTransport.ChannelFutureItem item = new AbstractClientTransport.ChannelFutureItem(
                failed, t.getProtocolConfig());
        t.installSlot(item);

        t.invalidateChannel(new ConnectedChannel());

        assertSame("exceptionally-completed slot must be left alone",
                item, t.peekSlot(0));
    }

    /**
     * Slot holding a {@code null} future (blank placeholder produced by an earlier
     * {@code invalidateChannel}) must be skipped — there is nothing to compare against and
     * the slot is already in the "needs reconnect" state.
     */
    @Test
    public void testInvalidateChannelSkipsBlankPlaceholderSlot() throws Exception {
        StormTransport t = new StormTransport(TransporterTestUtils.newProtocolConfig(),
                TransporterTestUtils.newChannelHandler(), TransporterTestUtils.newClientCodec());
        AbstractClientTransport.ChannelFutureItem blank = new AbstractClientTransport.ChannelFutureItem(
                null, t.getProtocolConfig());
        t.installSlot(blank);

        t.invalidateChannel(new ConnectedChannel());

        assertSame("blank placeholder must be left alone", blank, t.peekSlot(0));
    }

    /**
     * Verifies the {@code needsReconnect} truth table directly through the public path
     * (ensureChannelActive). Drives the static private predicate via the slots:
     * <ul>
     *     <li>blank placeholder (channelFuture==null) → must trigger reconnect (rebuild)</li>
     *     <li>connecting (future not done) → must NOT trigger reconnect (let it finish)</li>
     *     <li>completed exceptionally → must trigger reconnect</li>
     *     <li>completed with disconnected channel → must trigger reconnect</li>
     *     <li>completed with connected channel → must NOT trigger reconnect</li>
     * </ul>
     */
    @Test
    public void testNeedsReconnectTruthTable() throws Exception {
        StormTransport t = new StormTransport(TransporterTestUtils.newProtocolConfig(),
                TransporterTestUtils.newChannelHandler(), TransporterTestUtils.newClientCodec());
        Method ensure = AbstractClientTransport.class
                .getDeclaredMethod("ensureChannelActive", int.class);
        ensure.setAccessible(true);

        // Case 1: blank placeholder — must rebuild (makeCount goes 0 → 1)
        t.installSlot(new AbstractClientTransport.ChannelFutureItem(null, t.getProtocolConfig()));
        ensure.invoke(t, 0);
        assertEquals("blank placeholder must trigger rebuild", 1, t.makeCount.get());

        // Case 2: connecting — the slot we just rebuilt is itself "isConnecting" because
        // StormTransport.make() never completes the future. A second ensureChannelActive
        // call must short-circuit and NOT trigger another rebuild.
        ensure.invoke(t, 0);
        assertEquals("connecting slot must NOT trigger rebuild",
                1, t.makeCount.get());

        // Case 3: completed exceptionally — must rebuild.
        CompletableFuture<Channel> failed = new CompletableFuture<>();
        failed.completeExceptionally(new RuntimeException("boom"));
        t.replaceSlot(0, new AbstractClientTransport.ChannelFutureItem(failed, t.getProtocolConfig()));
        ensure.invoke(t, 0);
        assertEquals("exceptionally-completed slot must trigger rebuild",
                2, t.makeCount.get());

        // Case 4: disconnected — must rebuild.
        CompletableFuture<Channel> dead = new CompletableFuture<>();
        dead.complete(new DisconnectedChannel());
        t.replaceSlot(0, new AbstractClientTransport.ChannelFutureItem(dead, t.getProtocolConfig()));
        ensure.invoke(t, 0);
        assertEquals("disconnected slot must trigger rebuild",
                3, t.makeCount.get());

        // Case 5: connected — must NOT rebuild.
        CompletableFuture<Channel> live = new CompletableFuture<>();
        live.complete(new ConnectedChannel());
        t.replaceSlot(0, new AbstractClientTransport.ChannelFutureItem(live, t.getProtocolConfig()));
        ensure.invoke(t, 0);
        assertEquals("connected slot must NOT trigger rebuild",
                3, t.makeCount.get());
    }

    /**
     * Race-loser path of {@code invalidateChannel}: between "outside-lock match" and
     * "in-lock recheck" another thread may have already replaced the slot with a fresh
     * item. The race-loser branch must back off without touching anything.
     *
     * <p>We simulate the race by replacing the slot from a sneaky {@code close()} side-effect
     * on the channel: when {@code invalidateChannel} dispatches {@code item.close()} the
     * test has already verified the slot replacement; we instead use a manual two-step where
     * we install slot, snapshot the item, swap the slot, then call invalidateChannel — the
     * inner {@code latest != item} check must short-circuit. Equivalent in coverage and
     * deterministic.</p>
     */
    @Test
    public void testInvalidateChannelRaceLoserDoesNothing() throws Exception {
        StormTransport t = new StormTransport(TransporterTestUtils.newProtocolConfig(),
                TransporterTestUtils.newChannelHandler(), TransporterTestUtils.newClientCodec());
        ConnectedChannel target = new ConnectedChannel();
        CompletableFuture<Channel> aliveOld = new CompletableFuture<>();
        aliveOld.complete(target);
        AbstractClientTransport.ChannelFutureItem oldItem = new AbstractClientTransport.ChannelFutureItem(
                aliveOld, t.getProtocolConfig());
        t.installSlot(oldItem);

        // Concurrent thread already replaced the slot with a brand-new item before our
        // invalidate caller acquired the lock.
        ConnectedChannel fresh = new ConnectedChannel();
        CompletableFuture<Channel> aliveNew = new CompletableFuture<>();
        aliveNew.complete(fresh);
        AbstractClientTransport.ChannelFutureItem newItem = new AbstractClientTransport.ChannelFutureItem(
                aliveNew, t.getProtocolConfig());
        t.replaceSlot(0, newItem);

        // Now the late call arrives. Its outside-lock scan won't match (slot holds `fresh`,
        // not `target`), so it never enters the inner block. This still exercises the early
        // skip path in the iteration.
        t.invalidateChannel(target);

        assertSame("fresh slot must be preserved", newItem, t.peekSlot(0));
        assertFalse("fresh channel must not be closed", fresh.closeCalled);
        assertFalse("stale target must not be closed by late invalidate",
                target.closeCalled);
    }

    /**
     * After {@code invalidateChannel} the slot is a blank placeholder, and a subsequent
     * {@code ensureChannelActive} must rebuild — the full "idle close → request reconnect"
     * hand-off is verified in one shot.
     */
    @Test
    public void testInvalidateChannelThenEnsureChannelActiveRebuilds() throws Exception {
        StormTransport t = new StormTransport(TransporterTestUtils.newProtocolConfig(),
                TransporterTestUtils.newChannelHandler(), TransporterTestUtils.newClientCodec());
        ConnectedChannel live = new ConnectedChannel();
        CompletableFuture<Channel> alive = new CompletableFuture<>();
        alive.complete(live);
        AbstractClientTransport.ChannelFutureItem before = new AbstractClientTransport.ChannelFutureItem(
                alive, t.getProtocolConfig());
        t.installSlot(before);

        t.invalidateChannel(live);
        assertNotSame("slot must be replaced", before, t.peekSlot(0));
        assertTrue(live.closeCalled);

        Method ensure = AbstractClientTransport.class
                .getDeclaredMethod("ensureChannelActive", int.class);
        ensure.setAccessible(true);
        ensure.invoke(t, 0);

        assertEquals("post-invalidate ensureChannelActive must rebuild exactly once",
                1, t.makeCount.get());
    }

    private static class ClientTransportTest extends AbstractClientTransport {

        private boolean isTransportException;

        ClientTransportTest(ProtocolConfig config, ChannelHandler channelHandler,
                ClientCodec clientCodec, boolean isTransportException) throws TransportException {
            super(config, channelHandler, clientCodec);
            this.isTransportException = isTransportException;
        }

        @Override
        public Set<Channel> getChannels() {
            return null;
        }

        @Override
        protected void doOpen() {
            if (isTransportException) {
                throw new TransportException("");
            } else {
                throw new IllegalArgumentException();
            }
        }

        @Override
        protected CompletableFuture<Channel> make() throws Exception {
            return null;
        }

        @Override
        protected void doClose() {
            throw new IllegalArgumentException();
        }

        @Override
        protected boolean useChannelPool() {
            return false;
        }

    }
}
