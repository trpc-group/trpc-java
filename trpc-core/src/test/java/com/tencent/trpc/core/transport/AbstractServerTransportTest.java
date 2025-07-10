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

import static org.junit.Assert.assertTrue;

import com.tencent.trpc.core.common.config.ProtocolConfig;
import com.tencent.trpc.core.exception.TransportException;
import com.tencent.trpc.core.transport.codec.ServerCodec;
import java.util.Set;
import org.junit.Test;

public class AbstractServerTransportTest {

    @Test
    public void testOpenException() {
        ServerTransportTest test = new ServerTransportTest(TransporterTestUtils.newProtocolConfig(),
                TransporterTestUtils.newChannelHandler(), TransporterTestUtils.newServerCodec(), false);
        try {
            test.open();
            assertTrue(false);
        } catch (Exception ex) {
            assertTrue(ex instanceof TransportException && ex.getCause() instanceof IllegalArgumentException);
        }

        ServerTransportTest test2 = new ServerTransportTest(TransporterTestUtils.newProtocolConfig(),
                TransporterTestUtils.newChannelHandler(), TransporterTestUtils.newServerCodec(), true);
        try {
            test2.open();
            assertTrue(false);
        } catch (Exception ex) {
            assertTrue(ex instanceof TransportException && ex.getCause() == null);
        }
        test2.close();
        assertTrue(test2.isClosed());
        test2.toString();
    }


    private static class ServerTransportTest extends AbstractServerTransport {

        private boolean isTransportException;

        ServerTransportTest(ProtocolConfig config, ChannelHandler channelHandler,
                ServerCodec serverCodec, boolean isTransportException) throws TransportException {
            super(config, channelHandler, serverCodec);
            this.isTransportException = isTransportException;
        }

        @Override
        public Set<Channel> getChannels() {
            return null;
        }

        @Override
        public boolean isBound() {
            return false;
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
        protected void doClose() {
            throw new IllegalArgumentException();
        }

    }
}