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

import com.tencent.trpc.core.common.config.ProtocolConfig;
import com.tencent.trpc.core.transport.codec.ChannelBuffer;
import com.tencent.trpc.core.transport.codec.ClientCodec;
import com.tencent.trpc.core.transport.codec.ServerCodec;

public class TransporterTestUtils {

    public static ProtocolConfig newProtocolConfig() {
        ProtocolConfig config = new ProtocolConfig();
        config.setIp("127.0.0.1");
        config.setPort(6666);
        return config;
    }

    public static ChannelHandler newChannelHandler() {
        return new ChannelHandler() {
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
            }
        };
    }

    public static ServerCodec newServerCodec() {
        return new ServerCodec() {

            @Override
            public void encode(Channel channel, ChannelBuffer channelBuffer, Object message) {

            }

            @Override
            public Object decode(Channel channel, ChannelBuffer channelBuffer) {
                return null;
            }
        };
    }

    public static ClientCodec newClientCodec() {
        return new ClientCodec() {

            @Override
            public void encode(Channel channel, ChannelBuffer channelBuffer, Object message) {

            }

            @Override
            public Object decode(Channel channel, ChannelBuffer channelBuffer) {
                return null;
            }
        };
    }
}