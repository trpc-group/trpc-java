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
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Test class for registry cache.
 */
public class RegistryCenterCacheTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(RegistryCenterCacheTest.class);

    @TempDir
    Path tempDir;

    @Test
    public void testNormal() {
        try {
            String cachePath = tempDir.resolve("cache").toString();
            RegistryCenterConfig registryCenterConfig = new RegistryCenterConfig(initPluginConfig(cachePath));
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
            String cachePath = tempDir.resolve("cache").toString();
            RegistryCenterConfig registryCenterConfig = new RegistryCenterConfig(initPluginConfig(cachePath));
            RegistryCenterCache cache = new RegistryCenterCache(registryCenterConfig);

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

    private PluginConfig initPluginConfig(String cachePath) {
        Map<String, Object> properties = new HashMap<>();
        properties.put("ip", "0.0.0.0");
        properties.put("port", 2181);
        properties.put(REGISTRY_CENTER_SAVE_CACHE_KEY, true);
        properties.put(REGISTRY_CENTER_SYNCED_SAVE_CACHE_KEY, true);
        properties.put(REGISTRY_CENTER_CACHE_FILE_PATH_KEY, cachePath);
        PluginConfig pluginConfig = new PluginConfig("zookeeper", AbstractRegistryCenter.class,
                properties);
        return pluginConfig;
    }

}
