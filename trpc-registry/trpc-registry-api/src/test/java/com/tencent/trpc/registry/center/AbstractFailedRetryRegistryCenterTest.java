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

package com.tencent.trpc.registry.center;


import static com.tencent.trpc.registry.common.ConfigConstants.DEFAULT_REGISTRY_CENTER_RETRY_TIMES;
import static com.tencent.trpc.registry.common.ConfigConstants.REGISTRY_CENTER_CACHE_ALIVE_TIME_SECS_KEY;
import static com.tencent.trpc.registry.common.ConfigConstants.REGISTRY_CENTER_CACHE_FILE_PATH_KEY;
import static com.tencent.trpc.registry.common.ConfigConstants.REGISTRY_CENTER_RETRY_PERIOD_KEY;
import static com.tencent.trpc.registry.common.ConfigConstants.REGISTRY_CENTER_SAVE_CACHE_KEY;
import static com.tencent.trpc.registry.common.ConfigConstants.REGISTRY_CENTER_SYNCED_SAVE_CACHE_KEY;
import static com.tencent.trpc.registry.common.Constants.DEFAULT_REGISTRY_CENTER_SERVICE_TYPE;

import com.google.common.collect.Lists;
import com.tencent.trpc.core.common.config.PluginConfig;
import com.tencent.trpc.core.exception.TRpcExtensionException;
import com.tencent.trpc.core.logger.Logger;
import com.tencent.trpc.core.logger.LoggerFactory;
import com.tencent.trpc.core.registry.RegisterInfo;
import com.tencent.trpc.registry.center.AbstractFailedRetryRegistryCenter.RegisterInfoListenerHolder;
import com.tencent.trpc.registry.common.RegistryCenterEnum;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class AbstractFailedRetryRegistryCenterTest {

    private static final Logger logger = LoggerFactory.getLogger(AbstractFailedRetryRegistryCenterTest.class);

    private static AbstractFailedRetryRegistryCenter clientRegistry;
    private static AbstractFailedRetryRegistryCenter serverRegistry;


    private static Map<RegisterInfo, NotifyListener> subscribeMap = new ConcurrentHashMap<>();
    private static final String clientCacheFilePath = "/tmp/zookeeper.cache";
    private static String serverCacheFilePath = "/tmp/server_zookeeper.cache";
    private static int CACHE_EXPIRE_TIME = 1;

    @Before
    public void setUp() throws Exception {
        clientRegistry = buildFailedRetryRegistryCenter();
        clientRegistry.setPluginConfig(initPluginConfig());
        serverRegistry = buildFailedRetryRegistryCenter();
        serverRegistry.setPluginConfig(initPluginConfig());
    }

    @After
    public void tearDown() throws Exception {
        clientRegistry.destroy();
        serverRegistry.destroy();
        this.delCacheFile();
    }

    private AbstractFailedRetryRegistryCenter buildFailedRetryRegistryCenter() {
        return new AbstractFailedRetryRegistryCenter() {

            private AtomicInteger count = new AtomicInteger();

            private boolean canNormalReturn() {
                if (count.getAndIncrement() == DEFAULT_REGISTRY_CENTER_RETRY_TIMES) {
                    count.set(0);
                    return true;
                }
                return false;
            }

            @Override
            public void doRegister(RegisterInfo registerInfo) {
                if (canNormalReturn()) {
                    return;
                }
                throw new NullPointerException();
            }

            @Override
            public void doUnregister(RegisterInfo registerInfo) {
                if (canNormalReturn()) {
                    return;
                }
                throw new NullPointerException();
            }

            @Override
            public void doSubscribe(RegisterInfo registerInfo, NotifyListener notifyListener) {
                if (canNormalReturn()) {
                    return;
                }
                throw new NullPointerException();
            }

            @Override
            public void doUnsubscribe(RegisterInfo registerInfo, NotifyListener notifyListener) {
                if (canNormalReturn()) {
                    return;
                }
                throw new NullPointerException();
            }

            @Override
            public void doNotify(RegisterInfo registerInfo, NotifyListener notifyListener,
                    List<RegisterInfo> updatingRegisterInfos) {
                if (canNormalReturn()) {
                    super.doNotify(registerInfo, notifyListener, updatingRegisterInfos);
                    return;
                }
                throw new NullPointerException();
            }

            @Override
            public void init() throws TRpcExtensionException {
                logger.debug("AbstractFailedRetryRegistryCenter init");
            }
        };
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
        properties.put(REGISTRY_CENTER_RETRY_PERIOD_KEY, 10);
        PluginConfig pluginConfig = new PluginConfig("zookeeper", AbstractFailedRetryRegistryCenter.class,
                properties);
        return pluginConfig;
    }

    private RegisterInfo buildRegisterInfo() {
        RegisterInfo registerInfo = new RegisterInfo("trpc", "0.0.0.0", 12001,
                "group", "v1", "test.service1");
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

    @Test
    public void testRegistry() throws InterruptedException {
        Assert.assertEquals(0, clientRegistry.getFailedTasks().size());
        RegisterInfo registerInfo = buildRegisterInfo();
        clientRegistry.register(registerInfo);
        Assert.assertEquals(1, clientRegistry.getFailedTasks().size());
        Thread.sleep(500);
        Assert.assertEquals(0, clientRegistry.getFailedTasks().size());
    }

    @Test
    public void testUnregistry() throws InterruptedException {
        this.testRegistry();

        Assert.assertEquals(0, clientRegistry.getFailedTasks().size());
        RegisterInfo registerInfo = buildRegisterInfo();
        clientRegistry.unregister(registerInfo);
        Assert.assertEquals(1, clientRegistry.getFailedTasks().size());
        Thread.sleep(500);
        Assert.assertEquals(0, clientRegistry.getFailedTasks().size());
    }

    @Test
    public void testSubscribe() throws InterruptedException {
        this.delCacheFile();
        Assert.assertEquals(0, clientRegistry.getFailedTasks().size());
        RegisterInfo registerInfo = buildRegisterInfo();
        NotifyListener discovery = getNotifyListener(registerInfo);
        clientRegistry.subscribe(registerInfo, discovery);
        Assert.assertEquals(1, clientRegistry.getFailedTasks().size());
        Thread.sleep(500);
        Assert.assertEquals(0, clientRegistry.getFailedTasks().size());
    }

    @Test
    public void testUnsubscribe() throws InterruptedException {
        this.testSubscribe();

        Assert.assertEquals(0, clientRegistry.getFailedTasks().size());
        RegisterInfo registerInfo = buildRegisterInfo();
        NotifyListener discovery = getNotifyListener(registerInfo);
        clientRegistry.unsubscribe(registerInfo, discovery);
        Assert.assertEquals(1, clientRegistry.getFailedTasks().size());
        Thread.sleep(500);
        Assert.assertEquals(0, clientRegistry.getFailedTasks().size());
    }

    @Test
    public void testNotify() throws InterruptedException {
        RegisterInfo registerInfo = buildRegisterInfo();
        List<RegisterInfo> registerInfos = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            registerInfos.add(buildRegisterInfo(12000 + i));
        }
        registerInfos.add(buildRegisterInfo("test.service2"));
        NotifyListener discovery = getNotifyListener(registerInfo);

        Assert.assertEquals(0, clientRegistry.getFailedTasks().size());
        clientRegistry.notify(registerInfo, discovery, registerInfos);
        Assert.assertEquals(1, clientRegistry.getFailedTasks().size());
        Thread.sleep(500);
        Assert.assertEquals(0, clientRegistry.getFailedTasks().size());
        Assert.assertEquals(1, clientRegistry.getNotifiedRegisterInfos().size());
        Assert.assertEquals(10,
                clientRegistry.getNotifiedRegisterInfos().get(registerInfo).getRegisterInfos(
                        RegistryCenterEnum.transferFrom(DEFAULT_REGISTRY_CENTER_SERVICE_TYPE)).size());
    }

    @Test
    public void testNotify2() throws InterruptedException {
        RegisterInfo registerInfo = buildRegisterInfo();
        NotifyListener discovery = getNotifyListener(registerInfo);
        clientRegistry.notify(registerInfo, discovery, Lists.newArrayList());
        Assert.assertEquals(1, clientRegistry.getFailedTasks().size());
    }

    /**
     * 测试订阅失败时从缓存读取的降级操作
     */
    @Test
    public void testSubscribeCache() throws InterruptedException {
        this.testNotify();
        Assert.assertEquals(0, clientRegistry.getFailedTasks().size());
        RegisterInfo registerInfo = buildRegisterInfo();
        NotifyListener discovery = getNotifyListener(registerInfo);
        clientRegistry.subscribe(registerInfo, discovery);
        Assert.assertEquals(1, clientRegistry.getFailedTasks().size());
        Thread.sleep(500);
        Assert.assertEquals(0, clientRegistry.getFailedTasks().size());
    }

    @Test
    public void testRecover() throws NoSuchMethodException, InvocationTargetException,
            IllegalAccessException, InterruptedException {
        this.testRegistry();
        this.testSubscribe();
        Method method = clientRegistry.getClass().getSuperclass().getSuperclass().getDeclaredMethod("recover");
        method.setAccessible(true);
        method.invoke(clientRegistry);
        Assert.assertEquals(1, clientRegistry.getRegisteredRegisterInfos().size());
        Assert.assertEquals(1, clientRegistry.getSubscribedRegisterInfos().size());
        Assert.assertEquals(1,
                clientRegistry.getSubscribedRegisterInfos().get(buildRegisterInfo()).getNotifyListeners().size());
    }

    @Test
    public void testAddFailedRegisteredTask() {
        RegisterInfo registerInfo = buildRegisterInfo();
        serverRegistry.addFailedRegisteredTask(registerInfo);
        serverRegistry.addFailedRegisteredTask(registerInfo);
    }

    @Test
    public void testAddFailedSubscribedTask() {
        RegisterInfo registerInfo = buildRegisterInfo();
        NotifyListener notifyListener = getNotifyListener(registerInfo);
        serverRegistry.addFailedSubscribedTask(registerInfo, notifyListener);
        serverRegistry.addFailedSubscribedTask(registerInfo, notifyListener);
    }

    @Test
    public void testAddFailedSubscribedTaskException() {
        try {
            serverRegistry.addFailedSubscribedTask(null, null);
        } catch (Exception e) {
            logger.warn("addFailedSubscribedTask warn {}", e);
        }
    }

    @Test
    public void testNotifyListenerHolderEquals() {
        RegisterInfo registerInfo = buildRegisterInfo();
        NotifyListener notifyListener = getNotifyListener(registerInfo);
        RegisterInfoListenerHolder registerInfoListenerHolder =
                new RegisterInfoListenerHolder(registerInfo, notifyListener);
        boolean test = registerInfoListenerHolder.equals("test");
        Assert.assertFalse(test);
    }


    @Test
    public void testRecoverRegistered() {
        serverRegistry.recoverRegistered();
    }


    @Test
    public void testRecoverSubscribed() {
        serverRegistry.recoverSubscribed();
    }

}
