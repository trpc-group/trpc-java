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

package com.tencent.trpc.selector.zookeeper;


import com.tencent.trpc.core.common.ConfigManager;
import com.tencent.trpc.core.common.config.PluginConfig;
import com.tencent.trpc.core.common.config.ProviderConfig;
import com.tencent.trpc.core.common.config.ServerConfig;
import com.tencent.trpc.core.common.config.ServiceConfig;
import com.tencent.trpc.core.extension.ExtensionLoader;
import com.tencent.trpc.core.registry.RegisterInfo;
import com.tencent.trpc.core.registry.spi.Registry;
import com.tencent.trpc.core.selector.ServiceId;
import com.tencent.trpc.core.selector.ServiceInstance;
import com.tencent.trpc.core.selector.spi.Selector;
import com.tencent.trpc.core.utils.NetUtils;
import com.tencent.trpc.registry.common.RegistryCenterConfig;
import com.tencent.trpc.registry.transporter.ZookeeperClient;
import com.tencent.trpc.registry.transporter.curator.CuratorZookeeperFactory;
import com.tencent.trpc.registry.zookeeper.ZookeeperRegistryCenter;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.curator.test.TestingServer;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import tests.service.GreeterJsonService;
import tests.service.GreeterService;
import tests.service.impl1.GreeterJsonServiceImpl1;
import tests.service.impl1.GreeterServiceImpl1;

public class ZookeeperSelectorTest {

    private static TestingServer zkServer;
    private static ZookeeperClient client;
    private static CuratorZookeeperFactory curatorZookeeperFactory;
    private static ServerConfig serverConfig;
    private static ZookeeperRegistryCenter serverRegistry;

    private static String rootPath = "/trpc";
    private static final String LOCAL_IP = "127.0.0.1";
    private static final int PORT = 2182;
    private static final String LOCAL_IP_PORT = LOCAL_IP + ":" + PORT;
    private static final String SERVICE_NAME1 = "test.server1";
    private static final String SERVICE_NAME2 = "test.server2";

    private Selector selector;

    private static PluginConfig buildPluginConfig() {
        Map<String, Object> extMap = new HashMap<>();
        extMap.put("ip", LOCAL_IP);
        extMap.put("port", PORT);
        extMap.put("addresses", LOCAL_IP_PORT);
        extMap.put("username", "zookeeper");
        extMap.put("password", "zk1234");
        return new PluginConfig("zookeeper", Registry.class,
                ZookeeperRegistryCenter.class, extMap);
    }

    private static ServiceConfig getServiceConfig(ProviderConfig gspc, String name,
            String ip, int port, String protocol, String transport) {
        ServiceConfig serviceConfig = new ServiceConfig();
        serviceConfig.setName(name);
        serviceConfig.getProviderConfigs().add(gspc);
        serviceConfig.setIp(ip);
        serviceConfig.setPort(port);
        serviceConfig.setProtocol(protocol);
        serviceConfig.setTransporter(transport);
        return serviceConfig;
    }

    /**
     * Initialize and create a test service to start
     */
    @Before
    public void setUp() throws Exception {
        zkServer = new TestingServer(PORT, new File("/tmp/zk"));
        zkServer.start();
        ConfigManager.stopTest();

        Map<String, PluginConfig> configMap = new HashMap<>();
        PluginConfig pluginConfig = buildPluginConfig();
        configMap.put("zookeeper", pluginConfig);

        ConfigManager.getInstance().getPluginConfigMap().put(Registry.class, configMap);
        ConfigManager.startTest();

        ProviderConfig<GreeterService> gspc = new ProviderConfig<>();
        gspc.setServiceInterface(GreeterService.class);
        gspc.setRef(new GreeterServiceImpl1());

        ProviderConfig<GreeterJsonService> gjspc = new ProviderConfig<>();
        gjspc.setServiceInterface(GreeterJsonService.class);
        gjspc.setRef(new GreeterJsonServiceImpl1());

        HashMap<String, ServiceConfig> providers = new HashMap<>();

        ServiceConfig serviceConfig1 = getServiceConfig(gspc, SERVICE_NAME1,
                NetUtils.LOCAL_HOST, 18080, "trpc", "netty");
        serviceConfig1.getRegistries().put("zookeeper", new HashMap<>());

        providers.put(serviceConfig1.getName(), serviceConfig1);

        ServiceConfig serviceConfig2 = getServiceConfig(gspc, SERVICE_NAME2,
                NetUtils.LOCAL_HOST, 18081, "trpc", "netty");
        serviceConfig2.getRegistries().put("zookeeper", new HashMap<>());

        providers.put(serviceConfig2.getName(), serviceConfig2);

        ServerConfig sc = new ServerConfig();
        sc.setServiceMap(providers);
        sc.setApp("http-test-app");
        sc.setLocalIp(LOCAL_IP);
        sc.init();

        serverConfig = sc;
        serverRegistry = (ZookeeperRegistryCenter) ExtensionLoader.getExtensionLoader(Registry.class)
                .getExtension("zookeeper");

        curatorZookeeperFactory = new CuratorZookeeperFactory();
        client = curatorZookeeperFactory.connect(buildConfig());
        delete(rootPath);

        selector = ExtensionLoader.getExtensionLoader(Selector.class).getExtension("zookeeper");

        serverConfig.register();
    }

    private void delete(String path) {
        List<String> children = client.getChildren(path);
        if (children != null) {
            for (String child : children) {
                delete(path + "/" + child);
            }
        }
        children = client.getChildren(path);
        Assert.assertTrue(CollectionUtils.isEmpty(children));
    }

    private RegisterInfo buildRegisterInfo(String ip, int port, String serviceName) {
        return new RegisterInfo("trpc", ip, port, serviceName);
    }

    private RegisterInfo buildRegisterInfo(String ip, int port, String serviceName,
            Map<String, Object> param) {
        return new RegisterInfo("trpc", ip, port, serviceName, param);
    }

    private RegistryCenterConfig buildConfig() {
        RegistryCenterConfig config = new RegistryCenterConfig();
        config.setAddresses(LOCAL_IP_PORT);
        config.setUsername("zookeeper");
        config.setPassword("zk1234");
        return config;
    }

    @After
    public void tearDown() throws Exception {
        serverConfig.unregister();
        ConfigManager.stopTest();
        if (serverConfig != null) {
            serverConfig.stop();
            serverConfig = null;
        }
        serverRegistry.destroy();
        if (zkServer != null) {
            zkServer.close();
        }
    }

    @Test
    public void testAsyncSelectOne() throws ExecutionException, InterruptedException {
        ServiceId serviceId = new ServiceId();
        serviceId.setServiceName(SERVICE_NAME1);

        CompletionStage<ServiceInstance> future = selector.asyncSelectOne(serviceId, null);
        ServiceInstance serviceInstance = future.toCompletableFuture().get();
        Assert.assertNotNull(serviceInstance);
        Assert.assertEquals(LOCAL_IP, serviceInstance.getHost());
        Assert.assertEquals(18080, serviceInstance.getPort());

        serviceId.setServiceName(SERVICE_NAME2);

        future = selector.asyncSelectOne(serviceId, null);
        serviceInstance = future.toCompletableFuture().get();
        Assert.assertNotNull(serviceInstance);
        Assert.assertEquals(LOCAL_IP, serviceInstance.getHost());
        Assert.assertEquals(18081, serviceInstance.getPort());
    }

    @Test
    public void testAsyncSelectAll() throws ExecutionException, InterruptedException {
        ServiceId serviceId = new ServiceId();
        serviceId.setServiceName(SERVICE_NAME1);

        CompletionStage<List<ServiceInstance>> future = selector.asyncSelectAll(serviceId, null);
        List<ServiceInstance> serviceInstances = future.toCompletableFuture().get();
        Assert.assertNotNull(serviceInstances);
        Assert.assertEquals(1, serviceInstances.size());
        Assert.assertEquals(LOCAL_IP, serviceInstances.get(0).getHost());
        Assert.assertEquals(18080, serviceInstances.get(0).getPort());

        serviceId.setServiceName(SERVICE_NAME2);

        future = selector.asyncSelectAll(serviceId, null);
        serviceInstances = future.toCompletableFuture().get();
        Assert.assertNotNull(serviceInstances);
        Assert.assertEquals(1, serviceInstances.size());
        Assert.assertEquals(LOCAL_IP, serviceInstances.get(0).getHost());
        Assert.assertEquals(18081, serviceInstances.get(0).getPort());
    }

    @Test
    public void testReport() {
        selector.report(null, 0, 0);
    }

}
