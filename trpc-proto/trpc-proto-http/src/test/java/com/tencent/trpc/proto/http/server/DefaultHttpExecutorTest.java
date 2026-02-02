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

package com.tencent.trpc.proto.http.server;

import com.tencent.trpc.core.common.config.ProtocolConfig;
import com.tencent.trpc.core.common.config.ProviderConfig;
import com.tencent.trpc.core.common.config.ServiceConfig;
import com.tencent.trpc.core.rpc.ProviderInvoker;
import com.tencent.trpc.core.rpc.RpcClientContext;
import com.tencent.trpc.core.rpc.anno.TRpcMethod;
import com.tencent.trpc.core.rpc.anno.TRpcService;
import com.tencent.trpc.core.rpc.def.DefProviderInvoker;
import com.tencent.trpc.core.utils.NetUtils;
import com.tencent.trpc.proto.standard.common.TRPCProtocol;
import javax.ws.rs.HttpMethod;
import org.eclipse.jetty.server.HttpChannel;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.junit.jupiter.api.Test;

/**
 * DefaultHttpExecutor test cases.
 */
public class DefaultHttpExecutorTest {

    private static final DefaultHttpExecutor HTTP_EXECUTOR = new DefaultHttpExecutor(
            new ProtocolConfig());
    private static final ProviderInvoker<Hello> INVOKER = buildProviderInvoker();


    private static ProviderInvoker<Hello> buildProviderInvoker() {
        ProviderConfig<Hello> objectProviderConfig = new ProviderConfig<>();
        objectProviderConfig.setServiceInterface(Hello.class);
        objectProviderConfig.setRef(new HelloImpl());
        ServiceConfig serviceConfig = new ServiceConfig();
        serviceConfig.setBasePath("/hello");
        objectProviderConfig.setServiceConfig(serviceConfig);
        ProtocolConfig protocolConfig = new ProtocolConfig();
        return new DefProviderInvoker<>(protocolConfig, objectProviderConfig);
    }

    @Test
    public void testRegist() {
        HTTP_EXECUTOR.register(INVOKER);

        QueuedThreadPool threadPool = new QueuedThreadPool();
        threadPool.setDaemon(true);
        threadPool.setMaxThreads(4);
        threadPool.setMinThreads(4);
        threadPool.setIdleTimeout(60);

        Server server = new Server(threadPool);

        ServerConnector connector = new ServerConnector(server);
        connector.setHost(NetUtils.LOCAL_HOST);
        connector.setPort(8080);
        connector.setAcceptQueueSize(60);
        server.addConnector(connector);

        HttpChannel httpChannel = org.mockito.Mockito.mock(HttpChannel.class);
        Request request = org.mockito.Mockito.mock(Request.class);
        Response response = org.mockito.Mockito.mock(Response.class);

        org.mockito.Mockito.when(request.getMethod()).thenReturn(HttpMethod.GET);
        org.mockito.Mockito.when(request.getPathInfo()).thenReturn("/trpc.test.rpc.Hello/sayHello");
        org.mockito.Mockito.when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        org.mockito.Mockito.when(request.getRemotePort()).thenReturn(8080);
        org.mockito.Mockito.when(request.getAttribute(org.mockito.ArgumentMatchers.anyString())).thenReturn(null);

        HTTP_EXECUTOR.execute(request, response);
    }

    @Test
    public void testUnregist() {
        HTTP_EXECUTOR.unregister(INVOKER.getConfig());
    }

    @Test
    public void testDestroy() {
        HTTP_EXECUTOR.destroy();
    }

    @TRpcService(name = "trpc.test.rpc.Hello")
    public interface Hello {

        @TRpcMethod(name = "sayHello")
        String sayHello(RpcClientContext context, TRPCProtocol.RequestProtocol hello);
    }

    public static class HelloImpl implements Hello {

        @Override
        public String sayHello(RpcClientContext context, TRPCProtocol.RequestProtocol hello) {
            return "hello";
        }
    }
}
