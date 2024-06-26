package com.tencent.trpc.transport.netty;

import com.tencent.trpc.core.common.Constants;
import com.tencent.trpc.core.common.config.ProtocolConfig;
import com.tencent.trpc.core.transport.ClientTransport;
import com.tencent.trpc.core.transport.ServerTransport;
import com.tencent.trpc.core.transport.handler.ChannelHandlerAdapter;
import com.tencent.trpc.core.utils.NetUtils;
import org.junit.Assert;
import org.junit.Test;


public class NettyTransportFactoryTest {


    @Test
    public void serverCheckArgument() {
        ProtocolConfig configNetTCP = new ProtocolConfig();
        configNetTCP.setIp(NetUtils.LOCAL_HOST);
        configNetTCP.setPort(18888);
        configNetTCP.setNetwork(Constants.NETWORK_TCP);
        NettyServerTransportFactory factory = new NettyServerTransportFactory();
        factory.create(configNetTCP, new ChannelHandlerAdapter() {
            @Override
            public void received(com.tencent.trpc.core.transport.Channel channel,
                                 Object message) {
            }
        }, new TransportServerCodecTest());

        ProtocolConfig configNetBlank = new ProtocolConfig();
        configNetBlank.setIp(NetUtils.LOCAL_HOST);
        configNetBlank.setPort(18888);
        configNetBlank.setNetwork(Constants.NETWORK_UDP);
        ServerTransport server = factory.create(configNetBlank, new ChannelHandlerAdapter() {
            @Override
            public void received(com.tencent.trpc.core.transport.Channel channel,
                                 Object message) {
            }
        }, new TransportServerCodecTest());
        Assert.assertNotNull(server);
    }


    @Test
    public void clientCheckArgument() {
        ProtocolConfig configNetUDP = new ProtocolConfig();
        configNetUDP.setIp(NetUtils.LOCAL_HOST);
        configNetUDP.setPort(18888);
        configNetUDP.setNetwork(Constants.NETWORK_UDP);
        NettyClientTransportFactory factory = new NettyClientTransportFactory();
        factory.create(configNetUDP, new ChannelHandlerAdapter() {
            @Override
            public void received(com.tencent.trpc.core.transport.Channel channel,
                                 Object message) {
            }
        }, new TransportClientCodecTest());
        ProtocolConfig configNetBlank = new ProtocolConfig();
        configNetBlank.setIp(NetUtils.LOCAL_HOST);
        configNetBlank.setPort(18888);
        configNetBlank.setNetwork(Constants.NETWORK_UDP);
        ClientTransport client = factory.create(configNetBlank, new ChannelHandlerAdapter() {
            @Override
            public void received(com.tencent.trpc.core.transport.Channel channel,
                                 Object message) {
            }
        }, new TransportClientCodecTest());
        Assert.assertNotNull(client);
    }

}