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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.tencent.trpc.core.common.config.ProtocolConfig;
import com.tencent.trpc.core.exception.TransportException;
import com.tencent.trpc.core.transport.codec.ClientCodec;
import java.net.InetSocketAddress;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import org.junit.jupiter.api.Test;

public class AbstractClientTransportTest {

    /**
     * A message whose toString contains the format specifier characters, it is used to make sure the error message
     * is formatted only once.
     */
    private static final String MSG_WITH_PERCENT = "GET /a%2Fb?rate=100%";

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
     * The error message of a closed transport should carry the transport, the class and the message, and the format
     * specifiers must be replaced by the real arguments.
     */
    @Test
    public void testSendAfterClosedThrowsFormattedException() throws Exception {
        ClientTransportTest transport = newClosedTransport();
        try {
            transport.send("hello");
            fail("TransportException is expected");
        } catch (TransportException e) {
            String message = e.getMessage();
            assertNotNull(message);
            assertNull(e.getCause());
            assertTrue(message.contains(transport.toString()), message);
            assertTrue(message.contains(ClientTransportTest.class.getName()), message);
            assertTrue(message.contains("hello"), message);
            assertTrue(message.contains("send fail"), message);
            // all the format specifiers have been replaced
            assertTrue(!message.contains("%s"), message);
        }
    }

    /**
     * The message to be sent is untrusted, it may contain a percent sign. Formatting the error message twice would
     * throw a format exception and hide the real error, so it must be formatted only once.
     */
    @Test
    public void testSendAfterClosedWithPercentInMessage() throws Exception {
        ClientTransportTest transport = newClosedTransport();
        try {
            transport.send(MSG_WITH_PERCENT);
            fail("TransportException is expected");
        } catch (TransportException e) {
            assertTrue(e.getMessage().contains(MSG_WITH_PERCENT), e.getMessage());
        }
    }

    /**
     * The transport itself may also contain a percent sign, for example the remote address of a unix domain socket.
     */
    @Test
    public void testSendAfterClosedWithPercentInTransport() throws Exception {
        ClientTransportTest transport = newClosedTransport();
        transport.setDescription("transport-100%-desc");
        try {
            transport.send("hello");
            fail("TransportException is expected");
        } catch (TransportException e) {
            assertTrue(e.getMessage().contains("transport-100%-desc"), e.getMessage());
        }
    }

    @Test
    public void testGetChannelAfterClosedThrowsFormattedException() throws Exception {
        ClientTransportTest transport = newClosedTransport();
        transport.setDescription("transport-100%-desc");
        try {
            transport.getChannel();
            fail("TransportException is expected");
        } catch (TransportException e) {
            String message = e.getMessage();
            assertNotNull(message);
            assertNull(e.getCause());
            assertTrue(message.contains("transport-100%-desc"), message);
            assertTrue(message.contains(ClientTransportTest.class.getName()), message);
            assertTrue(message.contains("get channel fail"), message);
            assertTrue(!message.contains("%s"), message);
        }
    }

    /**
     * A null message must not break the error message building.
     */
    @Test
    public void testSendAfterClosedWithNullMessage() throws Exception {
        ClientTransportTest transport = newClosedTransport();
        try {
            transport.send(null);
            fail("TransportException is expected");
        } catch (TransportException e) {
            assertTrue(e.getMessage().contains("msg=null"), e.getMessage());
        }
    }

    /**
     * When a channel fails to be closed, the error should be logged with the channel item and the cause, and the
     * remaining steps(doClose and the handler destroying) should still be executed.
     *
     * <p>The lifecycle only executes stopInternal when it has left the new state, so the transport is opened first.
     * The open fails on purpose so that the state becomes FAILED, and then stop executes stopInternal.</p>
     */
    @Test
    public void testCloseLogsChannelCloseFailure() throws Exception {
        ClientTransportTest transport = new ClientTransportTest(TransporterTestUtils.newProtocolConfig(),
                new ThrowingDestroyChannelHandler(), TransporterTestUtils.newClientCodec(), true);
        transport.channels.add(new AbstractClientTransport.ChannelFutureItem(
                CompletableFuture.completedFuture(new ThrowingCloseChannel()),
                TransporterTestUtils.newProtocolConfig()));
        try {
            // the open fails, and the failed start triggers the stop which executes stopInternal
            transport.open();
            fail("TransportException is expected");
        } catch (TransportException e) {
            assertNotNull(e.getMessage());
        }
        assertTrue(transport.isClosed());
        // the channel close, the doClose and the handler destroying all failed, but they were all attempted
        assertTrue(transport.isDoCloseCalled());
    }

    private ClientTransportTest newClosedTransport() throws Exception {
        ClientTransportTest transport = new ClientTransportTest(TransporterTestUtils.newProtocolConfig(),
                TransporterTestUtils.newChannelHandler(), TransporterTestUtils.newClientCodec(), true);
        transport.close();
        assertTrue(transport.isClosed());
        return transport;
    }

    /**
     * A channel whose close always fails, it is used to trigger the error log of the channel closing.
     */
    private static class ThrowingCloseChannel implements Channel {

        @Override
        public CompletionStage<Void> close() {
            throw new IllegalStateException("close failed");
        }

        @Override
        public CompletionStage<Void> send(Object message) {
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public boolean isClosed() {
            return false;
        }

        @Override
        public boolean isConnected() {
            return true;
        }

        @Override
        public InetSocketAddress getRemoteAddress() {
            return new InetSocketAddress("127.0.0.1", 6666);
        }

        @Override
        public InetSocketAddress getLocalAddress() {
            return new InetSocketAddress("127.0.0.1", 6667);
        }

        @Override
        public ProtocolConfig getProtocolConfig() {
            return TransporterTestUtils.newProtocolConfig();
        }
    }

    private static class ClientTransportTest extends AbstractClientTransport {

        private boolean isTransportException;

        private String description;

        private boolean doCloseCalled;

        ClientTransportTest(ProtocolConfig config, ChannelHandler channelHandler,
                ClientCodec clientCodec, boolean isTransportException) throws TransportException {
            super(config, channelHandler, clientCodec);
            this.isTransportException = isTransportException;
        }

        void setDescription(String description) {
            this.description = description;
        }

        boolean isDoCloseCalled() {
            return doCloseCalled;
        }

        @Override
        public String toString() {
            return description == null ? super.toString() : description;
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
            doCloseCalled = true;
            throw new IllegalArgumentException();
        }

        @Override
        protected boolean useChannelPool() {
            return false;
        }

    }

    /**
     * A channel handler whose destroying always fails, it is used to trigger the error log of the handler
     * destroying.
     */
    private static class ThrowingDestroyChannelHandler implements ChannelHandler {

        @Override
        public void connected(Channel channel) {
        }

        @Override
        public void disconnected(Channel channel) {
        }

        @Override
        public void send(Channel channel, Object message) {
        }

        @Override
        public void received(Channel channel, Object message) {
        }

        @Override
        public void caught(Channel channel, Throwable exception) {
        }

        @Override
        public void destroy() {
            throw new IllegalStateException("destroy failed");
        }
    }
}
