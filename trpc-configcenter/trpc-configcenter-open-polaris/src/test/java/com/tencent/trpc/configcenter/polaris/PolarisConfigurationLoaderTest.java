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

package com.tencent.trpc.configcenter.polaris;

import com.tencent.polaris.configuration.api.core.ConfigFileService;
import com.tencent.polaris.configuration.api.core.ConfigKVFile;
import com.tencent.polaris.configuration.client.internal.ConfigYamlFile;
import com.tencent.trpc.core.common.config.PluginConfig;
import com.tencent.trpc.core.configcenter.ConfigurationEvent;
import com.tencent.trpc.core.configcenter.ConfigurationListener;
import com.tencent.trpc.core.exception.ConfigCenterException;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PolarisConfigurationLoaderTest {

    private PolarisConfigurationLoader configurationLoader;

    private final String mockGroup = "mock_group";

    private final String mockFile = "mock_file.properties";

    private PolarisConfigurationLoader mockLoader() {
        PolarisConfigurationLoader loader = new PolarisConfigurationLoader();
        loader.setPolarisConfig(mockPluginConfig());

        ConfigFileService fetcher = Mockito.mock(ConfigFileService.class);
        loader.setFetcher(fetcher);

        return loader;
    }

    private PolarisConfig mockPluginConfig() {
        Map<String, Object> params = new HashMap<>();
        List<Map<String, Object>> configs = new ArrayList<>();
        Map<String, Object> group = new HashMap<>();
        group.put(PolarisConfig.POLARIS_GROUP_KEY, mockGroup);
        List<String> files = new ArrayList<>();
        files.add(mockFile);
        group.put(PolarisConfig.POLARIS_FILENAMES_KEY, files);
        configs.add(group);

        params.put(PolarisConfig.POLARIS_NAMESPACE_KEY, "default");
        params.put(PolarisConfig.POLARIS_TIMEOUT_KEY, 5000);
        params.put(PolarisConfig.POLARIS_TOKen_KEY, "123");
        params.put(PolarisConfig.POLARIS_CONFIGS_KEY, configs);
        params.put(PolarisConfig.POLARIS_SERVER_ADDR_KEY, Collections.singletonList("127.0.0.1:8093"));

        return new PolarisConfig(new PluginConfig("", PolarisConfigurationLoader.class, params));
    }

    @Test
    public void testGetValue() {
        PolarisConfigurationLoader loader = mockLoader();

        ConfigKVFile mockKv = Mockito.mock(ConfigKVFile.class);
        Mockito.when(mockKv.getProperty("user.name", null)).thenReturn("polaris");

        ConfigFileService fetcher = loader.getFetcher();
        Mockito.when(fetcher.getConfigPropertiesFile("default", mockGroup, mockFile)).thenReturn(mockKv);

        String ret = loader.getValue("user.name", mockGroup);
        Assert.assertEquals("polaris", ret);


        ret = loader.getValue("user.name", mockGroup + "123");
        Assert.assertEquals(null, ret);
    }

    @Test
    public void testGetAllValues() {
        PolarisConfigurationLoader loader = mockLoader();

        ConfigKVFile mockKv = Mockito.mock(ConfigKVFile.class);
        Mockito.when(mockKv.getPropertyNames()).thenReturn(Collections.singleton("user.name"));
        Mockito.when(mockKv.getProperty("user.name", null)).thenReturn("polaris");

        ConfigFileService fetcher = loader.getFetcher();
        Mockito.when(fetcher.getConfigPropertiesFile("default", mockGroup, mockFile)).thenReturn(mockKv);

        Map<String, String> values = loader.getAllValue(mockGroup);
        Assert.assertEquals("polaris", values.get("user.name"));
    }

    @Test
    public void testAddListener() {
        ConfigurationListener listener = new ConfigurationListener() {
            @Override
            public void onChange(ConfigurationEvent event) throws ConfigCenterException {

            }
        };

        PolarisConfigurationLoader loader = mockLoader();


        loader.addListener(listener);
    }

}