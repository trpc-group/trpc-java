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

package com.tencent.trpc.proto.standard.clustertest;

import static org.junit.Assert.assertEquals;

import com.google.common.collect.Lists;
import com.google.protobuf.ByteString;
import com.tencent.trpc.core.common.ConfigManager;
import com.tencent.trpc.core.common.config.BackendConfig;
import com.tencent.trpc.core.common.config.ConsumerConfig;
import com.tencent.trpc.core.common.config.GlobalConfig;
import com.tencent.trpc.core.common.config.ProviderConfig;
import com.tencent.trpc.core.common.config.ServiceConfig;
import com.tencent.trpc.core.filter.FilterManager;
import com.tencent.trpc.core.rpc.CallInfo;
import com.tencent.trpc.core.rpc.RpcClientContext;
import com.tencent.trpc.core.utils.Charsets;
import com.tencent.trpc.proto.standard.common.HelloRequestProtocol;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class ClusterTest {

    private static final int TCP_PORT = 12321;
    private static ServiceConfig serviceConfig;

    private static void startServer() {
        ProviderConfig<GreeterService> providerConfig = new ProviderConfig<>();
        providerConfig.setRef(new GreeterServiceImpl());
        serviceConfig = getServiceConfig();
        serviceConfig.addProviderConfig(providerConfig);
        serviceConfig.setFilters(Lists.newArrayList("serverTest"));
        serviceConfig.export();
        GlobalConfig globalConfig = ConfigManager.getInstance().getGlobalConfig();
        globalConfig.setContainerName("container");
        globalConfig.setEnableSet(true);
        globalConfig.setFullSetName("set");
    }

    private static ServiceConfig getServiceConfig() {
        ServiceConfig serviceConfig = new ServiceConfig();
        serviceConfig.setIp("127.0.0.1");
        serviceConfig.setNetwork("tcp");
        serviceConfig.setPort(TCP_PORT);
        serviceConfig.setEnableLinkTimeout(true);
        return serviceConfig;
    }

    @Before
    public void before() {
        FilterManager.registerPlugin("serverTest", ServerFilterTest.class);
        FilterManager.registerPlugin("clientTest", ClientFilterTest.class);
        // 設置serverConfig
        ConfigManager.getInstance().start();
        startServer();
    }

    @After
    public void stop() {
        ConfigManager.getInstance().stop();
        if (serviceConfig != null) {
            serviceConfig.unExport();
        }
    }

    @Test
    public void testAttachmentAndCallInfo() {
        ConfigManager.getInstance().start();
        BackendConfig backendConfig = new BackendConfig();
        backendConfig.setFilters(Lists.newArrayList("clientTest"));
        backendConfig.setNamingUrl("ip://127.0.0.1:" + TCP_PORT);
        ConsumerConfig<GreeterServiceApi> consumerConfig = new ConsumerConfig<>();
        consumerConfig.setBackendConfig(backendConfig);
        consumerConfig.setServiceInterface(GreeterServiceApi.class);
        CallInfo callInfo = new CallInfo();
        callInfo.setCalleeApp("calleeapp");
        callInfo.setCalleeServer("calleeserver");

        callInfo.setCalleeMethod("calleemethod");
        callInfo.setCalleeService("calleeservice");
        callInfo.setCallerApp("callerapp");
        callInfo.setCallerServer("callerserver");

        callInfo.setCallerMethod("callermethod");
        callInfo.setCallerService("callerservice");
        callInfo.setCallerSetName("set");
        callInfo.setCallerContainerName("container");
        RpcClientContext context = new RpcClientContext();
        context.setCallInfo(callInfo);
        context.setTimeoutMills(100000);
        context.setDyeingKey("reqDyeingKey");
        context.getReqAttachMap().put("attachKey", "reqAttachValue".getBytes(Charsets.UTF_8));
        GreeterServiceApi proxy = consumerConfig.getProxy();
        String message = proxy.sayHello(context,
                        HelloRequestProtocol.HelloRequest.newBuilder()
                                .setMessage(ByteString.copyFromUtf8("haha")).build())
                .getMessage().toStringUtf8();
        assertEquals("haha", message);
        Assert.assertEquals("container", context.getCallInfo().getCallerContainerName());
        Assert.assertEquals("set", context.getCallInfo().getCallerSetName());
        Assert.assertEquals("reqAttachValue",
                new String((byte[]) (context.getRspAttachMap().get("attachKey")), Charsets.UTF_8));
        System.out.println(">>>[client]receive msg:" + message);
    }
}
