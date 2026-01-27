package com.tencent.trpc.transport.netty;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

import com.tencent.trpc.core.common.config.ProtocolConfig;
import com.tencent.trpc.core.exception.ErrorCode;
import com.tencent.trpc.core.exception.TRpcException;
import com.tencent.trpc.core.exception.TransportException;
import com.tencent.trpc.core.transport.codec.Codec;
import io.netty.buffer.AbstractByteBufAllocator;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.DecoderException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import java.nio.charset.StandardCharsets;

public class NettyCodecAdapterTest {

    @Test
    public void testTcpDecodeIllegalPacket1() {
        Codec codec = mock(Codec.class);
        doThrow(TRpcException.newFrameException(ErrorCode.TRPC_CLIENT_DECODE_ERR, "the request protocol is not trpc"))
                .when(codec).decode(any(), any());

        ProtocolConfig protocolConfig = new ProtocolConfig();
        // set batchDecoder true
        protocolConfig.setBatchDecoder(true);
        NettyCodecAdapter nettyCodecAdapter = NettyCodecAdapter.createTcpCodecAdapter(codec, protocolConfig);

        ChannelHandler decoder = nettyCodecAdapter.getDecoder();
        EmbeddedChannel embeddedChannel = new EmbeddedChannel();
        embeddedChannel.pipeline().addLast(decoder);

        ByteBuf byteBuf = AbstractByteBufAllocator.DEFAULT.heapBuffer();
        byteBuf.writeBytes("testTcpDecodeIllegalPacket1".getBytes(StandardCharsets.UTF_8));

        // write illegal packet
        EmbeddedChannel tmpEmbeddedChannel = embeddedChannel;
        DecoderException decoderException = Assertions.assertThrows(DecoderException.class, () -> {
            tmpEmbeddedChannel.writeInbound(byteBuf);
        });

        Assertions.assertTrue(decoderException.getCause() instanceof TransportException);
        Assertions.assertEquals(byteBuf.refCnt(), 0);
    }

    @Test
    public void testTcpDecodeIllegalPacket2() {
        Codec codec = mock(Codec.class);
        doThrow(TRpcException.newFrameException(ErrorCode.TRPC_CLIENT_DECODE_ERR, "the request protocol is not trpc"))
                .when(codec).decode(any(), any());

        ProtocolConfig protocolConfig = new ProtocolConfig();
        // set batchDecoder false
        protocolConfig.setBatchDecoder(false);
        NettyCodecAdapter nettyCodecAdapter = NettyCodecAdapter.createTcpCodecAdapter(codec, protocolConfig);

        ChannelHandler decoder = nettyCodecAdapter.getDecoder();
        EmbeddedChannel embeddedChannel = new EmbeddedChannel();
        embeddedChannel.pipeline().addLast(decoder);

        ByteBuf byteBuf = AbstractByteBufAllocator.DEFAULT.heapBuffer();
        byteBuf.writeBytes("testTcpDecodeIllegalPacket1".getBytes(StandardCharsets.UTF_8));

        // write illegal packet
        EmbeddedChannel tmpEmbeddedChannel = embeddedChannel;
        DecoderException decoderException = Assertions.assertThrows(DecoderException.class, () -> {
            tmpEmbeddedChannel.writeInbound(byteBuf);
        });

        Assertions.assertTrue(decoderException.getCause() instanceof TransportException);
        Assertions.assertEquals(byteBuf.refCnt(), 0);
    }
}
