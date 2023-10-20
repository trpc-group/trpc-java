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
import com.tencent.trpc.proto.standard.common.HelloRequestProtocol;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class UdpConcurrentTest {

    private static final int UDP_PORT = 12321;
    ServiceConfig serviceConfig;

    private static ServiceConfig getServiceConfig() {
        ServiceConfig serviceConfig = new ServiceConfig();
        serviceConfig.setIp("127.0.0.1");
        serviceConfig.setNetwork("udp");
        serviceConfig.setPort(UDP_PORT);
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
        int concurrent = 4;
        CountDownLatch latch = new CountDownLatch(concurrent);
        int cycle = 1000;
        List<TestResult> results = new ArrayList<TestResult>();
        for (int i = 0; i < concurrent; i++) {
            BackendConfig config = new BackendConfig();
            config.setNamingUrl("ip://127.0.0.1:" + UDP_PORT);
            config.setConnsPerAddr(1);
            config.setNetwork("udp");
            final ConcurrentTestServiceApi proxy = config.getProxy(ConcurrentTestServiceApi.class);
            final TestResult r = new TestResult();
            results.add(r);
            final int index = i;
            new Thread() {
                public void run() {
                    try {
                        System.out.println(proxy);
                        for (int i = 0; i < cycle; i++) {
                            RpcClientContext context = new RpcClientContext();
                            RpcContextUtils
                                    .putRequestAttachValue(context, "attach" + index + "-" + i,
                                            "attach" + index + "-v" + i);
                            String message = proxy
                                    .sayHello(context,
                                            HelloRequestProtocol.HelloRequest.newBuilder()
                                                    .setMessage(ByteString
                                                            .copyFromUtf8("req" + index + "-" + i))
                                                    .build())
                                    .getMessage().toStringUtf8();
                            assertEquals(
                                    RpcContextUtils.getRequestAttachValue(context,
                                            "attach" + index + "-" + i),
                                    "attach" + index + "-v" + i);
                            assertEquals(message, ("req" + index + "-" + i));
                        }
                        r.succ = true;
                    } catch (Exception ex) {
                        r.succ = false;
                        r.ex = ex;
                        ex.printStackTrace();
                    } finally {
                        latch.countDown();
                    }
                }
            }.start();
        }
        latch.await(10, TimeUnit.SECONDS);
        for (TestResult each : results) {
            assertTrue(each.succ);
        }
    }

    private void startServer() {
        // 1)服务接口配置
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
