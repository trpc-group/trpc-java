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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.tencent.trpc.core.common.config.ProtocolConfig;
import com.tencent.trpc.core.exception.TransportException;
import com.tencent.trpc.core.transport.codec.ClientCodec;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import org.junit.Test;

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
            assertTrue(message, message.contains(transport.toString()));
            assertTrue(message, message.contains(ClientTransportTest.class.getName()));
            assertTrue(message, message.contains("hello"));
            assertTrue(message, message.contains("send fail"));
            // all the format specifiers have been replaced
            assertTrue(message, !message.contains("%s"));
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
            assertTrue(e.getMessage(), e.getMessage().contains(MSG_WITH_PERCENT));
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
            assertTrue(e.getMessage(), e.getMessage().contains("transport-100%-desc"));
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
            assertTrue(message, message.contains("transport-100%-desc"));
            assertTrue(message, message.contains(ClientTransportTest.class.getName()));
            assertTrue(message, message.contains("get channel fail"));
            assertTrue(message, !message.contains("%s"));
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
            assertTrue(e.getMessage(), e.getMessage().contains("msg=null"));
        }
    }

    private ClientTransportTest newClosedTransport() throws Exception {
        ClientTransportTest transport = new ClientTransportTest(TransporterTestUtils.newProtocolConfig(),
                TransporterTestUtils.newChannelHandler(), TransporterTestUtils.newClientCodec(), true);
        transport.close();
        assertTrue(transport.isClosed());
        return transport;
    }

    private static class ClientTransportTest extends AbstractClientTransport {

        private boolean isTransportException;

        private String description;

        ClientTransportTest(ProtocolConfig config, ChannelHandler channelHandler,
                ClientCodec clientCodec, boolean isTransportException) throws TransportException {
            super(config, channelHandler, clientCodec);
            this.isTransportException = isTransportException;
        }

        void setDescription(String description) {
            this.description = description;
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
            throw new IllegalArgumentException();
        }

        @Override
        protected boolean useChannelPool() {
            return false;
        }

    }
}
