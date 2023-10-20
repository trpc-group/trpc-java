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

import static org.junit.Assert.assertTrue;

import com.tencent.trpc.core.common.config.ProtocolConfig;
import com.tencent.trpc.core.exception.TransportException;
import com.tencent.trpc.core.transport.codec.ClientCodec;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
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
