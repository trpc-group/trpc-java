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

package com.tencent.trpc.registry.zookeeper;


import static com.tencent.trpc.registry.common.ConfigConstants.REGISTRY_CENTER_ADDRESSED_KEY;
import static com.tencent.trpc.registry.common.ConfigConstants.REGISTRY_CENTER_PASSWORD_KEY;
import static com.tencent.trpc.registry.common.ConfigConstants.REGISTRY_CENTER_REGISTER_CONSUMER_KEY;
import static com.tencent.trpc.registry.common.ConfigConstants.REGISTRY_CENTER_USERNAME_KEY;

import com.tencent.trpc.core.common.ConfigManager;
import com.tencent.trpc.core.common.config.PluginConfig;
import com.tencent.trpc.core.common.config.ProviderConfig;
import com.tencent.trpc.core.common.config.ServerConfig;
import com.tencent.trpc.core.common.config.ServiceConfig;
import com.tencent.trpc.core.extension.ExtensionLoader;
import com.tencent.trpc.core.registry.RegisterInfo;
import com.tencent.trpc.core.registry.spi.Registry;
import com.tencent.trpc.core.selector.ServiceId;
import com.tencent.trpc.core.utils.NetUtils;
import com.tencent.trpc.registry.common.RegistryCenterConfig;
import com.tencent.trpc.registry.discovery.RegistryDiscovery;
import com.tencent.trpc.registry.transporter.ZookeeperClient;
import com.tencent.trpc.registry.transporter.curator.CuratorZookeeperFactory;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

public class ZookeeperRegistryTest {

    private static TestingServer zkServer;
    private static ZookeeperClient client;
    private static CuratorZookeeperFactory curatorZookeeperFactory;
    private static ServerConfig serverConfig;
    private static ZookeeperRegistryCenter clientRegistry;
    private static ZookeeperRegistryCenter serverRegistry;
    private static ZookeeperRegistryCenter nullRegistry;

    private static String rootPath = "/trpc";

    private static final String LOCAL_IP = "127.0.0.1";
    private static final int PORT = 2182;
    private static final String LOCAL_IP_PORT = LOCAL_IP + ":" + PORT;
    private static final String SERVICE_NAME1 = "test.server1";
    private static final String SERVICE_NAME2 = "test.server2";

    private static Map<RegisterInfo, RegistryDiscovery> subscribeMap = new HashMap<>();

    private static PluginConfig buildPluginConfig() {
        Map<String, Object> extMap = new HashMap<>();
        extMap.put("ip", LOCAL_IP);
        extMap.put("port", PORT);
        extMap.put(REGISTRY_CENTER_ADDRESSED_KEY, LOCAL_IP_PORT);
        extMap.put(REGISTRY_CENTER_USERNAME_KEY, "zookeeper");
        extMap.put(REGISTRY_CENTER_PASSWORD_KEY, "zk1234");
        extMap.put(REGISTRY_CENTER_REGISTER_CONSUMER_KEY, true);
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
     * Initialize and start the test service
     */
    @Before
    public void setUp() throws Exception {
        zkServer = new TestingServer(2182, new File("/tmp/zk"));
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

        clientRegistry = new ZookeeperRegistryCenter();
        clientRegistry.setPluginConfig(pluginConfig);
        clientRegistry.init();

        nullRegistry = new ZookeeperRegistryCenter();
        nullRegistry.setPluginConfig(pluginConfig);
        nullRegistry.init();
        nullRegistry.setZkClient(null);

        curatorZookeeperFactory = new CuratorZookeeperFactory();
        client = curatorZookeeperFactory.connect(buildConfig());
        delete(rootPath);

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
        config.setRegisterConsumer(true);
        return config;
    }

    @After
    public void tearDown() throws Exception {
        ConfigManager.stopTest();
        if (serverConfig != null) {
            serverConfig.stop();
            serverConfig = null;
        }
        serverRegistry.destroy();
        clientRegistry.destroy();
        if (zkServer != null) {
            zkServer.close();
        }
    }

    private void assertNodeContainsChild(String path, String childPath) {
        List<String> children = client.getChildren(path);
        for (String child : children) {
            if (child.equals(childPath)) {
                return;
            }
        }
        Assert.fail();
    }

    private void assertNodeChildSize(String path, int size) {
        List<String> children = client.getChildren(path);
        Assert.assertEquals(size, children.size());
    }

    @Test
    public void testRegistry() {
        serverConfig.register();
        Assert.assertEquals(2, serverRegistry.getRegisteredRegisterInfos().size());

        assertNodeContainsChild(rootPath, SERVICE_NAME1);
        assertNodeContainsChild(rootPath, SERVICE_NAME2);
        assertNodeContainsChild(rootPath + "/" + SERVICE_NAME1, "providers");
        assertNodeContainsChild(rootPath + "/" + SERVICE_NAME2, "providers");
        assertNodeContainsChild(rootPath + "/" + SERVICE_NAME1 + "/" + "providers",
                RegisterInfo.encode(buildRegisterInfo(LOCAL_IP, 18080, SERVICE_NAME1)));
        assertNodeContainsChild(rootPath + "/" + SERVICE_NAME2 + "/" + "providers",
                RegisterInfo.encode(buildRegisterInfo(LOCAL_IP, 18081, SERVICE_NAME2)));

    }

    @Test
    public void testUnregistry() {
        this.testRegistry();
        serverConfig.unregister();
        Assert.assertEquals(0, serverRegistry.getRegisteredRegisterInfos().size());
        assertNodeChildSize(rootPath + "/" + SERVICE_NAME1 + "/" + "providers", 0);
        assertNodeChildSize(rootPath + "/" + SERVICE_NAME2 + "/" + "providers", 0);
    }


    @Test
    public void testSubscribe() {
        ServiceId serviceId = new ServiceId();
        serviceId.setServiceName(SERVICE_NAME1);
        RegisterInfo registerInfo1 = buildRegisterInfo(LOCAL_IP, 0, SERVICE_NAME1);
        RegistryDiscovery discovery1 = new RegistryDiscovery(serviceId, clientRegistry);
        subscribeMap.put(registerInfo1, discovery1);

        serviceId.setServiceName(SERVICE_NAME2);
        RegisterInfo registerInfo2 = buildRegisterInfo(LOCAL_IP, 0, SERVICE_NAME2);
        RegistryDiscovery discovery2 = new RegistryDiscovery(serviceId, clientRegistry);
        subscribeMap.put(registerInfo2, discovery2);

        Assert.assertEquals(2, clientRegistry.getRegisteredRegisterInfos().size());
        Assert.assertEquals(2, clientRegistry.getSubscribedRegisterInfos().size());

        assertNodeContainsChild(rootPath + "/" + SERVICE_NAME1, "consumers");
        assertNodeContainsChild(rootPath + "/" + SERVICE_NAME2, "consumers");

        Map<String, Object> param = new HashMap<>();
        param.put("type", "consumers");

        assertNodeContainsChild(rootPath + "/" + SERVICE_NAME1 + "/" + "consumers",
                RegisterInfo.encode(buildRegisterInfo(LOCAL_IP, 0, SERVICE_NAME1, param)));

        assertNodeContainsChild(rootPath + "/" + SERVICE_NAME2 + "/" + "consumers",
                RegisterInfo.encode(buildRegisterInfo(LOCAL_IP, 0, SERVICE_NAME2, param)));

        this.testRegistry();

        // 等待订阅的数据更新完成
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        serviceId.setServiceName(SERVICE_NAME1);
        Assert.assertEquals(1, discovery1.getServiceInstances().size());
        Assert.assertEquals(1, discovery1.list(serviceId).size());
        Assert.assertEquals(LOCAL_IP, discovery1.list(serviceId).get(0).getHost());
        Assert.assertEquals(18080, discovery1.list(serviceId).get(0).getPort());

        ServiceId serviceId2 = new ServiceId();
        serviceId2.setServiceName(SERVICE_NAME2);
        Assert.assertEquals(1, discovery2.getServiceInstances().size());
        Assert.assertEquals(1, discovery2.list(serviceId2).size());
        Assert.assertEquals(LOCAL_IP, discovery2.list(serviceId2).get(0).getHost());
        Assert.assertEquals(18081, discovery2.list(serviceId2).get(0).getPort());


    }

    @Test
    public void testUnsubscribe() {
        this.testSubscribe();
        for (Map.Entry<RegisterInfo, RegistryDiscovery> entry : subscribeMap.entrySet()) {
            clientRegistry.unsubscribe(entry.getKey(), entry.getValue());
            ServiceId serviceId = new ServiceId();
            serviceId.setServiceName(entry.getKey().getServiceName());
            Assert.assertEquals(0, entry.getValue().getServiceInstances().size());
            Assert.assertEquals(0, entry.getValue().list(serviceId).size());
        }
        Assert.assertEquals(0, clientRegistry.getSubscribedRegisterInfos().size());
    }

    @Test
    public void testDestroy() {
        this.testUnsubscribe();
        clientRegistry.destroy();
        Assert.assertEquals(0, clientRegistry.getRegisteredRegisterInfos().size());
        Assert.assertEquals(0, clientRegistry.getSubscribedRegisterInfos().size());
        assertNodeChildSize(rootPath + "/" + SERVICE_NAME1 + "/" + "consumers", 0);
        assertNodeChildSize(rootPath + "/" + SERVICE_NAME2 + "/" + "consumers", 0);

        serverRegistry.destroy();
        Assert.assertEquals(0, serverRegistry.getRegisteredRegisterInfos().size());
        assertNodeChildSize(rootPath + "/" + SERVICE_NAME1 + "/" + "providers", 0);
        assertNodeChildSize(rootPath + "/" + SERVICE_NAME2 + "/" + "providers", 0);
    }

    @Test
    public void testNullSubscribe() {
        RegisterInfo registerInfo1 = buildRegisterInfo(LOCAL_IP, 0, SERVICE_NAME1);
        ServiceId serviceId = new ServiceId();
        serviceId.setServiceName(SERVICE_NAME1);
        RegistryDiscovery discovery1 = new RegistryDiscovery(serviceId, nullRegistry);
    }

    @Test
    public void testNullDestroy() {
        nullRegistry.destroy();
    }

}
