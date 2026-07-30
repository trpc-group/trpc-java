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

package com.tencent.trpc.proto.support;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.tencent.trpc.core.common.config.ProtocolConfig;
import com.tencent.trpc.core.transport.Channel;
import com.tencent.trpc.core.transport.ChannelHandler;
import com.tencent.trpc.core.transport.ClientTransport;
import com.tencent.trpc.core.transport.codec.ChannelBuffer;
import com.tencent.trpc.core.transport.codec.ClientCodec;
import com.tencent.trpc.core.transport.handler.ChannelHandlerAdapter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;

/**
 * Unit tests for the "all channels disconnected" self-close path added to
 * {@link DefRpcClient}. We bypass the SPI-driven Netty transport by reflectively replacing
 * {@code DefRpcClient.transport} with a controllable stub, then drive the
 * {@code InternalHandler.disconnected} callback to assert exactly when the RpcClient
 * tears itself down.
 */
public class DefRpcClientSelfCloseTest {

    /**
     * Lazy initial state: no channel has ever connected. A stray disconnect callback must
     * NOT bring the proxy down, otherwise we would tear down a not-yet-bootstrapped
     * client.
     */
    @Test
    public void testNoOpWhenNeverConnected() throws Exception {
        DefRpcClient client = newClientWithStubTransport();
        StubTransport stub = stubOf(client);

        // connectedCnt remains 0; the lazy slot also reports "isNotYetConnect" so the
        // transport reports connected==true under the AbstractClientTransport semantics.
        // Our stub mirrors that: connected stays true initially.
        invokeDisconnected(client, fakeChannel());

        assertFalse("must not self-close while never connected", stub.closed.get());
        assertFalse("closeFuture must remain pending",
                client.closeFuture().toCompletableFuture().isDone());
    }

    /**
     * Partial outage: the transport still has at least one usable channel. A disconnect
     * for one of the other slots must NOT trigger a self-close — the cluster manager
     * relies on slot rebuild for that case.
     */
    @Test
    public void testNoSelfCloseWhilePartiallyConnected() throws Exception {
        DefRpcClient client = newClientWithStubTransport();
        StubTransport stub = stubOf(client);
        ChannelHandlerAdapter handler = handlerOf(client);

        bumpConnectedCnt(handler, 2);
        // One channel down but another still usable — transport still reports connected.
        stub.connected.set(true);

        invokeDisconnected(client, fakeChannel());

        assertFalse("must not self-close while another channel is up", stub.closed.get());
    }

    /**
     * Once {@code close()} is in progress (or done), further {@code disconnected}
     * callbacks must short-circuit so we do not pile up no-op tasks on the scheduler.
     */
    @Test
    public void testNoActionAfterAlreadyClosed() throws Exception {
        DefRpcClient client = newClientWithStubTransport();
        StubTransport stub = stubOf(client);

        client.close();
        assertTrue("close() should have run synchronously on the test thread",
                stub.closed.get());
        int closeCallsBeforeStrayEvent = stub.closeCallCount.get();

        invokeDisconnected(client, fakeChannel());

        assertEquals("post-close disconnect must be a no-op",
                closeCallsBeforeStrayEvent, stub.closeCallCount.get());
    }

    /* ---------------- helpers ---------------- */

    private static DefRpcClient newClientWithStubTransport() throws Exception {
        ProtocolConfig config = ProtocolConfig.newInstance();
        config.setIp("127.0.0.1");
        config.setPort(0);
        // The constructor builds a real Netty transport via SPI. We immediately swap it
        // out for a stub so the tests neither bind sockets nor depend on Netty timing.
        DefRpcClient client = new DefRpcClient(config, new NoOpClientCodec());
        replaceTransport(client, new StubTransport(config));
        // Drive the lifecycle to STARTED so a later close() actually runs stopInternal()
        // (which in turn invokes doClose + completes the closeFuture). Without this the
        // NEW -> STOPPED short-circuit in LifecycleBase.stop() would skip doClose.
        client.open();
        return client;
    }

    private static void replaceTransport(DefRpcClient client, ClientTransport replacement)
            throws Exception {
        Field f = DefRpcClient.class.getDeclaredField("transport");
        f.setAccessible(true);
        // Strip 'final' so the assignment sticks under JDK 8.
        Field modifiers = Field.class.getDeclaredField("modifiers");
        modifiers.setAccessible(true);
        modifiers.setInt(f, f.getModifiers() & ~java.lang.reflect.Modifier.FINAL);
        f.set(client, replacement);
    }

    private static StubTransport stubOf(DefRpcClient client) throws Exception {
        Field f = DefRpcClient.class.getDeclaredField("transport");
        f.setAccessible(true);
        return (StubTransport) f.get(client);
    }

    private static ChannelHandlerAdapter handlerOf(DefRpcClient client) throws Exception {
        Field f = DefRpcClient.class.getDeclaredField("handler");
        f.setAccessible(true);
        return (ChannelHandlerAdapter) f.get(client);
    }

    private static void bumpConnectedCnt(ChannelHandlerAdapter handler, int by)
            throws Exception {
        Field f = ChannelHandlerAdapter.class.getDeclaredField("connectedCnt");
        f.setAccessible(true);
        AtomicInteger cnt = (AtomicInteger) f.get(handler);
        cnt.addAndGet(by);
    }

    private static void invokeDisconnected(DefRpcClient client, Channel channel)
            throws Exception {
        ChannelHandler handler = handlerOf(client);
        Method m = handler.getClass().getMethod("disconnected", Channel.class);
        m.setAccessible(true);
        m.invoke(handler, channel);
    }

    private static Channel fakeChannel() {
        return new Channel() {
            @Override
            public CompletionStage<Void> send(Object message) {
                return null;
            }

            @Override
            public boolean isConnected() {
                return false;
            }

            @Override
            public InetSocketAddress getRemoteAddress() {
                return null;
            }

            @Override
            public InetSocketAddress getLocalAddress() {
                return null;
            }

            @Override
            public boolean isClosed() {
                return true;
            }

            @Override
            public CompletionStage<Void> close() {
                return java.util.concurrent.CompletableFuture.completedFuture(null);
            }

            @Override
            public ProtocolConfig getProtocolConfig() {
                return null;
            }
        };
    }

    /**
     * Controllable stand-in for {@link ClientTransport}; lets the test toggle
     * "isConnected" and observe how often {@link #close()} was called.
     */
    private static final class StubTransport implements ClientTransport {

        final ProtocolConfig config;
        final AtomicBoolean connected = new AtomicBoolean(true);
        final AtomicBoolean closed = new AtomicBoolean(false);
        final AtomicInteger closeCallCount = new AtomicInteger(0);

        StubTransport(ProtocolConfig config) {
            this.config = config;
        }

        @Override
        public void open() {
        }

        @Override
        public CompletionStage<Void> send(Object message) {
            return null;
        }

        @Override
        public CompletionStage<Channel> getChannel() {
            return null;
        }

        @Override
        public Set<Channel> getChannels() {
            return Collections.emptySet();
        }

        @Override
        public boolean isConnected() {
            return connected.get();
        }

        @Override
        public boolean isClosed() {
            return closed.get();
        }

        @Override
        public ChannelHandler getChannelHandler() {
            return null;
        }

        @Override
        public InetSocketAddress getRemoteAddress() {
            return null;
        }

        @Override
        public ProtocolConfig getProtocolConfig() {
            return config;
        }

        @Override
        public void close() {
            closed.set(true);
            closeCallCount.incrementAndGet();
        }
    }

    /**
     * Minimal codec — none of the tests actually exercise encode/decode.
     */
    private static final class NoOpClientCodec extends ClientCodec {

        @Override
        public void encode(Channel channel, ChannelBuffer channelBuffer, Object message) {
        }

        @Override
        public Object decode(Channel channel, ChannelBuffer channelBuffer) {
            return null;
        }
    }
}
