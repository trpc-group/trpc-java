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

package com.tencent.trpc.registry.util;

import static com.tencent.trpc.registry.common.ConfigConstants.REGISTRY_CENTER_CACHE_FILE_PATH_KEY;
import static com.tencent.trpc.registry.common.ConfigConstants.REGISTRY_CENTER_SAVE_CACHE_KEY;
import static com.tencent.trpc.registry.common.ConfigConstants.REGISTRY_CENTER_SYNCED_SAVE_CACHE_KEY;

import com.tencent.trpc.core.common.config.PluginConfig;
import com.tencent.trpc.core.logger.Logger;
import com.tencent.trpc.core.logger.LoggerFactory;
import com.tencent.trpc.core.registry.RegisterInfo;
import com.tencent.trpc.registry.center.AbstractRegistryCenter;
import com.tencent.trpc.registry.common.RegistryCenterConfig;
import com.tencent.trpc.registry.common.RegistryCenterData;
import com.tencent.trpc.registry.common.RegistryCenterEnum;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

/**
 * Test class for registry cache.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({RegistryCenterCache.class})
public class RegistryCenterCacheTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(RegistryCenterCacheTest.class);

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void testNormal() {
        try {
            PowerMockito.whenNew(File.class).withAnyArguments().thenReturn(folder.newFile("testfile1.txt"));
            RegistryCenterConfig registryCenterConfig = new RegistryCenterConfig(initPluginConfig());
            RegisterInfo registerInfo = new RegisterInfo("trpc", "0.0.0.0", 12001,
                    "test.service1");
            RegistryCenterData registryCenterData = new RegistryCenterData();
            registryCenterData.putRegisterInfo(RegistryCenterEnum.PROVIDERS, registerInfo);
            registryCenterData.putRegisterInfo(RegistryCenterEnum.PROVIDERS, registerInfo);
            registryCenterData.putRegisterInfo(RegistryCenterEnum.CONSUMERS, registerInfo);
            RegistryCenterCache cache = new RegistryCenterCache(registryCenterConfig);
            cache.save(registerInfo, registryCenterData);
        } catch (Exception e) {
            LOGGER.error("RegistryCenterCacheTest error: ", e);
        }
    }

    @Test
    public void testRetry() {
        try {
            PowerMockito.whenNew(File.class).withAnyArguments().thenReturn(folder.newFile("testfile1.txt"));
            RegistryCenterConfig registryCenterConfig = new RegistryCenterConfig(initPluginConfig());
            RegistryCenterCache cache = new RegistryCenterCache(registryCenterConfig);

            PowerMockito.when(cache, "syncToDisk").thenThrow(new IllegalArgumentException("error"));
            RegisterInfo registerInfo = new RegisterInfo("trpc", "0.0.0.0", 12001,
                    "test.service1");
            RegistryCenterData registryCenterData = new RegistryCenterData();
            registryCenterData.putRegisterInfo(RegistryCenterEnum.PROVIDERS, registerInfo);
            registryCenterData.putRegisterInfo(RegistryCenterEnum.PROVIDERS, registerInfo);
            registryCenterData.putRegisterInfo(RegistryCenterEnum.CONSUMERS, registerInfo);
            cache.save(registerInfo, registryCenterData);
        } catch (Exception e) {
            LOGGER.error("RegistryCenterCacheTest error: ", e);
        }
    }

    private PluginConfig initPluginConfig() {
        Map<String, Object> properties = new HashMap<>();
        properties.put("ip", "0.0.0.0");
        properties.put("port", 2181);
        properties.put(REGISTRY_CENTER_SAVE_CACHE_KEY, true);
        properties.put(REGISTRY_CENTER_SYNCED_SAVE_CACHE_KEY, true);
        properties.put(REGISTRY_CENTER_CACHE_FILE_PATH_KEY, "/tmp/cache");
        PluginConfig pluginConfig = new PluginConfig("zookeeper", AbstractRegistryCenter.class,
                properties);
        return pluginConfig;
    }

}
