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

package com.tencent.trpc.registry.nacos.config;

import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import com.alibaba.nacos.api.PropertyKeyConst;
import com.tencent.trpc.core.common.config.PluginConfig;
import com.tencent.trpc.registry.nacos.NacosRegistryCenter;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;
import static com.tencent.trpc.registry.nacos.util.StringConstantFieldValuePredicateUtils.of;

public class NacosRegistryCenterConfigTest {

    @Test(expected = IllegalArgumentException.class)
    public void getParametersParamException() {
        PluginConfig pluginConfig = new PluginConfig("nacos",
                NacosRegistryCenter.class, (Map<String, Object>) null);
        NacosRegistryCenterConfig config = new NacosRegistryCenterConfig(pluginConfig);

        Map<String, Object> parameters = config.getParameters();
        Assert.assertNull(parameters);
    }

    @Test
    public void getParameters() {
        Map<String, Object> properties = new HashMap<>();
        properties.put("address", "127.0.0.1:8080");
        PluginConfig pluginConfig = new PluginConfig("nacos", NacosRegistryCenter.class, properties);
        NacosRegistryCenterConfig config = new NacosRegistryCenterConfig(pluginConfig);

        Map<String, Object> parameters = config.getParameters();
        assertNotNull(parameters);
    }

    @Test
    public void testGetParameters() {
        Map<String, Object> properties = new HashMap<>();
        properties.put("username", "testUserName");
        PluginConfig pluginConfig = new PluginConfig("nacos", NacosRegistryCenter.class, properties);
        NacosRegistryCenterConfig config = new NacosRegistryCenterConfig(pluginConfig);

        Map<String, String> parameters = config.getParameters(of(PropertyKeyConst.class));
        assertEquals("testUserName", parameters.get("username"));
        assertNull(parameters.get("namespace"));
    }
}