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

import com.tencent.trpc.core.common.config.PluginConfig;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PolarisConfigTest {

    @Test
    public void testPolarisConfig() {
        Map<String, Object> params = new HashMap<>();
        List<Map<String, Object>> configs = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            Map<String, Object> group = new HashMap<>();
            group.put(PolarisConfig.POLARIS_GROUP_KEY, "group-" + i);
            List<String> files = new ArrayList<>();
            files.add("file-" + i);
            group.put(PolarisConfig.POLARIS_FILENAMES_KEY, files);
            configs.add(group);
        }

        params.put(PolarisConfig.POLARIS_NAMESPACE_KEY, "default");
        params.put(PolarisConfig.POLARIS_TIMEOUT_KEY, 5000);
        params.put(PolarisConfig.POLARIS_TOKen_KEY, "123");
        params.put(PolarisConfig.POLARIS_CONFIGS_KEY, configs);
        params.put(PolarisConfig.POLARIS_SERVER_ADDR_KEY, Collections.singletonList("127.0.0.1:8093"));

        PluginConfig pluginConfig = new PluginConfig("", PolarisConfigurationLoader.class, params);
        PolarisConfig config = new PolarisConfig(pluginConfig);

        Assert.assertEquals(Long.valueOf(5000L), config.getTimeout());
        Assert.assertEquals("123", config.getToken());
        Assert.assertEquals("default", config.getNamespace());
        Assert.assertEquals(Collections.singletonList("127.0.0.1:8093"), config.getServerAddrs());
        Assert.assertEquals(3, config.getConfigs().size());

        int index = 0;
        for (PolarisConfig.Config subConfig : config.getConfigs()) {
            Assert.assertEquals(subConfig.getGroup(), "group-" + index);
            Assert.assertEquals(subConfig.getFilenames().size(), 1);
            for (String filename : subConfig.getFilenames()) {
                Assert.assertEquals(filename, "file-" + index);
            }
            index ++;
        }
    }

}