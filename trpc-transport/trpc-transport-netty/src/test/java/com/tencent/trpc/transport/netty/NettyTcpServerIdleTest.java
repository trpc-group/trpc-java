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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.tencent.trpc.core.common.config.ProtocolConfig;
import com.tencent.trpc.core.transport.Channel;
import com.tencent.trpc.core.transport.ClientTransport;
import com.tencent.trpc.core.transport.ServerTransport;
import com.tencent.trpc.core.transport.handler.ChannelHandlerAdapter;
import com.tencent.trpc.core.utils.NetUtils;
import io.netty.channel.ChannelPipeline;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;
import org.junit.Test;

/**
 * Verifies the server-side idle handler installation policy driven by
 * {@code NettyTcpServerTransport.resolveIdleTimeoutMills()}:
 * <ul>
 *     <li>When {@code idleTimeout > 0}, the {@code "server-idle"}
 *         {@link io.netty.handler.timeout.IdleStateHandler} IS installed on each accepted
 *         child channel, so the server can proactively close idle connections
 *         ({@code ALL_IDLE}). Verified end-to-end against a real accepted child channel.</li>
 *     <li>When {@code idleTimeout = 0}, the handler is NOT installed and the server never
 *         proactively closes a client connection due to idle. Verified end-to-end.</li>
 *     <li>The negative / {@code null} branches of {@code resolveIdleTimeoutMills()} are
 *         covered at unit level via reflection (see
 *         {@link #resolveIdleTimeoutMillsNormalizesNonPositiveAndNull()}), because an
 *         unset/null {@code idleTimeout} on {@link ProtocolConfig} gets re-populated to its
 *         {@code @ConfigProperty} default during {@code init()}, so it cannot reach the
 *         transport as {@code null} through the end-to-end path.</li>
 * </ul>
 */
public class NettyTcpServerIdleTest {

    private static final String IDLE_HANDLER_NAME = "server-idle";

    @Test
    public void idleTimeoutPositiveInstallsServerIdleHandler() throws Exception {
        runWithServerIdleTimeout(60_000, true);
    }

    @Test
    public void idleTimeoutZeroDoesNotInstallServerIdleHandler() throws Exception {
        runWithServerIdleTimeout(0, false);
    }

    /**
     * Directly exercises the private {@code resolveIdleTimeoutMills()} to cover the null,
     * negative, zero and positive normalisation branches — including the auto-unboxing NPE
     * guard for {@code null} that the end-to-end path cannot reach.
     */
    @Test
    public void resolveIdleTimeoutMillsNormalizesNonPositiveAndNull() throws Exception {
        assertEquals(0L, invokeResolveIdleTimeoutMills(null));
        assertEquals(0L, invokeResolveIdleTimeoutMills(-1));
        assertEquals(0L, invokeResolveIdleTimeoutMills(0));
        assertEquals(60_000L, invokeResolveIdleTimeoutMills(60_000));
    }

    /**
     * Reflectively construct a {@link NettyTcpServerTransport} with the given raw
     * {@code idleTimeout} and invoke its private {@code resolveIdleTimeoutMills()}.
     *
     * @param rawIdleTimeout the raw {@code idleTimeout} value to inject onto the config
     * @return the normalised idle timeout in milliseconds
     */
    private long invokeResolveIdleTimeoutMills(Integer rawIdleTimeout) throws Exception {
        int serverPort = NetUtils.getAvailablePort(NetUtils.LOCAL_HOST, 18888);
        ProtocolConfig serverConfig = new ProtocolConfig();
        serverConfig.setIp(NetUtils.LOCAL_HOST);
        serverConfig.setPort(serverPort);
        serverConfig.setNetwork("tcp");
        // Do NOT open() the transport: constructing it is enough to invoke the private
        // resolver, and skipping open() keeps the config's idleTimeout exactly as injected
        // (open()/init() would re-populate a null value with the @ConfigProperty default).
        NettyTcpServerTransport transport = new NettyTcpServerTransport(
                serverConfig, new ChannelHandlerAdapter(), new TransportServerCodecTest());

        // Inject the raw idleTimeout after construction so init() cannot overwrite it. The
        // field is declared on the BaseProtocolConfig superclass, so walk up the hierarchy.
        Field idleTimeoutField = findField(serverConfig.getClass(), "idleTimeout");
        assertNotNull("idleTimeout field must exist on the config hierarchy", idleTimeoutField);
        idleTimeoutField.setAccessible(true);
        idleTimeoutField.set(serverConfig, rawIdleTimeout);

        Method resolve = NettyTcpServerTransport.class
                .getDeclaredMethod("resolveIdleTimeoutMills");
        resolve.setAccessible(true);
        try {
            return (long) resolve.invoke(transport);
        } finally {
            try {
                transport.close();
            } catch (Throwable ignore) {
                // best-effort cleanup
            }
        }
    }

    /**
     * Boot a real Netty TCP server with the given server-side {@code idleTimeout}, connect
     * one client, then assert whether the accepted child channel's pipeline contains the
     * {@code "server-idle"} handler.
     *
     * @param serverIdleTimeout server-side idle timeout in milliseconds
     * @param expectInstalled {@code true} if the {@code server-idle} handler is expected
     */
    private void runWithServerIdleTimeout(int serverIdleTimeout, boolean expectInstalled)
            throws Exception {
        int serverPort = NetUtils.getAvailablePort(NetUtils.LOCAL_HOST, 18888);

        ProtocolConfig serverConfig = new ProtocolConfig();
        serverConfig.setIp(NetUtils.LOCAL_HOST);
        serverConfig.setPort(serverPort);
        serverConfig.setNetwork("tcp");
        serverConfig.setIdleTimeout(serverIdleTimeout);

        ServerTransport server = new NettyServerTransportFactory()
                .create(serverConfig, new ChannelHandlerAdapter(), new TransportServerCodecTest());
        server.open();

        ProtocolConfig clientConfig = new ProtocolConfig();
        clientConfig.setIp(NetUtils.LOCAL_HOST);
        clientConfig.setPort(serverPort);
        clientConfig.setNetwork("tcp");
        clientConfig.setIoThreadGroupShare(false);
        clientConfig.setLazyinit(false);
        // Disable client-side idle-close so it does not interfere with the assertion.
        clientConfig.setIdleTimeout(0);
        clientConfig.setConnsPerAddr(1);

        ClientTransport client = new NettyClientTransportFactory()
                .create(clientConfig, new ChannelHandlerAdapter(), new TransportClientCodecTest());
        try {
            client.open();
            // Force the connection to materialise so the server accepts a child channel.
            Channel clientChannel = client.getChannel().toCompletableFuture()
                    .get(2, TimeUnit.SECONDS);
            assertNotNull(clientChannel);
            assertTrue(clientChannel.isConnected());

            // Wait for the server to register the accepted child channel.
            NettyChannel accepted = awaitServerChannel(server);
            assertNotNull("server should have accepted one client channel", accepted);

            ChannelPipeline pipeline = accepted.getIoChannel().pipeline();
            if (expectInstalled) {
                assertNotNull("server-idle handler must be installed when idleTimeout > 0",
                        pipeline.get(IDLE_HANDLER_NAME));
            } else {
                assertNull("server-idle handler must NOT be installed when idleTimeout <= 0 or null",
                        pipeline.get(IDLE_HANDLER_NAME));
            }
        } finally {
            try {
                client.close();
            } catch (Throwable ignore) {
                // best-effort cleanup
            }
            try {
                server.close();
            } catch (Throwable ignore) {
                // best-effort cleanup
            }
        }
    }

    private NettyChannel awaitServerChannel(ServerTransport server) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 3_000;
        while (System.currentTimeMillis() < deadline) {
            for (Channel ch : server.getChannels()) {
                if (ch instanceof NettyChannel) {
                    return (NettyChannel) ch;
                }
            }
            Thread.sleep(50);
        }
        return null;
    }

    /**
     * Walk up the class hierarchy to locate a declared field by name.
     *
     * @param type the starting class
     * @param fieldName the field name to locate
     * @return the {@link Field} if found, otherwise {@code null}
     */
    private static Field findField(Class<?> type, String fieldName) {
        Class<?> current = type;
        while (current != null && current != Object.class) {
            try {
                return current.getDeclaredField(fieldName);
            } catch (NoSuchFieldException ignore) {
                current = current.getSuperclass();
            }
        }
        return null;
    }
}
