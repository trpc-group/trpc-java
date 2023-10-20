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

package com.tencent.trpc.registry.nacos;

import com.alibaba.nacos.api.naming.NamingService;
import com.tencent.trpc.core.common.config.PluginConfig;
import com.tencent.trpc.core.exception.TRpcExtensionException;
import com.tencent.trpc.core.registry.RegisterInfo;
import com.tencent.trpc.registry.center.NotifyListener;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.mock;

public class NacosRegistryCenterTest {

    @Test
    public void setPluginConfig() {
        Map<String, Object> properties = new HashMap<>();
        properties.put("username", "testUserName");
        PluginConfig pluginConfig = new PluginConfig("nacos", NacosRegistryCenter.class, properties);

        NacosRegistryCenter nacosRegistryCenter = new NacosRegistryCenter();
        nacosRegistryCenter.setPluginConfig(pluginConfig);
    }

    @Test(expected = IllegalStateException.class)
    public void init() {
        Map<String, Object> properties = new HashMap<>();
        properties.put("username", "testUserName");
        PluginConfig pluginConfig = new PluginConfig("nacos", NacosRegistryCenter.class, properties);

        NacosRegistryCenter nacosRegistryCenter = new NacosRegistryCenter();
        nacosRegistryCenter.setPluginConfig(pluginConfig);
        nacosRegistryCenter.init();
    }

    @Test
    public void doRegister() {
        NamingService namingService = mock(NamingService.class);
        NacosRegistryCenter nacosRegistryCenter = new NacosRegistryCenter();
        nacosRegistryCenter.setNamingService(namingService);

        RegisterInfo registerInfo = new RegisterInfo();
        nacosRegistryCenter.doRegister(registerInfo);
    }

    @Test
    public void doUnregister() {
        NamingService namingService = mock(NamingService.class);
        NacosRegistryCenter nacosRegistryCenter = new NacosRegistryCenter();
        nacosRegistryCenter.setNamingService(namingService);

        RegisterInfo registerInfo = new RegisterInfo();
        nacosRegistryCenter.doUnregister(registerInfo);
    }

    @Test
    public void doSubscribe() {
        Map<String, Object> properties = new HashMap<>();
        properties.put("username", "testUserName");
        PluginConfig pluginConfig = new PluginConfig("nacos", NacosRegistryCenter.class, properties);

        NacosRegistryCenter nacosRegistryCenter = new NacosRegistryCenter();
        nacosRegistryCenter.setPluginConfig(pluginConfig);

        NamingService namingService = mock(NamingService.class);
        nacosRegistryCenter.setNamingService(namingService);
        NotifyListener notifyListener = new NotifyListener() {
            @Override
            public void notify(List<RegisterInfo> registerInfos) {

            }

            @Override
            public void destroy() throws TRpcExtensionException {

            }
        };

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("username", "testUserName");
        RegisterInfo registerInfo = new RegisterInfo("trpc", "127.0.0.1", 8844, "testService", "defaultGroup",
                "1.0.0", parameters);
        nacosRegistryCenter.doSubscribe(registerInfo, notifyListener);
    }

    @Test
    public void isAvailable() {
        NacosRegistryCenter nacosRegistryCenter = new NacosRegistryCenter();

        NamingService namingService = mock(NamingService.class);
        nacosRegistryCenter.setNamingService(namingService);

        nacosRegistryCenter.isAvailable();
    }

}