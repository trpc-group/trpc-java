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

package com.tencent.trpc.registry.center;

import static com.tencent.trpc.registry.common.ConfigConstants.REGISTRY_CENTER_CACHE_ALIVE_TIME_SECS_KEY;
import static com.tencent.trpc.registry.common.ConfigConstants.REGISTRY_CENTER_CACHE_FILE_PATH_KEY;
import static com.tencent.trpc.registry.common.ConfigConstants.REGISTRY_CENTER_SAVE_CACHE_KEY;
import static com.tencent.trpc.registry.common.ConfigConstants.REGISTRY_CENTER_SYNCED_SAVE_CACHE_KEY;
import static com.tencent.trpc.registry.common.Constants.DEFAULT_REGISTRY_CENTER_SERVICE_TYPE;

import com.google.common.collect.Lists;
import com.tencent.trpc.core.common.config.PluginConfig;
import com.tencent.trpc.core.exception.TRpcExtensionException;
import com.tencent.trpc.core.logger.Logger;
import com.tencent.trpc.core.logger.LoggerFactory;
import com.tencent.trpc.core.registry.RegisterInfo;
import com.tencent.trpc.core.worker.support.thread.ThreadWorkerPool;
import com.tencent.trpc.registry.common.RegistryCenterEnum;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class AbstractRegistryCenterTest {

    private static final Logger logger = LoggerFactory.getLogger(AbstractRegistryCenterTest.class);

    private static AbstractRegistryCenter clientRegistry;
    private static AbstractRegistryCenter serverRegistry;
    private static Map<RegisterInfo, NotifyListener> subscribeMap = new ConcurrentHashMap<>();
    private static String clientCacheFilePath = "/tmp/zookeeper.cache";
    private static String serverCacheFilePath = "/tmp/server_zookeeper.cache";
    private static int CACHE_EXPIRE_TIME = 1;

    @Before
    public void setUp() throws Exception {
        clientRegistry = new AbstractRegistryCenter() {
            @Override
            public void init() throws TRpcExtensionException {
                logger.debug("client registry test init");
            }
        };
        clientRegistry.setPluginConfig(initPluginConfig());

        serverRegistry = new AbstractRegistryCenter() {
            @Override
            public void init() throws TRpcExtensionException {
                logger.debug("client registry test init");
            }
        };
        serverRegistry.setPluginConfig(
                initPluginConfig("0.0.0.0", 2181, false, false,
                        serverCacheFilePath, CACHE_EXPIRE_TIME));
        serverRegistry.getRegistryCenterConfig();
    }

    @Test
    public void testSetPluginConfig() {
        AbstractRegistryCenter registryCenter = new AbstractRegistryCenter() {
            @Override
            public void init() throws TRpcExtensionException {

            }
        };
        try {
            PluginConfig pluginConfig = new PluginConfig("id", ThreadWorkerPool.class);
            registryCenter.setPluginConfig(pluginConfig);
        } catch (Exception e) {
            logger.warn("setPluginConfig error {}", e);
        }
    }

    @After
    public void tearDown() throws Exception {
        this.delCacheFile();
        clientRegistry.destroy();
        serverRegistry.destroy();
    }

    private PluginConfig initPluginConfig() {
        return initPluginConfig("0.0.0.0", 2181, false, false, clientCacheFilePath,
                CACHE_EXPIRE_TIME);
    }

    private PluginConfig initPluginConfig(String ip, int port, boolean saveCache, boolean useSync,
            String syncFilePath, int cacheExpireTime) {
        Map<String, Object> properties = new HashMap<>();
        properties.put("ip", ip);
        properties.put("port", port);
        properties.put(REGISTRY_CENTER_SAVE_CACHE_KEY, saveCache);
        properties.put(REGISTRY_CENTER_SYNCED_SAVE_CACHE_KEY, useSync);
        properties.put(REGISTRY_CENTER_CACHE_FILE_PATH_KEY, syncFilePath);
        properties.put(REGISTRY_CENTER_CACHE_ALIVE_TIME_SECS_KEY, cacheExpireTime);
        PluginConfig pluginConfig = new PluginConfig("zookeeper", AbstractRegistryCenter.class,
                properties);
        return pluginConfig;
    }

    private RegisterInfo buildRegisterInfo() {
        RegisterInfo registerInfo = new RegisterInfo("trpc", "0.0.0.0", 12001,
                "test.service1");
        return registerInfo;
    }

    private RegisterInfo buildRegisterInfo(int port) {
        RegisterInfo registerInfo = new RegisterInfo("trpc", "0.0.0.0", port,
                "test.service1");
        return registerInfo;
    }

    private RegisterInfo buildRegisterInfo(String serviceName) {
        RegisterInfo registerInfo = new RegisterInfo("trpc", "0.0.0.0", 12001,
                serviceName);
        return registerInfo;
    }

    private void delCacheFile() {
        File file = new File(clientCacheFilePath);
        if (file.exists()) {
            file.delete();
        }
        file = new File(serverCacheFilePath);
        if (file.exists()) {
            file.delete();
        }
    }

    private NotifyListener getNotifyListener(RegisterInfo registerInfo) {
        return subscribeMap.computeIfAbsent(registerInfo,
                registerInfo1 -> new NotifyListener() {
                    @Override
                    public void notify(List<RegisterInfo> registerInfos) {

                    }

                    @Override
                    public void destroy() throws TRpcExtensionException {

                    }
                });
    }

    @Test
    public void testRegistry() {
        RegisterInfo registerInfo = buildRegisterInfo();
        Assert.assertEquals(0, clientRegistry.getRegisteredRegisterInfos().size());
        clientRegistry.register(registerInfo);
        Assert.assertEquals(1, clientRegistry.getRegisteredRegisterInfos().size());

    }

    @Test
    public void testUnregistry() {
        this.testRegistry();
        RegisterInfo registerInfo = buildRegisterInfo();
        clientRegistry.unregister(registerInfo);
        Assert.assertEquals(0, clientRegistry.getRegisteredRegisterInfos().size());
    }

    @Test
    public void testSubscribe() {

        RegisterInfo registerInfo = buildRegisterInfo();
        NotifyListener discovery = getNotifyListener(registerInfo);
        Assert.assertEquals(0, clientRegistry.getSubscribedRegisterInfos().size());
        clientRegistry.subscribe(registerInfo, discovery);
        Assert.assertEquals(1, clientRegistry.getSubscribedRegisterInfos().size());
        Assert.assertEquals(1,
                clientRegistry.getSubscribedRegisterInfos().get(registerInfo).getNotifyListeners().size());
    }

    @Test
    public void testUnsubscribe() {
        this.testSubscribe();
        RegisterInfo registerInfo = buildRegisterInfo();
        NotifyListener discovery = getNotifyListener(registerInfo);
        clientRegistry.unsubscribe(registerInfo, discovery);
        Assert.assertEquals(0, clientRegistry.getSubscribedRegisterInfos().size());
    }

    @Test
    public void testNotify() {
        RegisterInfo registerInfo = buildRegisterInfo();
        List<RegisterInfo> registerInfos = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            registerInfos.add(buildRegisterInfo(12000 + i));
        }
        registerInfos.add(buildRegisterInfo("test.service2"));
        NotifyListener discovery = getNotifyListener(registerInfo);

        Assert.assertEquals(0, clientRegistry.getNotifiedRegisterInfos().size());
        clientRegistry.notify(registerInfo, discovery, registerInfos);
        Assert.assertEquals(1, clientRegistry.getNotifiedRegisterInfos().size());
        Assert.assertEquals(10,
                clientRegistry.getNotifiedRegisterInfos().get(registerInfo).getRegisterInfos(
                        RegistryCenterEnum.transferFrom(DEFAULT_REGISTRY_CENTER_SERVICE_TYPE)).size());
    }

    @Test
    public void testNotify01() {
        RegisterInfo registerInfo = buildRegisterInfo();
        NotifyListener discovery = getNotifyListener(registerInfo);
        Assert.assertEquals(0, clientRegistry.getNotifiedRegisterInfos().size());
        clientRegistry.notify(registerInfo, discovery, Lists.newArrayList());
        Assert.assertEquals(1, clientRegistry.getNotifiedRegisterInfos().size());
        Assert.assertEquals(0,
                clientRegistry.getNotifiedRegisterInfos().get(registerInfo).getRegisterInfos(
                        RegistryCenterEnum.transferFrom(DEFAULT_REGISTRY_CENTER_SERVICE_TYPE)).size());
    }


    @Test
    public void testDestroy() {
        RegisterInfo registerInfo = buildRegisterInfo();
        clientRegistry.register(registerInfo);
        NotifyListener discovery = getNotifyListener(registerInfo);
        clientRegistry.subscribe(registerInfo, discovery);
        clientRegistry.destroy();
        Assert.assertEquals(0, clientRegistry.getSubscribedRegisterInfos().size());
        Assert.assertEquals(0, clientRegistry.getSubscribedRegisterInfos().size());
    }

    @Test
    public void testGetCache() {
        this.delCacheFile();
        this.testNotify();
        RegisterInfo registerInfo = buildRegisterInfo();
        List<RegisterInfo> registerInfos = clientRegistry.cache
                .getRegisterInfos(registerInfo.getServiceName());
        Assert.assertEquals(10, registerInfos.size());
    }

    @Test
    public void testRecover()
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        this.testRegistry();
        this.testSubscribe();
        Method method = clientRegistry.getClass().getSuperclass().getDeclaredMethod("recover");
        method.setAccessible(true);
        method.invoke(clientRegistry);
        Assert.assertEquals(1, clientRegistry.getRegisteredRegisterInfos().size());
        Assert.assertEquals(1, clientRegistry.getSubscribedRegisterInfos().size());
        Assert.assertEquals(1,
                clientRegistry.getSubscribedRegisterInfos().get(buildRegisterInfo()).getNotifyListeners().size());
    }

    @Test
    public void testExpireCache()
            throws InterruptedException, NoSuchMethodException, InvocationTargetException,
            IllegalAccessException {
        Method expireCacheMethod = clientRegistry.getClass().getSuperclass()
                .getDeclaredMethod("expireCache");
        expireCacheMethod.setAccessible(true);
        this.delCacheFile();
        this.testGetCache();

        expireCacheMethod.invoke(clientRegistry);
        Thread.sleep((CACHE_EXPIRE_TIME + 1) * 1000);
        List<RegisterInfo> registerInfos = clientRegistry.cache
                .getRegisterInfos(buildRegisterInfo().getServiceName());
        Assert.assertEquals(0, registerInfos.size());

    }

    @Test
    public void testRedoExpireCache()
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException,
            InterruptedException {
        Method expireCacheMethod = clientRegistry.getClass().getSuperclass()
                .getDeclaredMethod("expireCache");
        expireCacheMethod.setAccessible(true);
        Method redoExpireCacheMethod = clientRegistry.getClass().getSuperclass()
                .getDeclaredMethod("cancelExpireCache");
        redoExpireCacheMethod.setAccessible(true);

        this.delCacheFile();
        this.testGetCache();

        expireCacheMethod.invoke(clientRegistry);
        redoExpireCacheMethod.invoke(clientRegistry);
        Thread.sleep((CACHE_EXPIRE_TIME + 1) * 1000);
        List<RegisterInfo> registerInfos = clientRegistry.cache
                .getRegisterInfos(buildRegisterInfo().getServiceName());
        Assert.assertEquals(10, registerInfos.size());
    }

    @Test
    public void loadProperties()
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        this.delCacheFile();
        this.testGetCache();
        clientRegistry.setPluginConfig(initPluginConfig());
    }

    @Test
    public void testInvalidSyncFile()
            throws InterruptedException, NoSuchMethodException, InvocationTargetException,
            IllegalAccessException {
        try {
            serverRegistry.setPluginConfig(
                    initPluginConfig("0.0.0.0", 2181,
                            false, false, "/xxxx/" + serverCacheFilePath,
                            CACHE_EXPIRE_TIME));
        } catch (Exception e) {
            Assert.assertTrue(e instanceof IllegalArgumentException);
        }

    }

    @Test
    public void testRecoverRegistered() {
        serverRegistry.recoverRegistered();
    }


    @Test
    public void testRecoverSubscribed() {
        serverRegistry.recoverSubscribed();
    }

    @Test
    public void testGetRegistryCenterConfig() {
        Assert.assertNotNull(clientRegistry.getRegistryCenterConfig());
    }

    @Test
    public void testGetRegisteredRegisterInfos() {
        RegisterInfo registerInfo = buildRegisterInfo();
        clientRegistry.register(registerInfo);
        Assert.assertEquals(1, clientRegistry.getRegisteredRegisterInfos().size());
    }

    @Test
    public void testGetSubscribedRegisterInfos() {
        RegisterInfo registerInfo = buildRegisterInfo();
        NotifyListener discovery = getNotifyListener(registerInfo);
        clientRegistry.subscribe(registerInfo, discovery);
        Assert.assertEquals(1, clientRegistry.getSubscribedRegisterInfos().size());
    }

    @Test
    public void testGetNotifiedRegisterInfos() {
        RegisterInfo registerInfo = buildRegisterInfo();
        List<RegisterInfo> registerInfos = new ArrayList<>();
        registerInfos.add(buildRegisterInfo(12000));
        NotifyListener discovery = getNotifyListener(registerInfo);
        clientRegistry.notify(registerInfo, discovery, registerInfos);
        Assert.assertEquals(1, clientRegistry.getNotifiedRegisterInfos().size());
    }

    @Test
    public void testUnsubscribeWithEmptyListeners() {
        RegisterInfo registerInfo = buildRegisterInfo();
        NotifyListener discovery = getNotifyListener(registerInfo);
        clientRegistry.unsubscribe(registerInfo, discovery);
    }

    @Test
    public void testUnsubscribeMultipleTimes() {
        RegisterInfo registerInfo = buildRegisterInfo();
        NotifyListener discovery1 = new NotifyListener() {
            @Override
            public void notify(List<RegisterInfo> registerInfos) {
            }

            @Override
            public void destroy() throws TRpcExtensionException {
            }
        };
        NotifyListener discovery2 = new NotifyListener() {
            @Override
            public void notify(List<RegisterInfo> registerInfos) {
            }

            @Override
            public void destroy() throws TRpcExtensionException {
            }
        };
        clientRegistry.subscribe(registerInfo, discovery1);
        clientRegistry.subscribe(registerInfo, discovery2);
        Assert.assertEquals(2,
                clientRegistry.getSubscribedRegisterInfos().get(registerInfo).getNotifyListeners().size());
        clientRegistry.unsubscribe(registerInfo, discovery1);
        Assert.assertEquals(1,
                clientRegistry.getSubscribedRegisterInfos().get(registerInfo).getNotifyListeners().size());
        clientRegistry.unsubscribe(registerInfo, discovery2);
        Assert.assertEquals(0, clientRegistry.getSubscribedRegisterInfos().size());
    }

}
