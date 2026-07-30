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
import static org.junit.Assert.assertTrue;

import com.tencent.trpc.core.common.config.ProtocolConfig;
import com.tencent.trpc.core.transport.handler.ChannelHandlerAdapter;
import com.tencent.trpc.core.utils.NetUtils;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.epoll.EpollChannelOption;
import java.lang.reflect.Method;
import java.util.Map;
import org.junit.Test;

/**
 * White-box coverage for {@link NettyTcpClientTransport}'s long-connection helpers:
 * {@code resolveIdleTimeoutMills}, {@code applyTcpKeepAliveTuning} and the pipeline
 * wiring driven by {@code idleTimeout}. The pipeline assertions go through a real
 * {@code doOpen} on an unbound bootstrap — no socket is ever opened.
 */
public class NettyTcpClientTransportTest {

    /**
     * {@code resolveIdleTimeoutMills} must return 0 for {@code null} / non-positive
     * configurations (legacy "disabled" semantics) and the raw value otherwise.
     */
    @Test
    public void testResolveIdleTimeoutMillsBranches() throws Exception {
        Method m = NettyTcpClientTransport.class.getDeclaredMethod("resolveIdleTimeoutMills");
        m.setAccessible(true);

        // Disabled — null / 0 / negative all collapse to 0.
        NettyTcpClientTransport tNull = newTransport(null);
        assertEquals(0L, ((Long) m.invoke(tNull)).longValue());
        tNull.close();

        NettyTcpClientTransport tZero = newTransport(0);
        assertEquals(0L, ((Long) m.invoke(tZero)).longValue());
        tZero.close();

        NettyTcpClientTransport tNeg = newTransport(-1);
        assertEquals(0L, ((Long) m.invoke(tNeg)).longValue());
        tNeg.close();

        // Enabled — raw value is returned.
        NettyTcpClientTransport tPos = newTransport(180_000);
        assertEquals(180_000L, ((Long) m.invoke(tPos)).longValue());
        tPos.close();
    }

    /**
     * Each TCP keepalive parameter is applied independently and only when strictly positive.
     * The test sets non-default values for all three and asserts they appear on the
     * bootstrap; a separate test below covers the no-op branches.
     */
    @Test
    public void testApplyTcpKeepAliveTuningSetsAllPositiveValues() throws Exception {
        ProtocolConfig config = newConfig(180_000);
        config.setTcpKeepAliveIdle(45);
        config.setTcpKeepAliveIntvl(15);
        config.setTcpKeepAliveCnt(7);

        NettyTcpClientTransport transport = new NettyTcpClientTransport(config,
                new ChannelHandlerAdapter(), null);
        try {
            Bootstrap bootstrap = new Bootstrap();
            invokeApplyTcpKeepAliveTuning(transport, bootstrap);
            Map<ChannelOption<?>, Object> opts = bootstrap.config().options();
            assertEquals(45, opts.get(EpollChannelOption.TCP_KEEPIDLE));
            assertEquals(15, opts.get(EpollChannelOption.TCP_KEEPINTVL));
            assertEquals(7, opts.get(EpollChannelOption.TCP_KEEPCNT));
        } finally {
            transport.close();
        }
    }

    /**
     * Non-positive / null values must NOT be propagated to the bootstrap — the kernel
     * default is preserved. This branch matters because mis-configured yamls (negative
     * values) would otherwise surface as netty option errors at connect time.
     */
    @Test
    public void testApplyTcpKeepAliveTuningSkipsNonPositive() throws Exception {
        ProtocolConfig config = newConfig(180_000);
        // Idle is positive and must be set; the other two are zero / negative and must NOT.
        config.setTcpKeepAliveIdle(30);
        config.setTcpKeepAliveIntvl(0);
        config.setTcpKeepAliveCnt(-1);

        NettyTcpClientTransport transport = new NettyTcpClientTransport(config,
                new ChannelHandlerAdapter(), null);
        try {
            Bootstrap bootstrap = new Bootstrap();
            invokeApplyTcpKeepAliveTuning(transport, bootstrap);
            Map<ChannelOption<?>, Object> opts = bootstrap.config().options();
            assertEquals(30, opts.get(EpollChannelOption.TCP_KEEPIDLE));
            assertNull("non-positive intvl must not be propagated",
                    opts.get(EpollChannelOption.TCP_KEEPINTVL));
            assertNull("negative cnt must not be propagated",
                    opts.get(EpollChannelOption.TCP_KEEPCNT));
        } finally {
            transport.close();
        }
    }

    /**
     * When {@code idleTimeout > 0}, {@code doOpen} must register both
     * {@code idleState} and {@code idleClose} pipeline handlers. Driven offline by
     * reflecting on the {@link io.netty.channel.ChannelInitializer} captured in the
     * bootstrap and invoking its {@code initChannel(Channel)} on a fresh
     * {@link io.netty.channel.embedded.EmbeddedChannel}; no real socket / EventLoop is
     * involved so the test is fully deterministic and isolated from JVM-level concurrency.
     */
    @Test
    public void testDoOpenInstallsIdlePipelineWhenEnabled() throws Exception {
        ChannelPipeline pipeline = pipelineAfterDoOpen(180_000);
        assertNotNull("idleState handler must be installed when idleTimeout > 0",
                pipeline.get("idleState"));
        assertNotNull("idleClose handler must be installed when idleTimeout > 0",
                pipeline.get("idleClose"));
    }

    /**
     * When {@code idleTimeout <= 0}, {@code doOpen} must skip both idle-related handlers —
     * the legacy "disabled" mode continues to work for one-way RPC callers.
     */
    @Test
    public void testDoOpenSkipsIdlePipelineWhenDisabled() throws Exception {
        ChannelPipeline pipeline = pipelineAfterDoOpen(0);
        assertNull("idleState handler must NOT be installed when idleTimeout = 0",
                pipeline.get("idleState"));
        assertNull("idleClose handler must NOT be installed when idleTimeout = 0",
                pipeline.get("idleClose"));
    }

    /**
     * {@code useChannelPool} reflects {@code keepAlive}. Default {@code keepAlive=true} →
     * pool, explicit {@code false} → no pool.
     */
    @Test
    public void testUseChannelPoolFollowsKeepAlive() throws Exception {
        ProtocolConfig poolCfg = newConfig(180_000);
        poolCfg.setKeepAlive(true);
        NettyTcpClientTransport withPool = new NettyTcpClientTransport(poolCfg,
                new ChannelHandlerAdapter(), null);
        try {
            assertTrue(invokeUseChannelPool(withPool));
        } finally {
            withPool.close();
        }

        ProtocolConfig noPoolCfg = newConfig(180_000);
        noPoolCfg.setKeepAlive(false);
        NettyTcpClientTransport noPool = new NettyTcpClientTransport(noPoolCfg,
                new ChannelHandlerAdapter(), null);
        try {
            assertFalse(invokeUseChannelPool(noPool));
        } finally {
            noPool.close();
        }
    }

    /**
     * Helper: invoke private {@code applyTcpKeepAliveTuning(Bootstrap)}.
     */
    private static void invokeApplyTcpKeepAliveTuning(NettyTcpClientTransport t,
            Bootstrap bootstrap) throws Exception {
        Method m = NettyTcpClientTransport.class
                .getDeclaredMethod("applyTcpKeepAliveTuning", Bootstrap.class);
        m.setAccessible(true);
        m.invoke(t, bootstrap);
    }

    /**
     * Helper: invoke protected {@code useChannelPool()}.
     */
    private static boolean invokeUseChannelPool(NettyTcpClientTransport t) throws Exception {
        Method m = NettyTcpClientTransport.class.getDeclaredMethod("useChannelPool");
        m.setAccessible(true);
        return (boolean) m.invoke(t);
    }

    /**
     * Drive {@code doOpen} on a transport configured with the given {@code idleTimeout},
     * pull the {@link io.netty.channel.ChannelInitializer} captured in the bootstrap and
     * reflectively run its {@code initChannel(Channel)} against a fresh
     * {@link io.netty.channel.embedded.EmbeddedChannel}. The resulting pipeline reflects
     * exactly what production code would install on a real connected channel — no real
     * socket / EventLoop / connect attempt is involved, so the result is deterministic
     * regardless of host load or other tests running in the same JVM.
     */
    private static ChannelPipeline pipelineAfterDoOpen(int idleTimeout) throws Exception {
        ProtocolConfig config = new ProtocolConfig();
        config.setIp(NetUtils.LOCAL_HOST);
        // Port number is irrelevant — we never actually connect.
        config.setPort(NetUtils.getAvailablePort());
        config.setNetwork("tcp");
        config.setConnsPerAddr(1);
        // lazyinit=true: open() must NOT fire a real connect; we only need doOpen() so the
        // bootstrap is configured with our ChannelInitializer.
        config.setLazyinit(true);
        config.setKeepAlive(true);
        config.setIoThreadGroupShare(false);
        config.setIdleTimeout(idleTimeout);

        NettyTcpClientTransport transport = new NettyTcpClientTransport(config,
                new ChannelHandlerAdapter(), new TransportClientCodecTest());
        try {
            // open() runs through LifecycleObj → doOpen(); with lazyinit=true no connect
            // is attempted. After this the bootstrap has our ChannelInitializer registered.
            transport.open();
            io.netty.channel.ChannelHandler initializer = transport.getBootstrap().config().handler();
            assertNotNull("doOpen must register a ChannelInitializer", initializer);

            // Use a fresh EmbeddedChannel as the target for the ChannelInitializer's
            // initChannel(Channel) — we invoke it reflectively rather than via netty's
            // own pipeline.add()/register() path so the result is fully deterministic and
            // independent of any other tests / EventLoop state in the same JVM.
            io.netty.channel.embedded.EmbeddedChannel ch = new io.netty.channel.embedded.EmbeddedChannel();
            // The anonymous ChannelInitializer subclass declares exactly one
            // initChannel(Channel) method — non-synthetic, non-bridge.
            Method initChannel = null;
            for (Method m : initializer.getClass().getDeclaredMethods()) {
                if ("initChannel".equals(m.getName()) && m.getParameterCount() == 1
                        && io.netty.channel.Channel.class.isAssignableFrom(m.getParameterTypes()[0])
                        && !m.isSynthetic() && !m.isBridge()) {
                    initChannel = m;
                    break;
                }
            }
            assertNotNull("ChannelInitializer must declare an initChannel(Channel) method",
                    initChannel);
            initChannel.setAccessible(true);
            initChannel.invoke(initializer, ch);

            // Return the live pipeline. NOTE: do NOT close the embedded channel — close()
            // detaches every handler, which would invalidate {@code pipeline.get(name)}.
            return ch.pipeline();
        } finally {
            try {
                transport.close();
            } catch (Throwable ignore) {
                // best-effort cleanup; the assertions above already captured the result
            }
        }
    }

    private static NettyTcpClientTransport newTransport(Integer idleTimeout) throws Exception {
        ProtocolConfig config = newConfig(idleTimeout);
        return new NettyTcpClientTransport(config, new ChannelHandlerAdapter(),
                new TransportClientCodecTest());
    }

    private static ProtocolConfig newConfig(Integer idleTimeout) {
        ProtocolConfig config = new ProtocolConfig();
        config.setIp(NetUtils.LOCAL_HOST);
        config.setPort(NetUtils.getAvailablePort());
        config.setNetwork("tcp");
        config.setConnsPerAddr(1);
        config.setLazyinit(true);
        config.setKeepAlive(true);
        // Independent EventLoopGroup so each test cleans up its own threads.
        config.setIoThreadGroupShare(false);
        // Always set idleTimeout explicitly: production default (Constants) is 180_000 and
        // the disabled-path tests need it overridden to 0 / negative.
        config.setIdleTimeout(idleTimeout == null ? -1 : idleTimeout);
        return config;
    }
}
