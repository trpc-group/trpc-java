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
import java.net.InetSocketAddress;
import javax.ws.rs.HttpMethod;
import org.eclipse.jetty.http.HttpFields;
import org.eclipse.jetty.http.HttpURI;
import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.http.MetaData;
import org.eclipse.jetty.server.HttpChannel;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpOutput;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.junit.Test;

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

        HttpChannel httpChannel = new HttpChannel(connector, new HttpConfiguration(),
                null, null);
        Request request = new Request(httpChannel, null);
        request.setMetaData(
                new MetaData.Request(HttpMethod.GET,
                        new HttpURI(
                                "http://localhost:8080/hello/trpc.test.rpc.Hello/sayHello?verison=1"),
                        HttpVersion.HTTP_1_0, new HttpFields()));
        request.setMethod(HttpMethod.GET);
        request.setPathInfo("/trpc.test.rpc.Hello/sayHello");
        request.setRemoteAddr(new InetSocketAddress("127.0.0.1", 8080));

        Response response = new Response(new HttpChannel(connector, new HttpConfiguration(),
                null, null),
                new HttpOutput(new HttpChannel(connector, new HttpConfiguration(), null,
                        null)));
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
