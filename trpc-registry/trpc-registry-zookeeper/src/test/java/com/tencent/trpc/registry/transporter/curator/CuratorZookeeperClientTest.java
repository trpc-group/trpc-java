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

package com.tencent.trpc.registry.transporter.curator;


import com.tencent.trpc.core.logger.Logger;
import com.tencent.trpc.core.logger.LoggerFactory;
import com.tencent.trpc.registry.common.RegistryCenterConfig;
import com.tencent.trpc.registry.transporter.ChildListener;
import com.tencent.trpc.registry.transporter.DataListener;
import com.tencent.trpc.registry.transporter.ZookeeperClient;
import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.test.TestingServer;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.powermock.reflect.Whitebox;

public class CuratorZookeeperClientTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(CuratorZookeeperClientTest.class);

    private static ZookeeperClient client;
    private static CuratorZookeeperFactory curatorZookeeperFactory;
    private static TestingServer zkServer;


    private static final String LOCAL_IP = "127.0.0.1";
    private static final int PORT = 2183;
    private static final String LOCAL_IP_PORT = LOCAL_IP + ":" + PORT;
    private static String testRootName = "trpc";
    private static String testRootPath = String.format("/%s", testRootName);
    private static String testServiceName = "test.server1";
    private static String testServicePath = String.format("%s/%s", testRootPath, testServiceName);
    private static String testNodeName = "providers";
    private static String testNodeFullPath = String.format("%s/%s", testServicePath, testNodeName);

    @Before
    public void setUp() throws Exception {
        zkServer = new TestingServer(PORT, new File("/tmp/zk/curator"));
        zkServer.start();
        curatorZookeeperFactory = new CuratorZookeeperFactory();
        client = curatorZookeeperFactory.connect(buildConfig());
    }

    @After
    public void tearDown() throws Exception {
        if (client.isConnected()) {
            List<String> children = client.getChildren(testNodeFullPath);
            if (children != null) {
                for (String child : children) {
                    client.delete(testNodeFullPath + "/" + child);
                }
            }
            client.delete(testNodeFullPath);
            client.delete(testServicePath);
            client.delete(testRootPath);
            client.close();
        }
        if (zkServer != null) {
            zkServer.close();
        }
    }

    private RegistryCenterConfig buildConfig() {
        RegistryCenterConfig config = new RegistryCenterConfig();
        config.setAddresses(LOCAL_IP_PORT);
        config.setUsername("zookeeper");
        config.setPassword("zk1234");
        config.setRegisterConsumer(true);
        return config;
    }

    @Test
    public void testIsConnected() {
        client.getChildren(testRootPath);
        Assert.assertTrue(client.isConnected());
    }

    @Test
    public void testOperate() {
        List<String> children = client.getChildren(testRootPath);
        Assert.assertNull(children);

        client.create(testNodeFullPath, true);
        client.create(testNodeFullPath, true);

        children = client.getChildren("/");
        Assert.assertNotNull(children);
        Assert.assertTrue(children.size() > 0);

        children = client.getChildren(testRootPath);
        Assert.assertNotNull(children);
        Assert.assertEquals(1, children.size());
        Assert.assertEquals(testServiceName, children.get(0));

        children = client.getChildren(testServicePath);
        Assert.assertNotNull(children);
        Assert.assertEquals(1, children.size());
        Assert.assertEquals(testNodeName, children.get(0));

        children = client.getChildren(testNodeFullPath);
        Assert.assertNotNull(children);
        Assert.assertEquals(0, children.size());

        this.testDelete();
    }

    @Test
    public void testDelete() {
        List<String> children = client.getChildren(testNodeFullPath);
        if (children != null) {
            for (String child : children) {
                client.delete(testNodeFullPath + "/" + child);
            }
        }
        client.delete(testNodeFullPath);
        client.delete(testServicePath);
        client.delete(testRootPath);
        client.delete(testRootPath);

        children = client.getChildren(testRootPath);
        Assert.assertNull(children);
    }

    @Test
    public void testChildListener() throws InterruptedException {
        this.testDelete();
        List<String> children = client.getChildren(testRootPath);
        Assert.assertNull(children);

        final Map<String, List> assertCache = new HashMap<>();
        ChildListener childListener = new ChildListener() {
            @Override
            public void childChanged(String path, List<String> children) {
                assertCache.put(path, children);
            }
        };

        // 测试添加失败
        client.addChildListener(testNodeFullPath, childListener);

        client.create(testNodeFullPath, false);

        children = client.getChildren(testServicePath);
        Assert.assertNotNull(children);
        Assert.assertEquals(1, children.size());
        Assert.assertEquals(testNodeName, children.get(0));

        // 正常添加
        client.addChildListener(testNodeFullPath, childListener);
        String providerPath = testNodeFullPath + "/provider1";

        client.create(providerPath, true);

        children = client.getChildren(testNodeFullPath);
        Assert.assertNotNull(children);
        Assert.assertEquals(1, children.size());
        Thread.sleep(1000);
        Assert.assertTrue(assertCache.containsKey(testNodeFullPath));
        Assert.assertEquals(1, assertCache.get(testNodeFullPath).size());
        Assert.assertEquals("provider1", assertCache.get(testNodeFullPath).get(0));

        client.removeChildListener(testNodeFullPath, childListener);

    }

    @Test
    public void testDataListener() throws InterruptedException {
        this.testDelete();
        List<String> children = client.getChildren(testRootPath);
        Assert.assertNull(children);
        client.create(testNodeFullPath, false);

        children = client.getChildren(testServicePath);
        Assert.assertNotNull(children);
        Assert.assertEquals(1, children.size());
        Assert.assertEquals(testNodeName, children.get(0));

        final Map<String, List<ChildData>> assertCache = new HashMap<>();

        final String providerPath = testNodeFullPath + "/provider1";
        DataListener dataListener = (type, oldData, data) -> {
            List<ChildData> list = Arrays.asList(oldData, data);
            assertCache.put(providerPath, list);
        };

        client.addDataListener(providerPath, dataListener, Executors.newSingleThreadExecutor());

        client.create(providerPath, "aaaa", true);

        children = client.getChildren(testNodeFullPath);
        Assert.assertNotNull(children);
        Assert.assertEquals(1, children.size());
        Thread.sleep(1000);

        Assert.assertTrue(assertCache.containsKey(providerPath));
        Assert.assertEquals(2, assertCache.get(providerPath).size());
        Assert.assertEquals("aaaa", new String(assertCache.get(providerPath).get(1).getData()));

        client.create(providerPath, "bbbb", true);

        Thread.sleep(1000);

        Assert.assertTrue(assertCache.containsKey(providerPath));
        Assert.assertEquals(2, assertCache.get(providerPath).size());
        Assert.assertEquals("aaaa", new String(assertCache.get(providerPath).get(0).getData()));
        Assert.assertEquals("bbbb", new String(assertCache.get(providerPath).get(1).getData()));

        client.create(providerPath, "cccc", false);

        Thread.sleep(1000);

        Assert.assertTrue(assertCache.containsKey(providerPath));
        Assert.assertEquals(2, assertCache.get(providerPath).size());
        Assert.assertEquals(null, assertCache.get(providerPath).get(0));
        Assert.assertEquals("cccc", new String(assertCache.get(providerPath).get(1).getData()));

        client.removeDataListener(providerPath, dataListener);

    }

    @Test
    public void testInitError() {

        try {
            ZookeeperClient client = new CuratorZookeeperClient(null);
        } catch (Exception e) {
            LOGGER.info("testInitError success");
        }
    }

    @Test
    public void testGetChildrenError() {
        ZookeeperClient client = new CuratorZookeeperClient(buildConfig());
        ((CuratorZookeeperClient) client).setClient(null);

        final String providerPath = testNodeFullPath + "/provider3";

        try {
            client.getChildren(testRootPath);
        } catch (Exception e) {
            LOGGER.info("testGetChildrenError success");
        }
    }

    @Test
    public void testCreateError() {
        ZookeeperClient client = new CuratorZookeeperClient(buildConfig());
        ((CuratorZookeeperClient) client).setClient(null);

        final String providerPath = testNodeFullPath + "/provider3";

        try {
            client.create(providerPath, "cccc", false);
        } catch (Exception e) {
            LOGGER.info("testCreateError success");
        }
    }

    @Test
    public void testCreateEphemeralError() {
        ZookeeperClient client = new CuratorZookeeperClient(buildConfig());
        ((CuratorZookeeperClient) client).setClient(null);

        final String providerPath = testNodeFullPath + "/provider3";

        try {
            Whitebox.invokeMethod(client, "createEphemeral", providerPath);

        } catch (Exception e) {
            LOGGER.info("testCreateError success");
        }
    }

    @Test
    public void testCreateEphemeralDataError() {
        ZookeeperClient client = new CuratorZookeeperClient(buildConfig());
        ((CuratorZookeeperClient) client).setClient(null);

        final String providerPath = testNodeFullPath + "/provider3";

        try {
            Whitebox.invokeMethod(client, "createEphemeral", providerPath, "aaa");

        } catch (Exception e) {
            LOGGER.info("testCreateError success");
        }
    }

    @Test
    public void testCreatePersistent0Error() {
        ZookeeperClient client = new CuratorZookeeperClient(buildConfig());
        ((CuratorZookeeperClient) client).setClient(null);

        final String providerPath = testNodeFullPath + "/provider3";

        try {
            Whitebox.invokeMethod(client, "createPersistent", providerPath, "aaa");

        } catch (Exception e) {
            LOGGER.info("testCreatePersistent0Error success");
        }
    }

    @Test
    public void testDeleteError() {
        ZookeeperClient client = new CuratorZookeeperClient(buildConfig());
        ((CuratorZookeeperClient) client).setClient(null);

        final String providerPath = testNodeFullPath + "/provider3";

        try {
            client.delete(providerPath);
        } catch (Exception e) {
            LOGGER.info("testDeleteError success");
        }
    }

    @Test
    public void testAddChildListenerError() {
        ZookeeperClient client = new CuratorZookeeperClient(buildConfig());
        ((CuratorZookeeperClient) client).setClient(null);

        final String providerPath = testNodeFullPath + "/provider3";

        try {
            ChildListener childListener = (path, children1) -> {
            };
            client.addChildListener(providerPath, childListener);
        } catch (Exception e) {
            LOGGER.info("testAddChildListenerError success");
        }
    }

    @Test
    public void testAddTargetListenerError() {
        ZookeeperClient client = new CuratorZookeeperClient(buildConfig());
        ((CuratorZookeeperClient) client).setClient(null);

        final String providerPath = testNodeFullPath + "/provider3";

        try {
            DataListener dataListener = (type, oldData, data) -> {
                List<ChildData> list = Arrays.asList(oldData, data);
            };
            client.addDataListener(providerPath, dataListener, Executors.newSingleThreadExecutor());
        } catch (Exception e) {
            LOGGER.info("testAddChildListenerError success");
        }
    }

    @Test
    public void testCloseError() {
        ZookeeperClient client = new CuratorZookeeperClient(buildConfig());
        ((CuratorZookeeperClient) client).setClient(null);

        try {
            client.close();
        } catch (Exception e) {
            LOGGER.info("testCloseError success");
        }
    }

    @Test
    public void testCreatePersistentError() {

        final String providerPath = testNodeFullPath + "/provider3";

        try {
            client.create(providerPath, false);
            Whitebox.invokeMethod(client, "createPersistent", providerPath, "aaa");
        } catch (Exception e) {
            LOGGER.info("testCloseError success");
        }
    }

    @Test
    public void testCheckExists() throws Exception {
        client.create(testNodeFullPath, false);
        boolean exists = Whitebox.invokeMethod(client, "checkExists", testNodeFullPath);
        Assert.assertTrue(exists);
        
        boolean notExists = Whitebox.invokeMethod(client, "checkExists", testNodeFullPath + "/notexist");
        Assert.assertFalse(notExists);
    }

    @Test
    public void testCheckExistsError() throws Exception {
        ZookeeperClient client = new CuratorZookeeperClient(buildConfig());
        ((CuratorZookeeperClient) client).setClient(null);
        
        boolean exists = Whitebox.invokeMethod(client, "checkExists", testNodeFullPath);
        Assert.assertFalse(exists);
    }

    @Test
    public void testCreatePersistentNoData() throws Exception {
        Whitebox.invokeMethod(client, "createPersistent", testRootPath);
        List<String> children = client.getChildren("/");
        Assert.assertTrue(children.contains(testRootName));
    }

    @Test
    public void testCreatePersistentNoDataError() throws Exception {
        ZookeeperClient client = new CuratorZookeeperClient(buildConfig());
        ((CuratorZookeeperClient) client).setClient(null);
        
        try {
            Whitebox.invokeMethod(client, "createPersistent", testRootPath);
        } catch (Exception e) {
            LOGGER.info("testCreatePersistentNoDataError success");
        }
    }

    @Test
    public void testCreateEphemeralNodeExists() throws Exception {
        client.create(testNodeFullPath, false);
        String providerPath = testNodeFullPath + "/provider1";
        client.create(providerPath, true);
        
        Whitebox.invokeMethod(client, "createEphemeral", providerPath);
        List<String> children = client.getChildren(testNodeFullPath);
        Assert.assertEquals(1, children.size());
    }

    @Test
    public void testCreateEphemeralWithDataNodeExists() throws Exception {
        client.create(testNodeFullPath, false);
        String providerPath = testNodeFullPath + "/provider1";
        client.create(providerPath, "data1", true);
        
        Whitebox.invokeMethod(client, "createEphemeral", providerPath, "data2");
        List<String> children = client.getChildren(testNodeFullPath);
        Assert.assertEquals(1, children.size());
    }

    @Test
    public void testCreatePersistentWithDataNodeExists() throws Exception {
        client.create(testNodeFullPath, false);
        String providerPath = testNodeFullPath + "/provider1";
        client.create(providerPath, "data1", false);
        
        Whitebox.invokeMethod(client, "createPersistent", providerPath, "data2");
        List<String> children = client.getChildren(testNodeFullPath);
        Assert.assertEquals(1, children.size());
    }

    @Test
    public void testStateListener() throws Exception {
        final Map<String, Integer> stateMap = new HashMap<>();
        client.addStateListener(state -> {
            stateMap.put(state.name(), stateMap.getOrDefault(state.name(), 0) + 1);
        });
        
        client.create(testRootPath, false);
        Thread.sleep(500);
        Assert.assertTrue(client.isConnected());
    }

    @Test
    public void testGetCuratorConnectStringError() {
        try {
            RegistryCenterConfig config = new RegistryCenterConfig();
            config.setAddresses("");
            ZookeeperClient client = new CuratorZookeeperClient(config);
        } catch (Exception e) {
            LOGGER.info("testGetCuratorConnectStringError success");
        }
    }

    @Test
    public void testCuratorChildWatcherProcess() throws Exception {
        client.create(testNodeFullPath, false);
        
        final Map<String, List<String>> childrenMap = new HashMap<>();
        ChildListener childListener = (path, children) -> {
            childrenMap.put(path, children);
        };
        
        client.addChildListener(testNodeFullPath, childListener);
        
        String providerPath = testNodeFullPath + "/provider1";
        client.create(providerPath, true);
        
        Thread.sleep(1000);
        Assert.assertTrue(childrenMap.containsKey(testNodeFullPath));
        Assert.assertEquals(1, childrenMap.get(testNodeFullPath).size());
    }

    @Test
    public void testCuratorChildWatcherUnwatch() throws Exception {
        client.create(testNodeFullPath, false);
        
        final Map<String, List<String>> childrenMap = new HashMap<>();
        ChildListener childListener = (path, children) -> {
            childrenMap.put(path, children);
        };
        
        client.addChildListener(testNodeFullPath, childListener);
        client.removeChildListener(testNodeFullPath, childListener);
        
        String providerPath = testNodeFullPath + "/provider1";
        client.create(providerPath, true);
        
        Thread.sleep(1000);
        Assert.assertFalse(childrenMap.containsKey(testNodeFullPath));
    }

    @Test
    public void testCuratorDataCacheGetterSetter() throws Exception {
        DataListener dataListener1 = (type, oldData, data) -> {};
        DataListener dataListener2 = (type, oldData, data) -> {};
        
        CuratorZookeeperClient.CuratorDataCacheImpl cache = 
            new CuratorZookeeperClient.CuratorDataCacheImpl(dataListener1);
        
        Assert.assertEquals(dataListener1, cache.getDataListener());
        
        cache.setDataListener(dataListener2);
        Assert.assertEquals(dataListener2, cache.getDataListener());
    }

    @Test
    public void testRemoveDataListenerWithNullCache() throws Exception {
        client.create(testNodeFullPath, false);
        String providerPath = testNodeFullPath + "/provider1";
        
        DataListener dataListener = (type, oldData, data) -> {};
        client.removeDataListener(providerPath, dataListener);
    }

    @Test
    public void testCreateWithContent() throws Exception {
        client.create(testNodeFullPath, false);
        String providerPath = testNodeFullPath + "/provider1";
        client.create(providerPath, "testContent", false);
        
        List<String> children = client.getChildren(testNodeFullPath);
        Assert.assertEquals(1, children.size());
        Assert.assertEquals("provider1", children.get(0));
    }

    @Test
    public void testCreateEphemeralWithContent() throws Exception {
        client.create(testNodeFullPath, false);
        String providerPath = testNodeFullPath + "/provider1";
        client.create(providerPath, "testContent", true);
        
        List<String> children = client.getChildren(testNodeFullPath);
        Assert.assertEquals(1, children.size());
        Assert.assertEquals("provider1", children.get(0));
    }

    @Test
    public void testDoClose() throws Exception {
        ZookeeperClient newClient = curatorZookeeperFactory.connect(buildConfig());
        newClient.create(testRootPath, false);
        Assert.assertTrue(newClient.isConnected());
        newClient.close();
        newClient.close();
    }

    @Test
    public void testGetState() throws Exception {
        client.create(testRootPath, false);
        Thread.sleep(500);
    }

    @Test
    public void testAddStateListener() throws Exception {
        final Map<String, Integer> stateMap = new HashMap<>();
        client.addStateListener(state -> {
            stateMap.put(state.name(), stateMap.getOrDefault(state.name(), 0) + 1);
        });
        
        client.removeStateListener(state -> {});
    }

    @Test
    public void testGetContent() throws Exception {
        client.create(testNodeFullPath, "testContent", false);
        String content = client.getContent(testNodeFullPath);
        Assert.assertNull(content);
    }

    @Test
    public void testGetContentNotExist() throws Exception {
        String content = client.getContent(testNodeFullPath + "/notexist");
        Assert.assertNull(content);
    }

}

