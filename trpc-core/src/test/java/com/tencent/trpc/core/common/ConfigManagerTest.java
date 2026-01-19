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

package com.tencent.trpc.core.common;

import com.tencent.trpc.core.common.config.BackendConfig;
import com.tencent.trpc.core.common.config.ClientConfig;
import com.tencent.trpc.core.common.config.GlobalConfig;
import com.tencent.trpc.core.common.config.PluginConfig;
import com.tencent.trpc.core.common.config.ServerConfig;
import com.tencent.trpc.core.rpc.AppInitializer;
import com.tencent.trpc.core.worker.spi.WorkerPool;
import com.tencent.trpc.core.worker.support.thread.ThreadPoolConfig;
import com.tencent.trpc.core.worker.support.thread.ThreadWorkerPool;
import java.util.HashMap;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ConfigManagerTest {

    private static final int TCP_PORT = 8090;
    private static final long WAIT_TIME = 2000;

    /**
     * ConfigManager start
     */
    @BeforeEach
    public void setUp() {
        ConfigManager.stopTest();
        GlobalConfig globalConfig = new GlobalConfig();
        globalConfig.setContainerName("cn");
        globalConfig.setEnableSet(true);
        globalConfig.setEnvName("en");
        globalConfig.setNamespace("prod");
        globalConfig.setFullSetName("fsn");
        ConfigManager.getInstance().addTRPCRunListener(new TRPCRunListener() {
            @Override
            public void starting() {
                TRPCRunListener.super.starting();
            }
        });
        ConfigManager.getInstance().setGlobalConfig(globalConfig);
        ConfigManager.getInstance().getServerConfig().setRunListeners(Lists.newArrayList());
        ConfigManager.getInstance().getServerConfig()
                .getRunListeners().add("listener1");
        BackendConfig backendConfig = new BackendConfig();
        backendConfig.setNamingUrl("ip://127.0.0.1:" + TCP_PORT);
        BackendConfig backendConfig2 = new BackendConfig();
        backendConfig2.setNamingUrl("ip://127.0.0.1:" + TCP_PORT + 1);
        ClientConfig clientConfig = new ClientConfig();
        clientConfig.addBackendConfig(backendConfig);
        clientConfig.addBackendConfig(backendConfig2);
        ConfigManager.getInstance().setClientConfig(clientConfig);

        ConfigManager.startTest();
    }

    @AfterEach
    public void teardown() {
        ConfigManager.stopTest();
    }

    @Test
    public void testStart() {
        ConfigManager.getInstance().start();
        ConfigManager.getInstance().stop();
    }

    @Test
    public void testGetInstance() {
        ConfigManager instance = ConfigManager.getInstance();
        Assertions.assertNotNull(instance);
    }

    @Test
    public void testSetDefault() {
        ConfigManager.getInstance().setDefault();
    }

    @Test
    public void testRegisterPlugin() {
        ThreadPoolConfig poolConfig = new ThreadPoolConfig();
        poolConfig.setCorePoolSize(1);
        PluginConfig pluginConfig = new PluginConfig("workerPool", WorkerPool.class,
                ThreadWorkerPool.class, poolConfig.toMap());
        ConfigManager.getInstance().registerPlugin(pluginConfig);
    }

    @Test
    public void testGetGlobalConfig() {
        GlobalConfig globalConfig = ConfigManager.getInstance().getGlobalConfig();
        Assertions.assertNotNull(globalConfig);
        Assertions.assertEquals("cn", globalConfig.getContainerName());
        Assertions.assertEquals("en", globalConfig.getEnvName());
        Assertions.assertEquals("prod", globalConfig.getNamespace());
        Assertions.assertEquals("fsn", globalConfig.getFullSetName());
        Assertions.assertTrue(globalConfig.isEnableSet());
    }

    @Test
    public void testGetServerConfig() {
        Assertions.assertNotNull(ConfigManager.getInstance().getServerConfig());
    }

    @Test
    public void testSetServerConfig() {
        ServerConfig serverConfig = new ServerConfig();
        serverConfig.setServer("aaa");
        ConfigManager.getInstance().setServerConfig(serverConfig);
        Assertions.assertEquals("aaa", ConfigManager.getInstance().getServerConfig().getServer());
    }

    @Test
    public void testGetClientConfig() {
        Assertions.assertNotNull(ConfigManager.getInstance().getClientConfig());
    }

    @Test
    public void testSetClientConfig() {
        ClientConfig clientConfig = new ClientConfig();
        clientConfig.setCharset("aaa");
        ConfigManager.getInstance().setClientConfig(clientConfig);
        Assertions.assertEquals("aaa", ConfigManager.getInstance().getClientConfig().getCharset());
    }

    @Test
    public void testGetPluginConfigMap() {
        Assertions.assertNotNull(ConfigManager.getInstance().getPluginConfigMap());
    }

    @Test
    public void testSetPluginConfigMap() {
        ConfigManager.getInstance().setPluginConfigMap(new HashMap<>());
        Assertions.assertNotNull(ConfigManager.getInstance().getPluginConfigMap());
    }

    @Test
    public void testGetAppInitializer() {
        Assertions.assertNull(ConfigManager.getInstance().getAppInitializer());
    }

    @Test
    public void testSetAppInitializer() {
        ConfigManager.getInstance().setAppInitializer(new AppInitializer() {
            @Override
            public void init() {

            }

            @Override
            public void stop() {

            }
        });
    }

    @Test
    public void testGracefulRestart() throws InterruptedException {
        ConfigManager.startTest();
        ServerConfig serverConfig = new ServerConfig();
        serverConfig.setWaitTimeout(WAIT_TIME);
        serverConfig.setCloseTimeout(WAIT_TIME);
        serverConfig.setDefault();
        ConfigManager.getInstance().setServerConfig(serverConfig);
        ConfigManager.getInstance().start();
        Thread.sleep(WAIT_TIME);
        ConfigManager.getInstance().stop();
    }
}
