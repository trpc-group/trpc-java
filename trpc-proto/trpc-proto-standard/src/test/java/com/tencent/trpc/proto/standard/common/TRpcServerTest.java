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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.tencent.trpc.core.common.ConfigManager;
import com.tencent.trpc.core.common.TRpcProtocolType;
import com.tencent.trpc.core.common.config.BackendConfig;
import com.tencent.trpc.core.common.config.ConsumerConfig;
import com.tencent.trpc.core.common.config.ProtocolConfig;
import com.tencent.trpc.core.common.config.ProviderConfig;
import com.tencent.trpc.core.common.config.ServiceConfig;
import com.tencent.trpc.core.exception.ErrorCode;
import com.tencent.trpc.core.exception.TRpcException;
import com.tencent.trpc.core.exception.TransportException;
import com.tencent.trpc.core.rpc.*;
import com.tencent.trpc.core.transport.Channel;
import com.tencent.trpc.core.transport.handler.ChannelHandlerAdapter;
import com.tencent.trpc.core.utils.Charsets;
import com.tencent.trpc.core.utils.NetUtils;
import com.tencent.trpc.proto.standard.client.StandardRpcClientFactory;
import com.tencent.trpc.proto.standard.common.HelloRequestProtocol.HelloRequest;
import com.tencent.trpc.proto.standard.common.HelloRequestProtocol.HelloResponse;
import com.tencent.trpc.proto.standard.server.StandardRpcServerFactory;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.commons.codec.binary.StringUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class TRpcServerTest {

    ServiceConfig serviceConfig;
    ProviderConfig<GreeterService> providerConfig;
    ConsumerConfig<GreeterClientApi> clientConfig;
    BackendConfig backendConfig;
    RpcClient createRpcClient;
    ConsumerConfig<HelloServiceApi> helloClientConfig;

    @Before
    public void before() {
        ConfigManager.stopTest();
        ConfigManager.getInstance().startTest();
        providerConfig = new ProviderConfig<>();
        providerConfig.setServiceInterface(GreeterService.class);
        providerConfig.setRef(new GreeterServiceImp());
        serviceConfig = new ServiceConfig();
        String host = NetUtils.LOCAL_HOST;
        int port = NetUtils.getAvailablePort();
        serviceConfig.setIp(host);
        serviceConfig.setPort(port);
        serviceConfig.setEnableLinkTimeout(true);
        serviceConfig.addProviderConfig(providerConfig);
        ProviderConfig<HelloService> tempProviderConfig = new ProviderConfig();
        tempProviderConfig.setServiceInterface(HelloService.class);
        tempProviderConfig.setRef(new HelloServiceImpl());
        serviceConfig.addProviderConfig(tempProviderConfig);
        serviceConfig.export();
        ProtocolConfig copy = serviceConfig.getProtocolConfig().clone();
        copy.setPort(NetUtils.getAvailablePort());
        createRpcClient = new StandardRpcClientFactory().createRpcClient(copy);
        createRpcClient.open();
        clientConfig = new ConsumerConfig<>();
        backendConfig = new BackendConfig();
        backendConfig.setNamingUrl("ip://" + host + ":" + port);
        clientConfig.setBackendConfig(backendConfig);
        clientConfig.setServiceInterface(GreeterClientApi.class);
        BackendConfig helloBackendConfig = new BackendConfig();
        String nameingUrl = "ip://" + host + ":" + port;
        helloBackendConfig.setNamingUrl(nameingUrl);
        helloClientConfig = new ConsumerConfig<>();
        helloClientConfig.setServiceInterface(HelloServiceApi.class);
        helloClientConfig.setBackendConfig(helloBackendConfig);
    }

    @After
    public void after() {
        ConfigManager.stopTest();
        if (serviceConfig != null) {
            serviceConfig.unExport();
        }
        if (backendConfig != null) {
            backendConfig.stop();
        }
        RpcServerManager.shutdown();
        createRpcClient.close();
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void oneWayTest() {
        RpcClientContext context = new RpcClientContext();
        context.getReqAttachMap().put("key", "abc");
        context.setTimeoutMills(100000);
        context.setOneWay(true);

        HelloRequest request = HelloRequest.newBuilder().setMessage(ByteString.copyFrom("normal1".getBytes()))
                .build();
        GreeterClientApi proxy = clientConfig.getProxy();
        HelloResponse sayHello = proxy.sayHello(context, request);
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        assertNull(sayHello);

    }

    @Test
    public void aliasTest() {
        RpcClientContext context = new RpcClientContext();
        context.getReqAttachMap().put("key", "abcd");
        context.setTimeoutMills(100000);
        context.setRpcMethodAlias("/tencent_trpc_GreeterService_sayHello");
        HelloRequest request = HelloRequest.newBuilder().setMessage(ByteString.copyFrom("normal2".getBytes()))
                .build();
        GreeterClientApi proxy = clientConfig.getProxy();
        HelloResponse sayHello = proxy.sayHello(context, request);
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        assertEquals(sayHello.getMessage().toStringUtf8(), "normal2");
    }

    @Test
    public void aliasCalleeInfoTest() {
        RpcClientContext context = new RpcClientContext();
        context.getReqAttachMap().put("key", "abcd");
        context.setTimeoutMills(100000);
        context.setRpcMethodAlias("/tencent_trpc_GreeterService_sayHello");
        HelloRequest request = HelloRequest.newBuilder().setMessage(ByteString.copyFrom("alias1".getBytes()))
                .build();
        GreeterClientApi proxy = clientConfig.getProxy();
        HelloResponse sayHello = proxy.sayHello(context, request);
        assertEquals(sayHello.getMessage().toStringUtf8(), "sayHello");
    }

    @Test
    public void invalidRequestTest() throws Exception {
        ProtocolConfig protoConfig = ProtocolConfig.newInstance();
        protoConfig.setProtocolType(TRpcProtocolType.STANDARD.getName());
        protoConfig.setIp("127.0.0.1");
        protoConfig.setPort(NetUtils.getAvailablePort());
        ProtocolConfig protoConfigCopy = protoConfig.clone();
        protoConfigCopy.setPort(NetUtils.getAvailablePort());
        RpcClient createRpcClient = new StandardRpcClientFactory().createRpcClient(protoConfigCopy);

        ProtocolConfig protoConfigCopy2 = protoConfig.clone();
        protoConfigCopy2.setPort(NetUtils.getAvailablePort());
        RpcServer createRpcServer = new StandardRpcServerFactory().createRpcServer(protoConfigCopy2);

        Field clientFiled = createRpcClient.getClass().getDeclaredField("handler");
        clientFiled.setAccessible(true);
        ((ChannelHandlerAdapter) (clientFiled.get(createRpcClient))).received(null, "abc");

        Field serverField = createRpcServer.getClass().getDeclaredField("handler");
        serverField.setAccessible(true);
        ChannelHandlerAdapter channelHandlerAdapter =
                (ChannelHandlerAdapter) (serverField.get(createRpcServer));
        channelHandlerAdapter.received(null, "abc");
    }

    @Test
    public void noconnectTest() {
        serviceConfig.unExport();
        RpcServerManager.shutdown();
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        GreeterClientApi proxy = clientConfig.getProxy();
        RpcClientContext context = new RpcClientContext();
        context.getReqAttachMap().put("key", "abc");
        context.setTimeoutMills(100000);
        HelloRequest request =
                HelloRequest.newBuilder().setMessage(ByteString.copyFrom("normal1".getBytes()))
                        .build();
        Exception realEx = null;
        try {
            proxy.sayHello(context, request);
        } catch (Exception ex) {
            realEx = ex;
            ex.printStackTrace();
        }
        assertTrue(realEx instanceof TRpcException
                && ((TRpcException) realEx).getCode() == ErrorCode.TRPC_CLIENT_NETWORK_ERR);
    }

    @Test
    public void serverNormalTest() {
        GreeterClientApi proxy = clientConfig.getProxy();
        RpcClientContext context = new RpcClientContext();
        context.getReqAttachMap().put("key", "abc".getBytes());
        context.setTimeoutMills(1000000);
        HelloRequest request =
                HelloRequest.newBuilder().setMessage(ByteString.copyFrom("normal1".getBytes()))
                        .build();
        HelloResponse sayHello = proxy.sayHello(context, request);
        assertEquals(sayHello.getMessage().toStringUtf8(), "normal1");
        context.setTimeoutMills(1000000);
        HelloRequest request2 =
                HelloRequest.newBuilder().setMessage(ByteString.copyFrom("normal2".getBytes()))
                        .build();

        HelloResponse sayHello2 = proxy.asyncSayHello(context, request2).toCompletableFuture()
                .join();
        assertEquals(sayHello2.getMessage().toStringUtf8(), "normal2");
        System.out
                .println(">>>>>>>>>" + new String((byte[]) (context.getRspAttachMap().get("key"))));
        assertEquals(new String((byte[]) (context.getRspAttachMap().get("key")), Charsets.UTF_8),
                "abc");
        assertEquals("127.0.0.1",context.getValueMap().get(RpcContextValueKeys.CTX_CALLEE_REMOTE_IP));
    }

    @Test
    public void attachmentTest() {
        RpcClientContext context = new RpcClientContext();
        context.getReqAttachMap().put("key", "attachment".getBytes());
        context.setTimeoutMills(1000000);
        context.setRequestUncodecDataSegment("requestAttachment".getBytes(StandardCharsets.UTF_8));
        HelloRequest request = HelloRequest.newBuilder()
                .setMessage(ByteString.copyFrom("attachment".getBytes())).build();
        GreeterClientApi proxy = clientConfig.getProxy();
        HelloResponse sayHello = proxy.sayHello(context, request);
        byte[] responseAttachment = context.getResponseUncodecDataSegment();
        assertEquals("attachment", sayHello.getMessage().toStringUtf8());
        assertEquals("responseAttachment", new String(responseAttachment));
    }

    @Test
    public void responseAttachmentTest() {
        RpcClientContext context = new RpcClientContext();
        context.getReqAttachMap().put("key", "attachment".getBytes());
        context.setTimeoutMills(1000000);
        HelloRequest request = HelloRequest.newBuilder()
                .setMessage(ByteString.copyFrom("attachment".getBytes())).build();
        GreeterClientApi proxy = clientConfig.getProxy();
        HelloResponse sayHello = proxy.sayHello(context, request);
        byte[] responseAttachment = context.getResponseUncodecDataSegment();
        assertEquals("attachment", sayHello.getMessage().toStringUtf8());
        assertEquals("responseAttachment", StringUtils.newStringUtf8(responseAttachment));
    }

    @Test
    public void requestAttachmentTest() {
        RpcClientContext context = new RpcClientContext();
        context.getReqAttachMap().put("key", "attachment".getBytes());
        context.setTimeoutMills(1000000);
        context.setRequestUncodecDataSegment("requestAttachment".getBytes(StandardCharsets.UTF_8));
        HelloRequest request = HelloRequest.newBuilder()
                .setMessage(ByteString.copyFrom("requestAttachment".getBytes())).build();
        GreeterClientApi proxy = clientConfig.getProxy();
        HelloResponse sayHello = proxy.sayHello(context, request);
        assertEquals("requestAttachment", sayHello.getMessage().toStringUtf8());
    }

    @Test
    public void serverFrameExceptionTest() {
        GreeterClientApi proxy = clientConfig.getProxy();
        RpcClientContext context = new RpcClientContext();
        context.setTimeoutMills(1000);
        HelloRequest request =
                HelloRequest.newBuilder().setMessage(ByteString.copyFrom("normal1".getBytes()))
                        .build();
        HelloResponse sayHello = proxy.sayHello(context, request);
        assertEquals(sayHello.getMessage().toStringUtf8(), "normal1");

        HelloRequest request2 = HelloRequest.newBuilder()
                .setMessage(ByteString.copyFrom("sysexception".getBytes())).build();
        Exception realEx = null;
        try {
            proxy.sayHello(context, request2);
        } catch (Exception ex) {
            realEx = ex;
            ex.printStackTrace();
        }
        assertTrue(realEx instanceof TRpcException
                && ((TRpcException) realEx).getCode() == ErrorCode.TRPC_INVOKE_UNKNOWN_ERR);
    }

    @Test
    public void noFunTest() {
        GreeterClientApi proxy = clientConfig.getProxy();
        RpcClientContext context = new RpcClientContext();
        context.getReqAttachMap().put("key", "abc");
        context.setTimeoutMills(100000);
        HelloRequest request =
                HelloRequest.newBuilder().setMessage(ByteString.copyFrom("normal1".getBytes()))
                        .build();
        Exception realEx = null;
        try {
            HelloResponse sayHello = proxy.sayHellox(context, request);
        } catch (Exception ex) {
            realEx = ex;
            ex.printStackTrace();
        }
        assertTrue(realEx instanceof TRpcException
                && ((TRpcException) realEx).getCode() == ErrorCode.TRPC_SERVER_NOFUNC_ERR);
    }

    @Test
    public void serverBizExceptionTest() {
        GreeterClientApi proxy = clientConfig.getProxy();
        RpcClientContext context = new RpcClientContext();
        context.setTimeoutMills(100000);
        HelloRequest request = HelloRequest.newBuilder()
                .setMessage(ByteString.copyFrom("bizexception".getBytes())).build();
        context.setTimeoutMills(1000000);
        Exception realEx = null;
        try {
            proxy.sayHello(context, request);
        } catch (Exception ex) {
            realEx = ex;
            ex.printStackTrace();
        }
        assertEquals(((TRpcException) realEx).getBizCode(), 88);
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> realExRef = new AtomicReference<>();
        proxy.asyncSayHello(context, request).whenComplete((r, t) -> {
            realExRef.set(t);
            latch.countDown();
        });
        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        assertEquals(((TRpcException) (realExRef.get())).getBizCode(), 88);
    }

    @Test
    public void serverTimeoutTest() {
        String host = NetUtils.LOCAL_HOST;
        int port = NetUtils.getAvailablePort();
        ServiceConfig serviceConfig = new ServiceConfig();
        serviceConfig.setIp(host);
        serviceConfig.setPort(port);
        serviceConfig.setEnableLinkTimeout(true);
        ProviderConfig<GreeterService> providerConfig = new ProviderConfig<>();
        providerConfig.setServiceInterface(GreeterService.class);
        providerConfig.setRef(new GreeterServiceImp());
        serviceConfig.addProviderConfig(providerConfig);
        BackendConfig backendConfig = new BackendConfig();
        ConsumerConfig<GreeterClientApi> clientConfig = new ConsumerConfig<>();
        clientConfig.setBackendConfig(backendConfig);
        try {
            serviceConfig.export();
            backendConfig.setNamingUrl("ip://" + host + ":" + port);
            clientConfig.setServiceInterface(GreeterClientApi.class);
            GreeterClientApi proxy = clientConfig.getProxy();
            RpcClientContext context = new RpcClientContext();
            context.setTimeoutMills(1000);

            HelloRequest request = HelloRequest.newBuilder()
                    .setMessage(ByteString.copyFrom("timeout1".getBytes()))
                    .build();

            Exception realEx = null;
            try {
                HelloResponse sayHello = proxy.sayHello(context, request);
            } catch (Exception ex) {
                realEx = ex;
                ex.printStackTrace();
            }
            assertTrue(realEx instanceof TRpcException
                    && ((TRpcException) realEx).getCode() == ErrorCode.TRPC_CLIENT_INVOKE_TIMEOUT_ERR);

            context.setTimeoutMills(3000);
            HelloRequest request2 = HelloRequest.newBuilder().setMessage(ByteString.copyFrom("timeout2".getBytes()))
                    .build();
            HelloResponse sayHello2 = proxy.asyncSayHello(context, request2).toCompletableFuture().join();
            assertEquals(sayHello2.getMessage().toStringUtf8(), "timeout2");

            // consumer config Timeout
            BackendConfig backendConfig1 = new BackendConfig();
            ConsumerConfig<GreeterClientApi> clientConfig1 = new ConsumerConfig<>();
            clientConfig1.setBackendConfig(backendConfig1);
            backendConfig1.setNamingUrl("ip://" + host + ":" + port);
            clientConfig1.setServiceInterface(GreeterClientApi.class);
            clientConfig1.getBackendConfig().setRequestTimeout(1000);
            GreeterClientApi proxy1 = clientConfig1.getProxy();
            RpcClientContext context1 = new RpcClientContext();
            HelloRequest request1 =
                    HelloRequest.newBuilder().setMessage(ByteString.copyFrom("timeout3".getBytes()))
                            .build();

            Exception realEx1 = null;
            try {
                HelloResponse sayHello = proxy1.sayHello(context1, request1);
            } catch (Exception ex) {
                realEx1 = ex;
                ex.printStackTrace();
            } finally {
                backendConfig1.stop();
            }

            assertTrue(realEx1 instanceof TRpcException
                    && ((TRpcException) realEx1).getCode()
                    == ErrorCode.TRPC_CLIENT_INVOKE_TIMEOUT_ERR);

            // consumer config Timeout
            BackendConfig backendConfig2 = new BackendConfig();
            ConsumerConfig<GreeterClientApi> clientConfig2 = new ConsumerConfig<>();
            clientConfig2.setBackendConfig(backendConfig2);
            backendConfig2.setNamingUrl("ip://" + host + ":" + port);
            clientConfig2.setServiceInterface(GreeterClientApi.class);
            clientConfig2.getBackendConfig().setRequestTimeout(3000);
            GreeterClientApi proxy2 = clientConfig2.getProxy();
            RpcClientContext context4 = new RpcClientContext();
            HelloRequest request4 =
                    HelloRequest.newBuilder().setMessage(ByteString.copyFrom("timeout4".getBytes()))
                            .build();
            HelloResponse sayHello4 = proxy2.sayHello(context4, request4);
            assertEquals(sayHello4.getMessage().toStringUtf8(), "timeout4");
        } finally {
            serviceConfig.unExport();
            backendConfig.stop();
        }
    }

    @Test
    public void testCommonMethod() {
        HelloRequest.Builder builder = HelloRequest.newBuilder();
        builder.setMessage(ByteString.copyFromUtf8("hello"));
        HelloResponse response = helloClientConfig.getProxy().sayHello(new RpcClientContext(), builder.build());
        Assert.assertEquals(response.getMessage().toStringUtf8(), "hello response");
    }

    @Test
    public void testDefaultMethod() {
        HelloRequest.Builder builder = HelloRequest.newBuilder();
        builder.setMessage(ByteString.copyFromUtf8("hello"));
        HelloServiceApi serviceApi = helloClientConfig.getProxy();
        HelloResponse response = serviceApi.doDefaultMethod(new RpcClientContext(), builder.build());
        Assert.assertEquals(response.getMessage().toStringUtf8(), "this is default method");
        response = serviceApi.doUnExitedMethod(new RpcClientContext(), builder.build());
        Assert.assertEquals(response.getMessage().toStringUtf8(), "this is default method");
        try {
            clientConfig.getProxy().sayHellox(new RpcClientContext(), builder.build());
            Assert.fail();
        } catch (Exception ex) {
            assertTrue(ex instanceof TRpcException
                    && ((TRpcException) ex).getCode() == ErrorCode.TRPC_SERVER_NOFUNC_ERR);
        }
    }

    @Test
    public void testGenericMethod() throws InvalidProtocolBufferException {
        HelloRequest.Builder request = HelloRequest.newBuilder();
        request.setMessage(ByteString.copyFromUtf8("hello"));
        byte[] reqBytes = request.build().toByteArray();
        byte[] resBytes = helloClientConfig.getProxy().doGenericMethod(new RpcClientContext(), reqBytes);

        HelloResponse response = HelloResponse.parseFrom(resBytes);
        Assert.assertArrayEquals("this is generic method".getBytes(StandardCharsets.UTF_8),
                response.getMessage().toByteArray());
    }

    public static class EmptyChannel implements Channel {

        @Override
        public CompletionStage<Void> send(Object message) throws TransportException {
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletionStage<Void> close() {
            return CompletableFuture.completedFuture(null);
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

    }
}
