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

package com.tencent.trpc.configcenter.polaris;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import com.tencent.trpc.configcenter.polaris.PolarisConfig.Config;
import com.tencent.trpc.core.common.config.PluginConfig;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;

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
        params.put(PolarisConfig.POLARIS_TOKEN_KEY, "123");
        params.put(PolarisConfig.POLARIS_CONFIGS_KEY, configs);
        params.put(PolarisConfig.POLARIS_SERVER_ADDR_KEY, Collections.singletonList("127.0.0.1:8093"));

        PluginConfig pluginConfig = new PluginConfig("", PolarisConfigurationLoader.class, params);
        PolarisConfig config = new PolarisConfig(pluginConfig);

        assertEquals(Long.valueOf(5000L), config.getTimeout());
        assertEquals("123", config.getToken());
        assertEquals("default", config.getNamespace());
        assertEquals(Collections.singletonList("127.0.0.1:8093"), config.getServerAddrs());
        assertEquals(3, config.getConfigs().size());

        int index = 0;
        for (PolarisConfig.Config subConfig : config.getConfigs()) {
            assertEquals(subConfig.getGroup(), "group-" + index);
            assertEquals(subConfig.getFilenames().size(), 1);
            for (String filename : subConfig.getFilenames()) {
                assertEquals(filename, "file-" + index);
            }
            index++;
        }
    }

    @Test
    public void testConfig() {
        String group = "testGroup";
        List<String> filenames = Arrays.asList("file1", "file2");

        Config config1 = new Config(group, filenames);

        // Test getters
        assertEquals(group, config1.getGroup());
        assertEquals(filenames, config1.getFilenames());

        // Test setters
        String newGroup = "newGroup";
        List<String> newFilenames = Arrays.asList("file3", "file4");
        config1.setGroup(newGroup);
        config1.setFilename(newFilenames);
        assertEquals(newGroup, config1.getGroup());
        assertEquals(newFilenames, config1.getFilenames());

        // Test equals and hashCode
        assertEquals(config1, config1);
        Config config2 = new Config(group, filenames);
        assertNotEquals(config1, config2);
        assertNotEquals(config1.hashCode(), config2.hashCode());
    }

}