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

package com.tencent.trpc.spring.boot.starters.context;

import com.google.common.collect.Lists;
import com.tencent.trpc.spring.boot.starters.context.configuration.TRpcConfigurationProperties;
import com.tencent.trpc.spring.boot.starters.test.SpringBootTestApplication;
import com.tencent.trpc.spring.context.configuration.schema.YesOrNo;
import com.tencent.trpc.spring.context.configuration.schema.client.ClientServiceSchema;
import com.tencent.trpc.spring.context.configuration.schema.server.IoMode;
import com.tencent.trpc.spring.context.configuration.schema.server.ServerServiceSchema;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = SpringBootTestApplication.class, webEnvironment = WebEnvironment.NONE)
@ActiveProfiles("bind-test2")
public class BindTest2 {

    @Autowired
    private TRpcConfigurationProperties properties;

    @Test
    public void test() {
        assertGlobal();
        assertServer();
        assertServerService();
        assertClient();
        assertWorkerPool();
        assertRegistry();
        assertSelector();
    }

    private void assertGlobal() {
        Assertions.assertEquals(properties.getGlobal().getNamespace(), "test_namespace");
        Assertions.assertEquals(properties.getGlobal().getEnvName(), "test_env_name");
        Assertions.assertEquals(properties.getGlobal().getContainerName(), "test_container_name");
        Assertions.assertEquals(properties.getGlobal().getEnableSet(), YesOrNo.Y);
        Assertions.assertEquals(properties.getGlobal().getFullSetName(), "a.b.c");
    }

    private void assertServer() {
        Assertions.assertEquals(properties.getServer().getApp(), "QQPIM");
        Assertions.assertEquals(properties.getServer().getServer(), "DMServer");
        Assertions.assertEquals(properties.getServer().getAdmin().getAdminIp(), "127.0.0.1");
        Assertions.assertEquals(properties.getServer().getAdmin().getAdminPort(), Integer.valueOf(8091));
        Assertions.assertEquals(properties.getServer().getLocalIp(), "127.0.0.1");
        Assertions.assertEquals(properties.getServer().getCloseTimeout(), Long.valueOf(1000));
        Assertions.assertEquals(properties.getServer().getWaitTimeout(), Long.valueOf(1100));
        Assertions.assertEquals(properties.getServer().getNic(), "eth1");
        Assertions.assertEquals(properties.getServer().getRequestTimeout(), Integer.valueOf(2000));
        Assertions.assertEquals(properties.getServer().getEnableLinkTimeout(), false);
        Assertions.assertEquals(properties.getServer().getWorkerPool(), "woker_pool_provider_test");
        Assertions.assertEquals(properties.getServer().getFilters(), Lists.newArrayList("additional_server_filter"));
    }

    private void assertServerService() {
        ServerServiceSchema serviceSchema = properties.getServer().getService().get(0);
        Assertions.assertEquals(serviceSchema.getName(), "trpc.TestApp.TestServer.Greeter");
        Assertions.assertEquals(serviceSchema.getVersion(), "v.121");
        Assertions.assertEquals(serviceSchema.getGroup(), "g1");
        Assertions.assertEquals(serviceSchema.getImpls().get(0),
                "com.tencent.trpc.spring.boot.starters.test.GreeterServiceImpl");
        Assertions.assertEquals(serviceSchema.getIp(), "127.0.0.1");
        Assertions.assertEquals(serviceSchema.getNic(), "eth3");
        Assertions.assertEquals(serviceSchema.getPort(), Integer.valueOf(12345));
        Assertions.assertEquals(serviceSchema.getNetwork(), "udp");
        Assertions.assertEquals(serviceSchema.getProtocol(), "trpc");
        Assertions.assertEquals(serviceSchema.getSerialization(), "pb");
        Assertions.assertEquals(serviceSchema.getCompressor(), "gzip");
        Assertions.assertEquals(serviceSchema.getCompressMinBytes(), Integer.valueOf(10));
        Assertions.assertEquals(serviceSchema.getCharset(), "gbk");
        Assertions.assertEquals(serviceSchema.getKeepAlive(), false);
        Assertions.assertEquals(serviceSchema.getMaxConns(), Integer.valueOf(10));
        Assertions.assertEquals(serviceSchema.getBacklog(), Integer.valueOf(1111));
        Assertions.assertEquals(serviceSchema.getSendBuffer(), Integer.valueOf(10));
        Assertions.assertEquals(serviceSchema.getReceiveBuffer(), Integer.valueOf(20));
        Assertions.assertEquals(serviceSchema.getPayload(), Integer.valueOf(2222));
        Assertions.assertEquals(serviceSchema.getIdleTimeout(), Integer.valueOf(200));
        Assertions.assertEquals(serviceSchema.getLazyinit(), false);
        Assertions.assertEquals(serviceSchema.getIoMode(), IoMode.kqueue);
        Assertions.assertEquals(serviceSchema.getIoThreadGroupShare(), false);
        Assertions.assertEquals(serviceSchema.getIoThreads(), Integer.valueOf(20));
        Assertions.assertEquals(serviceSchema.getRequestTimeout(), Integer.valueOf(3000));
        Assertions.assertEquals(serviceSchema.getWorkerPool(), "woker_pool_provider_test");
        Assertions.assertEquals(serviceSchema.getEnableLinkTimeout(), true);
        Assertions.assertEquals(serviceSchema.getReusePort(), true);
        Assertions.assertEquals(serviceSchema.getFilters(), Lists.newArrayList("additional_server_filter"));
    }

    private void assertClient() {
        Assertions.assertEquals(properties.getClient().getNamespace(), "dev");
        Assertions.assertEquals(properties.getClient().getWorkerPool(), "woker_pool_consumer_test");
        Assertions.assertEquals(properties.getClient().getRequestTimeout(), Integer.valueOf(2000));
        Assertions.assertEquals(properties.getClient().getNetwork(), "udp");
        Assertions.assertEquals(properties.getClient().getProtocol(), "trpc");
        Assertions.assertEquals(properties.getClient().getSerialization(), "pb");
        Assertions.assertEquals(properties.getClient().getCompressor(), "snappy");
        Assertions.assertEquals(properties.getClient().getCharset(), "gbk");
        Assertions.assertEquals(properties.getClient().getKeepAlive(), false);
        Assertions.assertEquals(properties.getClient().getMaxConns(), Integer.valueOf(10));
        Assertions.assertEquals(properties.getClient().getBacklog(), Integer.valueOf(1111));
        Assertions.assertEquals(properties.getClient().getSendBuffer(), Integer.valueOf(10));
        Assertions.assertEquals(properties.getClient().getReceiveBuffer(), Integer.valueOf(20));
        Assertions.assertEquals(properties.getClient().getIdleTimeout(), Integer.valueOf(200));
        Assertions.assertEquals(properties.getClient().getLazyinit(), false);
        Assertions.assertEquals(properties.getClient().getConnsPerAddr(), Integer.valueOf(5));
        Assertions.assertEquals(properties.getClient().getConnTimeout(), Integer.valueOf(2000));
        Assertions.assertEquals(properties.getClient().getIoThreadGroupShare(), false);
        Assertions.assertEquals(properties.getClient().getIoThreads(), Integer.valueOf(20));
        Assertions.assertEquals(properties.getClient().getFilters(), Lists.newArrayList("additional_client_filter"));
        ClientServiceSchema clientServiceSchema = properties.getClient().getService().get(0);
        Assertions.assertEquals(clientServiceSchema.getName(), "trpc.TestApp.TestServer.Greeter1Naming");
        Assertions.assertEquals(clientServiceSchema.getInterface(),
                "com.tencent.trpc.spring.boot.starters.test.GreeterService");
        Assertions.assertEquals(clientServiceSchema.getNamingUrl(), "ip://127.0.0.1:12345");
        Assertions.assertEquals(clientServiceSchema.getNamespace(), "dev2");
        Assertions.assertEquals(clientServiceSchema.getCallee(), "trpc.TestApp.TestServer.GreeterCallee");
        Assertions.assertEquals(clientServiceSchema.getCallerServiceName(), "trpc.TestApp.TestServer.GreeterCallee2");
        Assertions.assertEquals(clientServiceSchema.getGroup(), "g1");
        Assertions.assertEquals(clientServiceSchema.getVersion(), "v1");
        Assertions.assertEquals(clientServiceSchema.getCompressor(), "gzip");
        Assertions.assertEquals(clientServiceSchema.getCompressMinBytes(), Integer.valueOf(1));
    }

    private void assertWorkerPool() {
        Map<String, Map<String, Object>> workerPool = properties.getPlugins().getWorkerPool();
        Assertions.assertEquals(workerPool.get("woker_pool_provider_test").get("_type"), "thread");
        Assertions.assertEquals(workerPool.get("woker_pool_provider_test").get("core_pool_size"), 10000);
        Assertions.assertEquals(workerPool.get("woker_pool_consumer_test").get("_type"), "thread");
        Assertions.assertEquals(workerPool.get("woker_pool_consumer_test").get("core_pool_size"), 10000);
    }

    private void assertRegistry() {
        Map<String, Map<String, Object>> registry = properties.getPlugins().getRegistry();
        Assertions.assertEquals(registry.get("polaris").get("heartbeat_interval"), 1000);
        Assertions.assertEquals(registry.get("polaris").get("register_self"), false);
    }

    private void assertSelector() {
        Map<String, Map<String, Object>> selector = properties.getPlugins().getSelector();
        Assertions.assertEquals(selector.get("polaris").get("mode"), 0);
    }

    private void assertRemoteLog() {
        Map<String, Map<String, Object>> remoteLog = properties.getPlugins().getRemoteLog();
        Assertions.assertEquals(remoteLog.get("atta").get("attaid"), "0a100055063");
        Assertions.assertEquals(remoteLog.get("atta").get("token"), "3514966829");
        Assertions.assertEquals(remoteLog.get("atta").get("switch"), "open");
    }

    @SuppressWarnings("unchecked")
    private void assertTracing() {
        Map<String, Map<String, Object>> tracing = properties.getPlugins().getTracing();
        Assertions.assertEquals(tracing.get("tjg").get("appid"), "trpc");
        Assertions.assertEquals(tracing.get("tjg").get("service_name"), "trpc.${app}.${server}");
        Assertions.assertEquals(tracing.get("tjg").get("hostname"), "${container_name}");
        Assertions.assertEquals(tracing.get("tjg").get("local_addr"), "${local_ip}");
        Assertions.assertEquals(tracing.get("tjg").get("local_port"), 0);
        Map<String, Object> sampler = (Map<String, Object>) tracing.get("tjg").get("sampler");
        Assertions.assertEquals(sampler.get("type"), "mix");
        Map<String, Object> mix = (Map<String, Object>) sampler.get("mix");
        Assertions.assertEquals(mix.get("sample_rate"), 1024);
        Assertions.assertEquals(mix.get("min_speed_rate"), 1);
        Assertions.assertEquals(mix.get("max_speed_rate"), 10);
    }
}
