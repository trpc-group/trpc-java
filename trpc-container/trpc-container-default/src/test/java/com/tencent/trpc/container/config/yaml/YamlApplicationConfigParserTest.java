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

package com.tencent.trpc.container.config.yaml;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.tencent.trpc.core.common.ConfigManager;
import com.tencent.trpc.core.common.TRpcSystemProperties;
import com.tencent.trpc.core.common.config.BackendConfig;
import com.tencent.trpc.core.common.config.ClientConfig;
import com.tencent.trpc.core.common.config.GlobalConfig;
import com.tencent.trpc.core.common.config.PluginConfig;
import com.tencent.trpc.core.common.config.ProviderConfig;
import com.tencent.trpc.core.common.config.ServerConfig;
import com.tencent.trpc.core.common.config.ServiceConfig;
import com.tencent.trpc.core.selector.spi.Selector;
import com.tencent.trpc.core.utils.YamlParser;
import java.util.Map;
import org.apache.commons.collections4.MapUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class YamlApplicationConfigParserTest {

    @Before
    public void before() {
        ConfigManager.stopTest();
    }

    @After
    public void after() {
        ConfigManager.stopTest();
    }

    @Test
    public void parse() {
        ConfigManager.stopTest();
        ConfigManager applicationConfig =
                new YamlApplicationConfigParser().parseFromClassPath("trpc_java_parse_test.yaml");
        assert applicationConfig != null;
        applicationConfig.setDefault();
        checkGlobalConfig(applicationConfig);
        checkServerConfig(applicationConfig);
        checkClientConfig(applicationConfig);
        checkPluginConfig(applicationConfig);
    }

    @Test
    public void testParseListenerDefault() {
        ConfigManager.stopTest();
        ConfigManager applicationConfig =
                new YamlApplicationConfigParser().parseFromClassPath("listener_default.yaml");
        assert applicationConfig != null;
        applicationConfig.setDefault();
        checkGlobalConfig(applicationConfig);
        checkServerConfig(applicationConfig);
        checkClientConfig(applicationConfig);
        checkPluginConfig(applicationConfig);
    }


    public void checkGlobalConfig(ConfigManager applicationConfig) {
        GlobalConfig globalConfig = applicationConfig.getGlobalConfig();
        assertEquals(globalConfig.getEnvName(), "test_env_name");
        assertEquals(globalConfig.getContainerName(), "test_container_name");
        assertEquals(globalConfig.getNamespace(), "test_namespace");
        assertTrue(globalConfig.isEnableSet());
        assertEquals(globalConfig.getFullSetName(), "a.b.c");
        assertEquals(globalConfig.getExt().size(), 2);
        assertEquals(globalConfig.getExt().get("ext_a"), "value_a");
        Map<String, Object> extB = (Map<String, Object>) MapUtils.getMap(globalConfig.getExt(), "ext_b");
        assertEquals(extB.get("value_b_key_a"), "value_b_a");
    }

    public void checkPluginConfig(ConfigManager applicationConfig) {
        PluginConfig pluginConfig = applicationConfig.getPluginConfigMap().get(Selector.class).get("polaris");
        assertEquals("/tmp", ((Map<String, Object>) pluginConfig.getProperties().get("localCache")).get("persistDir"));
    }

    /**
     * Test ClientConfig
     *
     * @param applicationConfig configManager instance
     */
    public void checkClientConfig(ConfigManager applicationConfig) {
        ClientConfig clientConfig = applicationConfig.getClientConfig();
        assertEquals(clientConfig.getExtMap().get("connection_request_timeout"), 1000);

        assertEquals(clientConfig.getNamespace(), "dev");
        assertEquals(clientConfig.getFilters().get(0), "filter");
        assertEquals(clientConfig.getRequestTimeout(), 2000);
        assertEquals(clientConfig.getCharset(), "gbk");
        assertEquals(clientConfig.getProtocol(), "trpc");
        assertEquals(clientConfig.getNetwork(), "udp");
        assertEquals(clientConfig.getSerialization(), "pb");
        assertEquals(clientConfig.getCompressor(), "snappy");
        assertEquals(clientConfig.getTransporter(), "httpnetty");
        assertEquals(clientConfig.getIdleTimeout().intValue(), 200);
        assertEquals(clientConfig.getConnsPerAddr(), 5);
        assertEquals(clientConfig.isLazyinit(), false);
        assertEquals(clientConfig.getReceiveBuffer(), 20);
        assertEquals(clientConfig.getSendBuffer(), 10);
        assertEquals(clientConfig.isLazyinit(), false);
        assertEquals(clientConfig.getConnTimeout(), 2000);
        assertEquals(clientConfig.isIoThreadGroupShare(), false);
        assertEquals(clientConfig.getIoThreads(), 20);
        assertEquals(clientConfig.getWorkerPool(), "woker_pool_consumer_test");
        assertEquals(clientConfig.getInterceptors().size(), 1);
        assertEquals(clientConfig.getInterceptors().get(0), "test");
        // 测试默认值
        BackendConfig backendConfig = clientConfig.getBackendConfigMap().get("trpc.TestApp.TestServer.Greeter");
        assertEquals(backendConfig.getNamespace(), "dev2");
        assertEquals(backendConfig.getGroup(), "g1");
        assertEquals(backendConfig.getVersion(), "v1");
        assertEquals(backendConfig.getCallee(), "trpc.TestApp.TestServer.GreeterCallee");
        assertEquals(backendConfig.getFilters().get(0), "filter");
        assertEquals(backendConfig.getRequestTimeout(), 2000);
        assertEquals(backendConfig.getProtocol(), "trpc");
        assertEquals(backendConfig.getNetwork(), "udp");
        assertEquals(backendConfig.getSerialization(), "pb");
        assertEquals(backendConfig.getCompressor(), "gzip");
        assertEquals(1, backendConfig.getCompressMinBytes());
        assertEquals(backendConfig.getTransporter(), "httpnetty");
        assertEquals(backendConfig.getIdleTimeout().intValue(), 200);
        assertEquals(backendConfig.isKeepAlive(), false);
        assertEquals(backendConfig.getConnsPerAddr(), 5);
        assertEquals(backendConfig.getReceiveBuffer(), 20);
        assertEquals(backendConfig.getSendBuffer(), 10);
        assertEquals(backendConfig.isIoThreadGroupShare(), false);
        assertEquals(backendConfig.getIoThreads(), 20);
        assertEquals(backendConfig.getSendBuffer(), 10);
        assertEquals(backendConfig.isLazyinit(), false);
        assertEquals(backendConfig.getConnTimeout(), 2000);
        assertEquals(backendConfig.getWorkerPool(), "woker_pool_consumer_test");
        assertEquals(backendConfig.getInterceptors().size(), 2);
        assertEquals(backendConfig.getInterceptors().get(0), "test");
        assertEquals(backendConfig.getInterceptors().get(1), "test1");
        // 测试默认值覆盖
        BackendConfig backendConfig2 = clientConfig.getBackendConfigMap().get("trpc.TestApp.TestServer.Greeter2");
        assertEquals(backendConfig2.getFilters().get(0), "filter");
        assertEquals(backendConfig2.getFilters().get(1), "filter2");
        assertEquals(backendConfig2.getRequestTimeout(), 3000);
        assertEquals(backendConfig2.getProtocol(), "trpc");
        assertEquals(backendConfig2.getNetwork(), "tcp");
        assertEquals(backendConfig2.getSerialization(), "json");
        assertEquals(backendConfig2.getCompressor(), "zip");
        assertEquals(65535, backendConfig2.getCompressMinBytes());
        assertEquals(backendConfig2.getTransporter(), "netty");
        assertEquals(backendConfig2.getIdleTimeout().intValue(), 300);
        assertEquals(backendConfig2.isKeepAlive(), true);
        assertEquals(backendConfig2.getConnsPerAddr(), 9);
        assertEquals(backendConfig2.getReceiveBuffer(), 40);
        assertEquals(backendConfig2.getCharset(), "utf8");
        assertEquals(backendConfig2.getSendBuffer(), 30);
        assertEquals(backendConfig2.isLazyinit(), false);
        assertEquals(backendConfig2.getConnTimeout(), 3000);
        assertEquals(backendConfig2.getWorkerPool(), "woker_pool_consumer_test2");
        assertFalse(backendConfig2.getMock());
        assertEquals(backendConfig2.getMockClass(), "java.lang.Object");
        assertEquals(backendConfig2.isIoThreadGroupShare(), true);
        assertEquals(backendConfig2.getIoThreads(), 40);
    }

    /**
     * Test ServerConfig
     *
     * @param applicationConfig configManager instance
     */
    public void checkServerConfig(ConfigManager applicationConfig) {
        ServerConfig serverConfig = applicationConfig.getServerConfig();
        assertEquals(serverConfig.getApp(), "QQPIM");
        assertEquals(serverConfig.getServer(), "DMServer");
        assertEquals(serverConfig.getAdminConfig().getAdminIp(), "127.0.0.1");
        assertEquals(serverConfig.getAdminConfig().getAdminPort(), 8091);
        assertEquals(serverConfig.getLocalIp(), "127.0.0.1");
        assertEquals(serverConfig.getNic(), "eth1");
        assertEquals(serverConfig.getCloseTimeout(), 1000);
        assertEquals(serverConfig.getWaitTimeout(), 1200);
        assertEquals(serverConfig.getFilters().get(0), "filter");
        assertEquals(false, serverConfig.getEnableLinkTimeout());
        assertEquals(serverConfig.getRequestTimeout(), 2000);
        assertEquals(serverConfig.getWorkerPool(), "woker_pool_provider_test");

        ServiceConfig serviceConfig = serverConfig.getServiceMap().get("trpc.TestApp.TestServer.Greeter");
        assertTrue(serviceConfig.getReusePort());
        assertEquals(serviceConfig.getVersion(), "v.121");
        assertEquals(serviceConfig.getGroup(), "g1");
        assertEquals(serviceConfig.getName(), "trpc.TestApp.TestServer.Greeter");
        assertEquals(serviceConfig.getIp(), "127.0.0.1");
        assertEquals(serviceConfig.getPort(), 12345);
        assertEquals(serviceConfig.getNic(), "eth3");
        assertEquals(serviceConfig.getProtocol(), "trpc");
        assertEquals(serviceConfig.getNetwork(), "udp");
        assertEquals(serviceConfig.getSerialization(), "pb");
        assertEquals(serviceConfig.getCompressor(), "gzip");
        assertTrue(serviceConfig.getEnableLinkTimeout());
        assertEquals(10, serviceConfig.getCompressMinBytes());
        assertEquals(serviceConfig.getTransporter(), "httpnetty");
        assertEquals(serviceConfig.getCharset(), "gbk");
        assertEquals(serviceConfig.isKeepAlive(), false);
        assertEquals(serviceConfig.getMaxConns(), 10);
        assertEquals(serviceConfig.getBacklog(), 1111);
        assertEquals(serviceConfig.getSendBuffer(), 10);
        assertEquals(serviceConfig.getReceiveBuffer(), 20);
        assertEquals(serviceConfig.getPayload(), 2222);
        assertEquals(serviceConfig.getIdleTimeout().intValue(), 200);
        assertEquals(serviceConfig.isLazyinit(), false);
        assertEquals(serviceConfig.getIoMode(), "kqueue");
        assertEquals(serviceConfig.isIoThreadGroupShare(), false);
        assertEquals(serviceConfig.getIoThreads(), 20);
        assertEquals(serviceConfig.getRequestTimeout(), 3000);
        assertEquals(serviceConfig.getFilters().get(0), "filter");
        assertEquals(serviceConfig.getFilters().get(1), "filter2");
        assertEquals(serviceConfig.getWorkerPool(), "woker_pool_provider_test2");
        assertEquals(serviceConfig.getRegistries().get("polaris").get("token"),
                "xxxx");
        assertEquals(serviceConfig.getRegistries().get("polaris2").get("token"),
                "xxxx");
        ProviderConfig providerConfig = serviceConfig.getProviderConfigs().get(0);
        assertEquals(providerConfig.getRefClazz(), "com.tencent.trpc.container.demo.GreeterServiceImp");
        assertEquals(serviceConfig.getWorkerPool(), "woker_pool_provider_test2");

        ServiceConfig serviceConfig1 = serverConfig.getServiceMap().get("trpc.TestApp.TestServer.Greeter1");
        assertEquals("com.a", serviceConfig1.getProviderConfigs().get(0).getRefClazz());
        assertEquals("com.b", serviceConfig1.getProviderConfigs().get(1).getRefClazz());
        assertEquals("tcp", serviceConfig1.getNetwork());
        assertEquals(serviceConfig1.getWorkerPool(), "woker_pool_provider_test");

        ServiceConfig serviceConfig2 = serverConfig.getServiceMap().get("trpc.TestApp.TestServer.Greeter2");
        assertFalse(serviceConfig2.getReusePort());
        assertEquals("filter", serviceConfig2.getFilters().get(0));
        assertEquals("tcp", serviceConfig2.getNetwork());
        assertEquals(serviceConfig.getProviderConfigs().get(0).getRefClazz(),
                "com.tencent.trpc.container.demo.GreeterServiceImp");
        assertEquals(serviceConfig.getProviderConfigs().get(0).getRefClazz(),
                "com.tencent.trpc.container.demo.GreeterServiceImp");

        ServiceConfig serviceConfig3 = serverConfig.getServiceMap().get("trpc.TestApp.TestServer.Greeter3");
        assertFalse(serviceConfig3.getReusePort());
        assertEquals("filter", serviceConfig3.getFilters().get(0));
        assertEquals("tcp", serviceConfig3.getNetwork());
        assertEquals(serviceConfig3.getProviderConfigs().get(0).getRefClazz(),
                "com.tencent.trpc.container.demo.GreeterServiceImp");
        assertEquals(serviceConfig3.getProviderConfigs().get(0).getRefClazz(),
                "com.tencent.trpc.container.demo.GreeterServiceImp");
    }

    @Test
    public void testServerIpParse() {
        ConfigManager.stopTest();
        ConfigManager applicationConfig = new YamlApplicationConfigParser()
                .parseFromClassPath("trpc_java_ip_parse_test.yaml");
        applicationConfig.setDefault();

        String localIp = applicationConfig.getServerConfig().getLocalIp();
        String ip = applicationConfig.getServerConfig().getServiceMap().get("trpc.TestApp.TestServer.Greeter").getIp();
        assertEquals(localIp, ip);
        assertTrue(applicationConfig.getGlobalConfig().getExt().isEmpty());
    }

    @Test
    public void testParseMap() {
        ConfigManager.stopTest();
        String path = YamlParser.class.getClassLoader().getResource("trpc_java.yaml").getPath();
        Map<String, Object> map = new YamlApplicationConfigParser().parseMap(path);
        Assert.assertNotEquals(map.size(), 0);
    }

    @Test
    public void testParseMap_confPath() {
        ConfigManager.stopTest();
        TRpcSystemProperties.setProperties(TRpcSystemProperties.CONFIG_PATH, "");
        Map<String, Object> map1 = new YamlApplicationConfigParser().parseMap("");
        Assert.assertNotNull(map1);
        String path = YamlParser.class.getClassLoader().getResource("trpc_java.yaml").getPath();
        TRpcSystemProperties.setProperties(TRpcSystemProperties.CONFIG_PATH, path);
        Map<String, Object> map = new YamlApplicationConfigParser().parseMap("");
        Assert.assertNotEquals(map.size(), 0);
    }

    @Test
    public void testParseImpls() {
        ConfigManager.stopTest();
        ConfigManager applicationConfig = new YamlApplicationConfigParser()
                .parseFromClassPath("trpc_java_impls_test.yaml");
        assert applicationConfig != null;
        applicationConfig.setDefault();
        ProviderConfig providerConfig1 = applicationConfig.getServerConfig().getServiceMap()
                .get("trpc.TestApp.TestServer.Greeter").getProviderConfigs().get(0);
        Assert.assertEquals(providerConfig1.getRequestTimeout(), 2000);
        Assert.assertEquals(providerConfig1.getWorkerPool(), "trpc_provider_biz_def");
        Assert.assertEquals(providerConfig1.getFilters().size(), 2);
        Assert.assertEquals(providerConfig1.getFilters().get(0), "a");
        Assert.assertEquals(providerConfig1.getFilters().get(1), "b");
        Assert.assertFalse(providerConfig1.getEnableLinkTimeout());

        ProviderConfig providerConfig2 = applicationConfig.getServerConfig().getServiceMap()
                .get("trpc.TestApp.TestServer.Greeter").getProviderConfigs().get(1);
        Assert.assertEquals(providerConfig2.getRequestTimeout(), Integer.MAX_VALUE);
        Assert.assertEquals(providerConfig2.getWorkerPool(), "trpc_provider_biz_def");
        Assert.assertEquals(providerConfig2.getFilters().size(), 1);
        Assert.assertEquals(providerConfig2.getFilters().get(0), "c");
        Assert.assertTrue(providerConfig2.getEnableLinkTimeout());
    }

    @Test
    public void testEx() {
        ConfigManager.stopTest();
        try {
            new YamlApplicationConfigParser().parseMap("abc");
            TRpcSystemProperties.setProperties(TRpcSystemProperties.CONFIG_PATH, "abc");
            new YamlApplicationConfigParser().parseMap("abc");
            String path = YamlParser.class.getClassLoader().getResource("trpc_java.yaml").getPath();
            TRpcSystemProperties.setProperties(TRpcSystemProperties.CONFIG_PATH, path);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Test
    public void testSet() {
        ServerConfig serverConfig = new ServerConfig();
        serverConfig.setWaitTimeout(2000L);
        assertEquals(2000L, serverConfig.getWaitTimeout());
    }
}
