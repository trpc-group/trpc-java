package com.tencent.trpc.transport.netty;

import com.tencent.trpc.core.common.Constants;
import com.tencent.trpc.core.common.config.ProtocolConfig;
import com.tencent.trpc.core.transport.handler.ChannelHandlerAdapter;
import com.tencent.trpc.core.utils.NetUtils;
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
        factory.create(configNetBlank, new ChannelHandlerAdapter() {
            @Override
            public void received(com.tencent.trpc.core.transport.Channel channel,
                                 Object message) {
            }
        }, new TransportServerCodecTest());
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
        factory.create(configNetBlank, new ChannelHandlerAdapter() {
            @Override
            public void received(com.tencent.trpc.core.transport.Channel channel,
                                 Object message) {
            }
        }, new TransportClientCodecTest());
    }

}