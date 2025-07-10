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

package com.tencent.trpc.spring.boot.starters.context;

import com.google.common.collect.Lists;
import com.tencent.trpc.spring.boot.starters.context.configuration.TRpcConfigurationProperties;
import com.tencent.trpc.spring.boot.starters.test.SpringBootTestApplication;
import com.tencent.trpc.spring.context.configuration.schema.YesOrNo;
import com.tencent.trpc.spring.context.configuration.schema.client.ClientServiceSchema;
import com.tencent.trpc.spring.context.configuration.schema.server.IoMode;
import com.tencent.trpc.spring.context.configuration.schema.server.ServerServiceSchema;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
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
        Assert.assertEquals(properties.getGlobal().getNamespace(), "test_namespace");
        Assert.assertEquals(properties.getGlobal().getEnvName(), "test_env_name");
        Assert.assertEquals(properties.getGlobal().getContainerName(), "test_container_name");
        Assert.assertEquals(properties.getGlobal().getEnableSet(), YesOrNo.Y);
        Assert.assertEquals(properties.getGlobal().getFullSetName(), "a.b.c");
    }

    private void assertServer() {
        Assert.assertEquals(properties.getServer().getApp(), "QQPIM");
        Assert.assertEquals(properties.getServer().getServer(), "DMServer");
        Assert.assertEquals(properties.getServer().getAdmin().getAdminIp(), "127.0.0.1");
        Assert.assertEquals(properties.getServer().getAdmin().getAdminPort(), Integer.valueOf(8091));
        Assert.assertEquals(properties.getServer().getLocalIp(), "127.0.0.1");
        Assert.assertEquals(properties.getServer().getCloseTimeout(), Long.valueOf(1000));
        Assert.assertEquals(properties.getServer().getWaitTimeout(), Long.valueOf(1100));
        Assert.assertEquals(properties.getServer().getNic(), "eth1");
        Assert.assertEquals(properties.getServer().getRequestTimeout(), Integer.valueOf(2000));
        Assert.assertEquals(properties.getServer().getEnableLinkTimeout(), false);
        Assert.assertEquals(properties.getServer().getWorkerPool(), "woker_pool_provider_test");
        Assert.assertEquals(properties.getServer().getFilters(), Lists.newArrayList("additional_server_filter"));
    }

    private void assertServerService() {
        ServerServiceSchema serviceSchema = properties.getServer().getService().get(0);
        Assert.assertEquals(serviceSchema.getName(), "trpc.TestApp.TestServer.Greeter");
        Assert.assertEquals(serviceSchema.getVersion(), "v.121");
        Assert.assertEquals(serviceSchema.getGroup(), "g1");
        Assert.assertEquals(serviceSchema.getImpls().get(0),
                "com.tencent.trpc.spring.boot.starters.test.GreeterServiceImpl");
        Assert.assertEquals(serviceSchema.getIp(), "127.0.0.1");
        Assert.assertEquals(serviceSchema.getNic(), "eth3");
        Assert.assertEquals(serviceSchema.getPort(), Integer.valueOf(12345));
        Assert.assertEquals(serviceSchema.getNetwork(), "udp");
        Assert.assertEquals(serviceSchema.getProtocol(), "trpc");
        Assert.assertEquals(serviceSchema.getSerialization(), "pb");
        Assert.assertEquals(serviceSchema.getCompressor(), "gzip");
        Assert.assertEquals(serviceSchema.getCompressMinBytes(), Integer.valueOf(10));
        Assert.assertEquals(serviceSchema.getCharset(), "gbk");
        Assert.assertEquals(serviceSchema.getKeepAlive(), false);
        Assert.assertEquals(serviceSchema.getMaxConns(), Integer.valueOf(10));
        Assert.assertEquals(serviceSchema.getBacklog(), Integer.valueOf(1111));
        Assert.assertEquals(serviceSchema.getSendBuffer(), Integer.valueOf(10));
        Assert.assertEquals(serviceSchema.getReceiveBuffer(), Integer.valueOf(20));
        Assert.assertEquals(serviceSchema.getPayload(), Integer.valueOf(2222));
        Assert.assertEquals(serviceSchema.getIdleTimeout(), Integer.valueOf(200));
        Assert.assertEquals(serviceSchema.getLazyinit(), false);
        Assert.assertEquals(serviceSchema.getIoMode(), IoMode.kqueue);
        Assert.assertEquals(serviceSchema.getIoThreadGroupShare(), false);
        Assert.assertEquals(serviceSchema.getIoThreads(), Integer.valueOf(20));
        Assert.assertEquals(serviceSchema.getRequestTimeout(), Integer.valueOf(3000));
        Assert.assertEquals(serviceSchema.getWorkerPool(), "woker_pool_provider_test");
        Assert.assertEquals(serviceSchema.getEnableLinkTimeout(), true);
        Assert.assertEquals(serviceSchema.getReusePort(), true);
        Assert.assertEquals(serviceSchema.getFilters(), Lists.newArrayList("additional_server_filter"));
    }

    private void assertClient() {
        Assert.assertEquals(properties.getClient().getNamespace(), "dev");
        Assert.assertEquals(properties.getClient().getWorkerPool(), "woker_pool_consumer_test");
        Assert.assertEquals(properties.getClient().getRequestTimeout(), Integer.valueOf(2000));
        Assert.assertEquals(properties.getClient().getNetwork(), "udp");
        Assert.assertEquals(properties.getClient().getProtocol(), "trpc");
        Assert.assertEquals(properties.getClient().getSerialization(), "pb");
        Assert.assertEquals(properties.getClient().getCompressor(), "snappy");
        Assert.assertEquals(properties.getClient().getCharset(), "gbk");
        Assert.assertEquals(properties.getClient().getKeepAlive(), false);
        Assert.assertEquals(properties.getClient().getMaxConns(), Integer.valueOf(10));
        Assert.assertEquals(properties.getClient().getBacklog(), Integer.valueOf(1111));
        Assert.assertEquals(properties.getClient().getSendBuffer(), Integer.valueOf(10));
        Assert.assertEquals(properties.getClient().getReceiveBuffer(), Integer.valueOf(20));
        Assert.assertEquals(properties.getClient().getIdleTimeout(), Integer.valueOf(200));
        Assert.assertEquals(properties.getClient().getLazyinit(), false);
        Assert.assertEquals(properties.getClient().getConnsPerAddr(), Integer.valueOf(5));
        Assert.assertEquals(properties.getClient().getConnTimeout(), Integer.valueOf(2000));
        Assert.assertEquals(properties.getClient().getIoThreadGroupShare(), false);
        Assert.assertEquals(properties.getClient().getIoThreads(), Integer.valueOf(20));
        Assert.assertEquals(properties.getClient().getFilters(), Lists.newArrayList("additional_client_filter"));
        ClientServiceSchema clientServiceSchema = properties.getClient().getService().get(0);
        Assert.assertEquals(clientServiceSchema.getName(), "trpc.TestApp.TestServer.Greeter1Naming");
        Assert.assertEquals(clientServiceSchema.getInterface(),
                "com.tencent.trpc.spring.boot.starters.test.GreeterService");
        Assert.assertEquals(clientServiceSchema.getNamingUrl(), "ip://127.0.0.1:12345");
        Assert.assertEquals(clientServiceSchema.getNamespace(), "dev2");
        Assert.assertEquals(clientServiceSchema.getCallee(), "trpc.TestApp.TestServer.GreeterCallee");
        Assert.assertEquals(clientServiceSchema.getCallerServiceName(), "trpc.TestApp.TestServer.GreeterCallee2");
        Assert.assertEquals(clientServiceSchema.getGroup(), "g1");
        Assert.assertEquals(clientServiceSchema.getVersion(), "v1");
        Assert.assertEquals(clientServiceSchema.getCompressor(), "gzip");
        Assert.assertEquals(clientServiceSchema.getCompressMinBytes(), Integer.valueOf(1));
    }

    private void assertWorkerPool() {
        Map<String, Map<String, Object>> workerPool = properties.getPlugins().getWorkerPool();
        Assert.assertEquals(workerPool.get("woker_pool_provider_test").get("_type"), "thread");
        Assert.assertEquals(workerPool.get("woker_pool_provider_test").get("core_pool_size"), 10000);
        Assert.assertEquals(workerPool.get("woker_pool_consumer_test").get("_type"), "thread");
        Assert.assertEquals(workerPool.get("woker_pool_consumer_test").get("core_pool_size"), 10000);
    }

    private void assertRegistry() {
        Map<String, Map<String, Object>> registry = properties.getPlugins().getRegistry();
        Assert.assertEquals(registry.get("polaris").get("heartbeat_interval"), 1000);
        Assert.assertEquals(registry.get("polaris").get("register_self"), false);
    }

    private void assertSelector() {
        Map<String, Map<String, Object>> selector = properties.getPlugins().getSelector();
        Assert.assertEquals(selector.get("polaris").get("mode"), 0);
    }

    private void assertRemoteLog() {
        Map<String, Map<String, Object>> remoteLog = properties.getPlugins().getRemoteLog();
        Assert.assertEquals(remoteLog.get("atta").get("attaid"), "0a100055063");
        Assert.assertEquals(remoteLog.get("atta").get("token"), "3514966829");
        Assert.assertEquals(remoteLog.get("atta").get("switch"), "open");
    }

    @SuppressWarnings("unchecked")
    private void assertTracing() {
        Map<String, Map<String, Object>> tracing = properties.getPlugins().getTracing();
        Assert.assertEquals(tracing.get("tjg").get("appid"), "trpc");
        Assert.assertEquals(tracing.get("tjg").get("service_name"), "trpc.${app}.${server}");
        Assert.assertEquals(tracing.get("tjg").get("hostname"), "${container_name}");
        Assert.assertEquals(tracing.get("tjg").get("local_addr"), "${local_ip}");
        Assert.assertEquals(tracing.get("tjg").get("local_port"), 0);
        Map<String, Object> sampler = (Map<String, Object>) tracing.get("tjg").get("sampler");
        Assert.assertEquals(sampler.get("type"), "mix");
        Map<String, Object> mix = (Map<String, Object>) sampler.get("mix");
        Assert.assertEquals(mix.get("sample_rate"), 1024);
        Assert.assertEquals(mix.get("min_speed_rate"), 1);
        Assert.assertEquals(mix.get("max_speed_rate"), 10);
    }
}
