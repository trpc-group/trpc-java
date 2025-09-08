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

package com.tencent.trpc.proto.standard.concurrenttest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.google.protobuf.ByteString;
import com.tencent.trpc.core.common.ConfigManager;
import com.tencent.trpc.core.common.config.BackendConfig;
import com.tencent.trpc.core.common.config.ProviderConfig;
import com.tencent.trpc.core.common.config.ServiceConfig;
import com.tencent.trpc.core.rpc.RpcClientContext;
import com.tencent.trpc.core.utils.RpcContextUtils;
import com.tencent.trpc.proto.standard.common.HelloRequestProtocol.HelloRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TcpConcurrentTest {

    private static final int TCP_PORT = 12421;
    ProviderConfig<ConcurrentTestService> providerConfig;
    ServiceConfig serviceConfig;

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
        ConfigManager.stopTest();
        ConfigManager.startTest();
        startServer();
    }

    @After
    public void stop() {
        ConfigManager.stopTest();
        if (serviceConfig != null) {
            serviceConfig.unExport();
        }
    }

    @Test
    public void testUdpConcurrentTest() throws InterruptedException {
        int concurrent = 10;
        CountDownLatch latch = new CountDownLatch(concurrent);
        int cycle = 1000;
        List<TestResult> results = new ArrayList<>();
        for (int i = 0; i < concurrent; i++) {
            BackendConfig config = new BackendConfig();
            config.setNamingUrl("ip://127.0.0.1:" + TCP_PORT);
            config.setConnsPerAddr(1);
            config.setNetwork("tcp");
            final ConcurrentTestServiceApi proxy = config.getProxy(ConcurrentTestServiceApi.class);
            final TestResult r = new TestResult();
            results.add(r);
            final int index = i;
            new Thread(() -> {
                try {
                    for (int i1 = 0; i1 < cycle; i1++) {
                        RpcClientContext context = new RpcClientContext();
                        RpcContextUtils.putRequestAttachValue(context, "attach" + index + "-" + i1,
                                "attach" + index + "-v" + i1);
                        String message = proxy.sayHello(context, HelloRequest.newBuilder()
                                        .setMessage(ByteString.copyFromUtf8("req" + index + "-" + i1))
                                        .build())
                                .getMessage().toStringUtf8();
                        assertEquals(RpcContextUtils.getRequestAttachValue(context, "attach" + index + "-" + i1),
                                "attach" + index + "-v" + i1);
                        assertEquals(message, ("req" + index + "-" + i1));
                    }
                    r.succ = true;
                    latch.countDown();
                } catch (Exception ex) {
                    r.succ = false;
                    r.ex = ex;
                    ex.printStackTrace();
                    latch.countDown();
                }
            }).start();
        }
        latch.await(200, TimeUnit.SECONDS);
        for (TestResult each : results) {
            assertTrue(each.succ);
        }
    }

    private void startServer() {
        ProviderConfig<ConcurrentTestService> providerConfig = new ProviderConfig<>();
        providerConfig.setRef(new ConcurrentTestServiceImpl());
        serviceConfig = getServiceConfig();
        serviceConfig.setRequestTimeout(10000000);
        serviceConfig.addProviderConfig(providerConfig);
        serviceConfig.export();
    }

    private static class TestResult {

        public boolean succ;
        public Exception ex;
    }
}
