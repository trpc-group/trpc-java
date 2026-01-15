package com.tencent.trpc.core.transport.handler;

import com.tencent.trpc.core.common.config.ProtocolConfig;
import com.tencent.trpc.core.transport.common.TestChannel;
import org.junit.Test;

public class ChannelHandlerAdapterTest {

    @Test
    public void testDebug() {
        ChannelHandlerAdapter channelHandlerAdapter = new ChannelHandlerAdapter();
        ProtocolConfig protocolConfig = ProtocolConfig.newInstance();
        protocolConfig.setIp("127.0.0.1");
        protocolConfig.setPort(8888);
        channelHandlerAdapter.disconnected(new TestChannel(protocolConfig));
    }
}