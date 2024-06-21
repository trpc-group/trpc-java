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

import com.tencent.polaris.configuration.api.core.ChangeType;
import com.tencent.polaris.configuration.api.core.ConfigFileChangeListener;
import com.tencent.polaris.configuration.api.core.ConfigFileService;
import com.tencent.polaris.configuration.api.core.ConfigKVFile;
import com.tencent.polaris.configuration.api.core.ConfigKVFileChangeEvent;
import com.tencent.polaris.configuration.api.core.ConfigKVFileChangeListener;
import com.tencent.polaris.configuration.api.core.ConfigPropertyChangeInfo;
import com.tencent.trpc.core.common.config.PluginConfig;
import com.tencent.trpc.core.configcenter.ConfigurationEvent;
import com.tencent.trpc.core.configcenter.ConfigurationListener;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

public class PolarisConfigurationLoaderTest {

    private PolarisConfigurationLoader configurationLoader;

    private final String MOCK_GROUP = "mock_group";

    private final String MOCK_FILE = "mock_file.properties";

    private final String USER_NAME = "user.name";

    private PolarisConfigurationLoader mockLoader() {
        PolarisConfigurationLoader loader = new PolarisConfigurationLoader();
        loader.setPolarisConfig(mockPluginConfig());

        ConfigFileService fetcher = Mockito.mock(ConfigFileService.class);
        loader.setFetcher(fetcher);

        return loader;
    }

    private PolarisConfig mockPluginConfig() {
        Map<String, Object> group = new HashMap<>();
        group.put(PolarisConfig.POLARIS_GROUP_KEY, MOCK_GROUP);
        List<String> files = new ArrayList<>();
        files.add(MOCK_FILE);
        group.put(PolarisConfig.POLARIS_FILENAMES_KEY, files);
        List<Map<String, Object>> configs = new ArrayList<>();
        configs.add(group);

        Map<String, Object> params = new HashMap<>();
        params.put(PolarisConfig.POLARIS_NAMESPACE_KEY, "default");
        params.put(PolarisConfig.POLARIS_TIMEOUT_KEY, 5000);
        params.put(PolarisConfig.POLARIS_TOKEN_KEY, "123");
        params.put(PolarisConfig.POLARIS_CONFIGS_KEY, configs);
        params.put(PolarisConfig.POLARIS_SERVER_ADDR_KEY, Collections.singletonList("127.0.0.1:8093"));

        return new PolarisConfig(new PluginConfig("", PolarisConfigurationLoader.class, params));
    }

    @Test
    public void testGetValue() {
        PolarisConfigurationLoader loader = mockLoader();

        ConfigKVFile mockKv = Mockito.mock(ConfigKVFile.class);
        Mockito.when(mockKv.getPropertyNames()).thenReturn(new HashSet<String>() {
            {
                add(USER_NAME);
            }
        });
        Mockito.when(mockKv.getProperty(USER_NAME, null)).thenReturn("polaris");

        ConfigFileService fetcher = loader.getFetcher();
        Mockito.when(fetcher.getConfigPropertiesFile("default", MOCK_GROUP, MOCK_FILE)).thenReturn(mockKv);

        String ret = loader.getValue(USER_NAME, MOCK_GROUP);
        Assert.assertEquals("polaris", ret);

        ret = loader.getValue(USER_NAME, MOCK_GROUP + "123");
        Assert.assertEquals(null, ret);
    }

    @Test
    public void testGetAllValues() {
        PolarisConfigurationLoader loader = mockLoader();

        ConfigKVFile mockKv = Mockito.mock(ConfigKVFile.class);
        Mockito.when(mockKv.getPropertyNames()).thenReturn(Collections.singleton("user.name"));
        Mockito.when(mockKv.getProperty("user.name", null)).thenReturn("polaris");

        ConfigFileService fetcher = loader.getFetcher();
        Mockito.when(fetcher.getConfigPropertiesFile("default", MOCK_GROUP, MOCK_FILE)).thenReturn(mockKv);

        Map<String, String> values = loader.getAllValue(MOCK_GROUP);
        Assert.assertEquals("polaris", values.get("user.name"));
    }

    @Test
    public void testAddListener() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<ConfigurationEvent> ref = new AtomicReference<>();

        ConfigurationListener listener = event -> {
            ref.set(event);
            latch.countDown();
        };

        PolarisConfigurationLoader loader = mockLoader();

        Map<String, ConfigPropertyChangeInfo> changeInfos = new HashMap<>();
        changeInfos.put("user.name", new ConfigPropertyChangeInfo(
                "user.name", "", "polaris", ChangeType.ADDED));
        ConfigKVFileChangeEvent changeEvent = new ConfigKVFileChangeEvent(changeInfos);

        ConfigFileService fetcher = loader.getFetcher();
        Mockito.when(fetcher.getConfigPropertiesFile("default", MOCK_GROUP, MOCK_FILE))
                .thenReturn(new MockConfigKVFile(changeEvent));

        loader.addListener(listener);
        latch.await(1, TimeUnit.SECONDS);

        Assert.assertNotNull(ref.get());
        ConfigurationEvent event = ref.get();
        Assert.assertEquals(event.getKey(), "user.name");
        Assert.assertEquals(event.getValue(), "polaris");
        Assert.assertEquals(event.getType(), ChangeType.ADDED.name());
    }

    private static class MockConfigKVFile implements ConfigKVFile {

        private final ConfigKVFileChangeEvent event;

        private MockConfigKVFile(ConfigKVFileChangeEvent event) {
            this.event = event;
        }

        @Override
        public String getProperty(String key, String defaultValue) {
            return null;
        }

        @Override
        public Integer getIntProperty(String key, Integer defaultValue) {
            return null;
        }

        @Override
        public Long getLongProperty(String key, Long defaultValue) {
            return null;
        }

        @Override
        public Short getShortProperty(String key, Short defaultValue) {
            return null;
        }

        @Override
        public Float getFloatProperty(String key, Float defaultValue) {
            return null;
        }

        @Override
        public Double getDoubleProperty(String key, Double defaultValue) {
            return null;
        }

        @Override
        public Byte getByteProperty(String key, Byte defaultValue) {
            return null;
        }

        @Override
        public Boolean getBooleanProperty(String key, Boolean defaultValue) {
            return null;
        }

        @Override
        public String[] getArrayProperty(String key, String delimiter, String[] defaultValue) {
            return new String[0];
        }

        @Override
        public <T extends Enum<T>> T getEnumProperty(String key, Class<T> enumType, T defaultValue) {
            return null;
        }

        @Override
        public <T> T getJsonProperty(String key, Class<T> clazz, T defaultValue) {
            return null;
        }

        @Override
        public <T> T getJsonProperty(String key, Type typeOfT, T defaultValue) {
            return null;
        }

        @Override
        public Set<String> getPropertyNames() {
            return null;
        }

        @Override
        public String getContent() {
            return null;
        }

        @Override
        public <T> T asJson(Class<T> objectType, T defaultValue) {
            return null;
        }

        @Override
        public <T> T asJson(Type typeOfT, T defaultValue) {
            return null;
        }

        @Override
        public boolean hasContent() {
            return false;
        }

        @Override
        public String getMd5() {
            return null;
        }

        @Override
        public void addChangeListener(ConfigKVFileChangeListener listener) {
            listener.onChange(event);
        }

        @Override
        public void addChangeListener(ConfigFileChangeListener listener) {
        }

        @Override
        public void removeChangeListener(ConfigFileChangeListener listener) {

        }

        @Override
        public void removeChangeListener(ConfigKVFileChangeListener listener) {

        }

        @Override
        public String getNamespace() {
            return null;
        }

        @Override
        public String getFileGroup() {
            return null;
        }

        @Override
        public String getFileName() {
            return null;
        }
    }

}