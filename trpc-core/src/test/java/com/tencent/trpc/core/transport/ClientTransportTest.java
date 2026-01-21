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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tencent.trpc.core.common.config.ProtocolConfig;
import com.tencent.trpc.core.transport.common.TestClientCodec;
import com.tencent.trpc.core.transport.common.TestClientChannelHandler;
import com.tencent.trpc.core.transport.impl.TestClientTransport;
import java.net.InetSocketAddress;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ClientTransportTest {

    private static final String HOST = "localhost";

    private static final int PORT = 8080;

    private static final String PROTOCOL = "trpc";

    private ClientTransport clientTransport;

    private ClientTransport clientNotPool;

    @BeforeEach
    public void before() throws Exception {
        clientTransport = newClientTransport(HOST, PORT, Boolean.TRUE);
        clientNotPool = newClientTransport(HOST, PORT, Boolean.FALSE);
    }

    private TestClientTransport newClientTransport(String ip, int port, boolean keepAlive) {
        ProtocolConfig protocolConfig = new ProtocolConfig();
        protocolConfig.setIp(HOST);
        protocolConfig.setPort(PORT);
        protocolConfig.setKeepAlive(keepAlive);
        TestClientChannelHandler clientChannelHandler = new TestClientChannelHandler();
        TestClientCodec clientCodec = new TestClientCodec();
        return new TestClientTransport(protocolConfig, clientChannelHandler, clientCodec);
    }

    @Test
    public void testClientTransportSend() {
        clientTransport.open();
        CompletionStage<Void> completionStage = clientTransport.send("123");
        assertNotNull(completionStage);
        completionStage.whenComplete((value, exception) -> assertNull(exception));
        clientTransport.close();
        clientNotPool.open();
        completionStage = clientNotPool.send("123");
        assertNotNull(completionStage);
        completionStage.whenComplete((value, exception) -> assertNull(exception));
        clientNotPool.close();
    }

    @Test
    public void testValueOfField() {
        this.clientTransport.open();
        InetSocketAddress address = clientTransport.getRemoteAddress();
        assertEquals(address.getHostName(), HOST);
        assertEquals(address.getPort(), PORT);
        ProtocolConfig protocolConfig = clientTransport.getProtocolConfig();
        assertEquals(protocolConfig.getProtocol(), PROTOCOL);
        this.clientTransport.close();
    }

    @Test
    public void testGetChannelUsedChannelPool() {
        clientTransport = newClientTransport(HOST, PORT, false);
        clientTransport.open();
        CompletionStage<Channel> channelCompletionStage = clientTransport.getChannel();
        channelCompletionStage.whenComplete((channel, throwable) -> {
            assertNotNull(channel);
            assertNull(throwable);
        });
        Set<Channel> channels = clientTransport.getChannels();
        assertTrue(channels.size() > 0);
        clientTransport.close();
    }

    @Test
    public void testGetChannelNotUsedChannelPool() {
        this.clientTransport.open();
        CompletionStage<Channel> channelCompletionStage = this.clientTransport.getChannel();
        channelCompletionStage.whenComplete((channel, throwable) -> {
            assertNotNull(channel);
            assertNull(throwable);
        });
        Set<Channel> channels = this.clientTransport.getChannels();
        assertTrue(channels.size() > 0);
        this.clientTransport.close();
    }


    @Test
    public void testClientTransportStart() {
        this.clientTransport.open();
        assertTrue(this.clientTransport.isConnected());
    }

    @Test
    public void testClientTransportStop() {
        this.clientTransport.open();
        this.clientTransport.close();
        assertTrue(clientTransport.isClosed());
    }
}
