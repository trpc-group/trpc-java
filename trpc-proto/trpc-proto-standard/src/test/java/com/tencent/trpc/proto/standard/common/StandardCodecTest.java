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

package com.tencent.trpc.proto.standard.common;

import com.google.protobuf.ByteString;
import com.tencent.trpc.core.common.ConfigManager;
import com.tencent.trpc.core.common.config.ProtocolConfig;
import com.tencent.trpc.core.common.config.ServerConfig;
import com.tencent.trpc.core.compressor.CompressType;
import com.tencent.trpc.core.compressor.support.GZipCompressor;
import com.tencent.trpc.core.rpc.Request;
import com.tencent.trpc.core.rpc.RpcClientContext;
import com.tencent.trpc.core.rpc.RpcInvocation;
import com.tencent.trpc.core.rpc.common.RpcMethodInfo;
import com.tencent.trpc.core.rpc.def.DecodableValue;
import com.tencent.trpc.core.rpc.def.DefRequest;
import com.tencent.trpc.core.rpc.def.DefResponse;
import com.tencent.trpc.core.utils.Charsets;
import com.tencent.trpc.proto.standard.common.HelloRequestProtocol.HelloRequest;
import com.tencent.trpc.proto.standard.common.HelloRequestProtocol.HelloResponse;
import com.tencent.trpc.transport.netty.NettyChannel;
import com.tencent.trpc.transport.netty.NettyChannelBuffer;
import io.netty.buffer.UnpooledByteBufAllocator;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import org.apache.commons.lang3.ArrayUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class StandardCodecTest {

    @BeforeEach
    public void before() {
        ServerConfig serverConfig = new ServerConfig();
        serverConfig.setServer("abc");
        serverConfig.setLocalIp("127.0.0.1");
        ConfigManager.getInstance().setServerConfig(serverConfig);
    }

    @Test
    public void codecTest() throws NoSuchMethodException, SecurityException, UnsupportedEncodingException {
        RpcInvocation invocation = new RpcInvocation();
        invocation.setRpcMethodInfo(new RpcMethodInfo(GreeterClientApi.class,
                GreeterClientApi.class.getMethod("asyncSayHello", RpcClientContext.class, HelloRequest.class)));
        invocation.setArguments(new Object[]{
                HelloRequest.newBuilder().setMessage(ByteString.copyFromUtf8("hello standard")).build()});
        invocation.setRpcServiceName("helloservice");
        invocation.setRpcMethodName("sayHello");
        invocation.setFunc("/helloservice/sayHello");
        DefRequest clientRequest = new DefRequest();
        clientRequest.setInvocation(invocation);
        clientRequest.setRequestId(8889521);
        clientRequest.putAttachment("key", "value".getBytes());
        clientRequest.putAttachment("abc", "abc".getBytes());
        clientRequest.setContext(new RpcClientContext());
        ProtocolConfig config = new ProtocolConfig();
        config.setIp("127.0.0.1");
        config.setPort(125);
        config.setDefault();
        config.setCompressMinBytes(10);
        config.setCompressor(GZipCompressor.NAME);
        NettyChannel channel = new NettyChannel(null, config);
        NettyChannelBuffer nettyChannelBuffer = new NettyChannelBuffer(UnpooledByteBufAllocator.DEFAULT.buffer(65535));
        StandardClientCodec standardClientCodec = new StandardClientCodec();
        standardClientCodec.encode(channel, nettyChannelBuffer, clientRequest);
        StandardServerCodec standardServerCodec = new StandardServerCodec();

        Request serverRequest = (Request) standardServerCodec.decode(channel, nettyChannelBuffer);
        serverRequest.getAttachments().put("hello", "hello");
        TRpcReqHead tRpcReqHead = serverRequest.getAttachReqHead();
        Assertions.assertEquals(CompressType.GZIP, tRpcReqHead.getHead().getContentEncoding());
        Assertions.assertEquals(8889521, serverRequest.getRequestId());
        Assertions.assertEquals(serverRequest.getInvocation().getRpcMethodName(), "sayHello");
        Assertions.assertEquals(serverRequest.getInvocation().getRpcServiceName(), "helloservice");
        Assertions.assertTrue(ArrayUtils.isEquals(serverRequest.getAttachment("abc"), "abc".getBytes()));
        Object decode = ((DecodableValue) serverRequest.getInvocation().getArguments()[0])
                .decode(HelloRequest.class, false);
        Assertions.assertEquals(((HelloRequest) decode).getMessage().toStringUtf8(), "hello standard");
        Assertions.assertEquals(new String(((byte[]) serverRequest.getAttachment("key")), "UTF-8"), "value");
        Assertions.assertEquals(((TRpcReqHead) serverRequest.getAttachReqHead()).getHead().getCaller().toStringUtf8(),
                "trpc...");
        Assertions.assertEquals(((TRpcReqHead) serverRequest.getAttachReqHead()).getHead().getFunc().toStringUtf8(),
                "/helloservice/sayHello");
        DefResponse serverSendResponse = new DefResponse();
        serverSendResponse.setRequest(serverRequest);
        serverSendResponse.setRequestId(8889521);
        serverSendResponse.setValue(HelloResponse.newBuilder().setMessage(
                        ByteString.copyFrom("response".getBytes(Charsets.UTF_8)))
                .build());
        serverSendResponse.putAttachment("rsp-key", "value".getBytes());
        serverSendResponse.putAttachment("rsp-abc", "abc".getBytes());
        standardServerCodec.encode(channel, nettyChannelBuffer, serverSendResponse);

        DefResponse clientResponse = (DefResponse) standardClientCodec.decode(channel, nettyChannelBuffer);
        TRpcRspHead tRpcRspHead = clientResponse.getAttachRspHead();
        Assertions.assertEquals(CompressType.GZIP, tRpcRspHead.getHead().getContentEncoding());
        Assertions.assertEquals(clientResponse.getRequestId(), 8889521);
        Assertions.assertEquals(
                new String(((byte[]) clientResponse.getAttachment("rsp-key")), StandardCharsets.UTF_8),
                "value");
        Assertions.assertEquals(((TRpcRspHead) clientResponse.getAttachRspHead()).getHead().getCallType(),
                TRPCProtocol.TrpcCallType.TRPC_UNARY_CALL_VALUE);
        Object clientRspDecode = ((DecodableValue) clientResponse.getValue()).decode(HelloResponse.class, false);
        Assertions.assertEquals(((HelloResponse) clientRspDecode).getMessage().toStringUtf8(), "response");
        Assertions.assertEquals(new String((byte[]) clientResponse.getAttachment("key"), StandardCharsets.UTF_8),
                "value");
        Assertions.assertEquals(new String((byte[]) clientResponse.getAttachment("abc")), "abc");
        try {
            ((DecodableValue) clientResponse.getValue()).decode(DefResponse.class, false);
            Assertions.fail();
        } catch (Exception e) {
            Assertions.assertTrue(e instanceof RuntimeException);
            Assertions.assertTrue(e.getMessage().contains("deserialize to"));
        }
    }

    @Test
    public void compressTest() throws NoSuchMethodException, SecurityException, UnsupportedEncodingException {
        RpcInvocation invocation = new RpcInvocation();
        invocation.setRpcMethodInfo(new RpcMethodInfo(GreeterClientApi.class, GreeterClientApi.class
                .getMethod("asyncSayHello", RpcClientContext.class, HelloRequest.class)));
        invocation.setArguments(new Object[]{
                HelloRequest.newBuilder().setMessage(ByteString.copyFromUtf8("hello standard")).build()});
        invocation.setRpcServiceName("helloservice");
        invocation.setRpcMethodName("sayHello");
        invocation.setFunc("/helloservice/sayHello");
        DefRequest clientRequest = new DefRequest();
        clientRequest.setInvocation(invocation);
        clientRequest.setRequestId(8889521);
        clientRequest.putAttachment("key", "value".getBytes());
        clientRequest.putAttachment("abc", "abc".getBytes());
        clientRequest.setContext(new RpcClientContext());

        ProtocolConfig config = new ProtocolConfig();
        config.setIp("127.0.0.1");
        config.setPort(125);
        config.setDefault();
        config.setCompressMinBytes(1000);
        config.setCompressor(GZipCompressor.NAME);
        config.setSign("md5Sign");
        NettyChannel channel = new NettyChannel(null, config);
        NettyChannelBuffer nettyChannelBuffer = new NettyChannelBuffer(UnpooledByteBufAllocator.DEFAULT.buffer(65535));
        StandardClientCodec standardClientCodec = new StandardClientCodec();
        standardClientCodec.encode(channel, nettyChannelBuffer, clientRequest);
        StandardServerCodec standardServerCodec = new StandardServerCodec();

        Request serverRequest = (Request) standardServerCodec.decode(channel, nettyChannelBuffer);
        TRpcReqHead tRpcReqHead = serverRequest.getAttachReqHead();
        Assertions.assertEquals(CompressType.NONE, tRpcReqHead.getHead().getContentEncoding());
        Assertions.assertEquals(8889521, serverRequest.getRequestId());
        Assertions.assertEquals(serverRequest.getInvocation().getRpcMethodName(), "sayHello");
        Assertions.assertEquals(serverRequest.getInvocation().getRpcServiceName(), "helloservice");
        Assertions.assertTrue(ArrayUtils.isEquals(serverRequest.getAttachment("abc"), "abc".getBytes()));
        Object decode = ((DecodableValue) serverRequest.getInvocation().getArguments()[0])
                .decode(HelloRequest.class, false);
        Assertions.assertEquals(((HelloRequest) decode).getMessage().toStringUtf8(), "hello standard");
        Assertions.assertEquals(new String(((byte[]) serverRequest.getAttachment("key")), "UTF-8"),
                "value");
        Assertions.assertEquals(((TRpcReqHead) serverRequest.getAttachReqHead()).getHead().getCaller().toStringUtf8(),
                "trpc...");
        Assertions.assertEquals(((TRpcReqHead) serverRequest.getAttachReqHead()).getHead().getFunc().toStringUtf8(),
                "/helloservice/sayHello");

        DefResponse serverSendResponse = new DefResponse();
        serverSendResponse.setRequest(serverRequest);
        serverSendResponse.setRequestId(8889521);
        serverSendResponse.setValue(HelloResponse.newBuilder()
                .setMessage(ByteString.copyFrom("response".getBytes(Charsets.UTF_8))).build());
        serverSendResponse.putAttachment("rsp-key", "value".getBytes());
        serverSendResponse.putAttachment("rsp-abc", "abc".getBytes());
        standardServerCodec.encode(channel, nettyChannelBuffer, serverSendResponse);
        DefResponse clientResponse = (DefResponse) standardClientCodec.decode(channel, nettyChannelBuffer);
        TRpcReqHead tRpcReqHead1 = serverRequest.getAttachReqHead();
        Assertions.assertEquals(CompressType.NONE, tRpcReqHead1.getHead().getContentEncoding());
        Assertions.assertEquals(clientResponse.getRequestId(), 8889521);
        Assertions.assertEquals(new String(((byte[]) clientResponse.getAttachment("rsp-key")), "UTF-8"), "value");
        Assertions.assertEquals(((TRpcRspHead) clientResponse.getAttachRspHead()).getHead().getCallType(),
                TRPCProtocol.TrpcCallType.TRPC_UNARY_CALL_VALUE);
        Object clientRspDecode =
                ((DecodableValue) clientResponse.getValue()).decode(HelloResponse.class, false);
        Assertions.assertEquals(((HelloResponse) clientRspDecode).getMessage().toStringUtf8(), "response");
    }
}
