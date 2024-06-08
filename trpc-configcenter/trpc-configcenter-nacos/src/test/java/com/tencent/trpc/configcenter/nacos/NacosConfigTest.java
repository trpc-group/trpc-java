/*
 *
 *  * Tencent is pleased to support the open source community by making tRPC available.
 *  *
 *  * Copyright (C) 2023 THL A29 Limited, a Tencent company.
 *  * All rights reserved.
 *  *
 *  * If you have downloaded a copy of the tRPC source code from Tencent,
 *  * please note that tRPC source code is licensed under the Apache 2.0 License,
 *  * A copy of the Apache 2.0 License can be found in the LICENSE file.
 *
 */

package com.tencent.trpc.configcenter.nacos;

import com.google.common.collect.Maps;
import com.tencent.trpc.core.common.config.PluginConfig;
import org.junit.Test;

import java.util.Map;

public class NacosConfigTest {

    @Test
    public void testSetMethod() {
        Map<String, Object> extMap = Maps.newHashMap();

        PluginConfig pluginConfig = new PluginConfig(NacosConfigurationLoader.NAME, NacosConfigurationLoader.class,
                extMap);
        NacosConfig config = new NacosConfig(pluginConfig);
        config.setNamespace("test");
        config.setPassword("test");
        config.setFileExtension("test");
        config.setUsername("test");
        config.setNamespace("test");
    }

}