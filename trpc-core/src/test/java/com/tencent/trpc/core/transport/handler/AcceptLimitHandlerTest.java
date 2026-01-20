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

package com.tencent.trpc.core.transport.handler;

import com.tencent.trpc.core.common.config.ProtocolConfig;
import com.tencent.trpc.core.exception.TransportException;
import com.tencent.trpc.core.transport.Channel;
import com.tencent.trpc.core.transport.common.TestServerCodec;
import com.tencent.trpc.core.transport.impl.TestServerTransport;
import java.net.InetSocketAddress;
import java.util.concurrent.CompletionStage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class AcceptLimitHandlerTest {

    private AcceptLimitHandler acceptLimitHandler;

    private Channel channel;

    /**
     * Init AcceptLimitHandler & channel
     */
    @BeforeEach
    public void setUp() {
        ProtocolConfig config = new ProtocolConfig();
        config.setIp("localhost");
        config.setMaxConns(20);
        ChannelHandlerAdapter channelHandlerAdapter = new ChannelHandlerAdapter();
        acceptLimitHandler = new AcceptLimitHandler(channelHandlerAdapter,
                new TestServerTransport(config, channelHandlerAdapter, new TestServerCodec()));
        this.channel = new Channel() {
            @Override
            public CompletionStage<Void> send(Object message) throws TransportException {
                return null;
            }

            @Override
            public CompletionStage<Void> close() {
                return null;
            }

            @Override
            public boolean isClosed() {
                return false;
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
            public ProtocolConfig getProtocolConfig() {
                return null;
            }
        };
    }

    @Test
    public void testConnected() {
        acceptLimitHandler.connected(channel);
    }

    @Test
    public void testDisconnected() {
        acceptLimitHandler.disconnected(channel);
    }

    @Test
    public void testSend() {
        acceptLimitHandler.send(channel, new Object());
    }

    @Test
    public void testReceived() {
        acceptLimitHandler.received(channel, new Object());
    }

    @Test
    public void testCaught() {
        acceptLimitHandler.caught(channel, null);
    }

    @Test
    public void testDestroy() {
        acceptLimitHandler.destroy();
    }

}
