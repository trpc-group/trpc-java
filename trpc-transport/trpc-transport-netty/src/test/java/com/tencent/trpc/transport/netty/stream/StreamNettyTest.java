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

package com.tencent.trpc.transport.netty.stream;

import com.tencent.trpc.core.common.config.ProtocolConfig;
import com.tencent.trpc.core.stream.Closeable;
import com.tencent.trpc.core.stream.transport.ClientTransport;
import com.tencent.trpc.core.stream.transport.RpcConnection;
import com.tencent.trpc.core.stream.transport.ServerTransport;
import com.tencent.trpc.core.utils.NetUtils;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Assert;
import org.junit.Test;
import reactor.core.publisher.Mono;

public class StreamNettyTest {

    private static final String IP = "127.0.0.1";

    @Test
    public void testTransport() {
        ProtocolConfig protoConfig = getProtoConfig("tcp");

        AtomicInteger connected = new AtomicInteger();

        NettyServerTransportFactory serverTransportFactory = new NettyServerTransportFactory();
        ServerTransport<? extends Closeable> serverTransport =
                serverTransportFactory.create(protoConfig, () -> in -> in);
        Assert.assertNotNull(serverTransport);
        Mono<? extends Closeable> serverMono = serverTransport.start(conn -> {
            connected.incrementAndGet();
            return Mono.empty();
        });
        Assert.assertNotNull(serverMono);

        Closeable[] server = new Closeable[1];
        serverMono.log().doOnSuccess(svr -> server[0] = svr).block();
        Assert.assertNotNull(server[0]);

        NettyClientTransportFactory clientTransportFactory = new NettyClientTransportFactory();
        ClientTransport clientTransport = clientTransportFactory.create(protoConfig, () -> in -> in);
        Assert.assertNotNull(clientTransport);

        final RpcConnection[] conn = new RpcConnection[1];

        Mono<RpcConnection> connMono = clientTransport.connect();
        Assert.assertNotNull(connMono);
        connMono.doOnSuccess(c -> conn[0] = c).block();
        Assert.assertNotNull(conn[0]);

        conn[0].dispose();
        Assert.assertTrue(conn[0].isDisposed());
        server[0].dispose();
    }

    private ProtocolConfig getProtoConfig(String network) {
        ProtocolConfig protocolConfig = ProtocolConfig.newInstance();
        protocolConfig.setNetwork(network);
        protocolConfig.setIp(IP);
        protocolConfig.setPort(NetUtils.getAvailablePort());
        return protocolConfig;
    }

}
