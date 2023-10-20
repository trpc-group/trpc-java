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

package com.tencent.trpc.registry.nacos.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.pojo.Instance;
import com.tencent.trpc.core.common.config.PluginConfig;
import com.tencent.trpc.core.registry.RegisterInfo;
import com.tencent.trpc.registry.nacos.NacosRegistryCenter;
import com.tencent.trpc.registry.nacos.config.NacosRegistryCenterConfig;
import com.tencent.trpc.registry.nacos.constant.NacosConstant;

public class NacosNamingServiceUtilsTest {

    @Test
    public void convert() {
        // Instance不为空的
        Map<String, String> metadata = new HashMap<>();
        metadata.put(NacosConstant.URL_META_KEY,
                "trpc%3A%2F%2F127.0.0.1%3A12345%2Ftrpc.TestApp.TestServer."
                        + "Greeter1Naming%3Fname%3Dtrpc.TestApp.TestServer.Greeter1Naming");

        Instance instance = new Instance();
        instance.setServiceName("trpc.TestApp.TestServer.Greeter1Naming");
        instance.setMetadata(metadata);
        List<Instance> instances = new ArrayList<>();
        instances.add(instance);

        RegisterInfo registerInfo = new RegisterInfo("trpc", "127.0.0.1", 0,
                "trpc.TestApp.TestServer.Greeter1Naming");
        List<RegisterInfo> registerInfos = NacosNamingServiceUtils.convert(instances, registerInfo);

        Assert.assertNotNull(registerInfos);
        Assert.assertEquals(registerInfos.get(0).getPort(), 12345);

        // Instance为空的
        List<Instance> emptyInstances = new ArrayList<>();
        List<RegisterInfo> emptyRegisterInfos = NacosNamingServiceUtils.convert(emptyInstances, registerInfo);

        Assert.assertNotNull(emptyRegisterInfos);
        Assert.assertEquals(emptyRegisterInfos.get(0).getPort(), 0);
    }

    @Test(expected = IllegalStateException.class)
    public void createNamingService() {
        Map<String, Object> properties = new HashMap<>();
        properties.put("addresses", "127.0.0.1:8844");
        properties.put("username", "testUserName");
        PluginConfig pluginConfig = new PluginConfig("nacos", NacosRegistryCenter.class, properties);
        NacosRegistryCenterConfig config = new NacosRegistryCenterConfig(pluginConfig);

        NamingService namingService = NacosNamingServiceUtils.createNamingService(config);
        Assert.assertNull(namingService);
    }
}