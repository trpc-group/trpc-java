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

package com.tencent.trpc.configcenter.nacos;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.config.ConfigChangeEvent;
import com.alibaba.nacos.api.config.ConfigChangeItem;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.config.PropertyChangeType;
import com.alibaba.nacos.api.config.listener.Listener;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.client.config.listener.impl.AbstractConfigChangeListener;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.tencent.trpc.core.common.ConfigManager;
import com.tencent.trpc.core.common.config.PluginConfig;
import com.tencent.trpc.core.configcenter.ConfigurationListener;
import com.tencent.trpc.core.exception.ConfigCenterException;
import com.tencent.trpc.core.exception.TRpcExtensionException;
import com.tencent.trpc.core.extension.ExtensionLoader;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executor;
import org.mockito.MockedStatic;

/**
 * NacosConfigurationLoader Test
 */
public class NacosConfigurationLoaderTest {

    /**
     * Nacos namespace
     */
    private static final String NAMESPACE = "namespace";
    /**
     * Nacos server address
     */
    private static final String SERVER_ADDR = "127.0.0.1:8848";
    /**
     * default group
     */
    private static final String DEFAULT_GROUP = "DEFAULT_GROUP";
    /**
     * default dataId
     */
    private static final String DEFAULT_DATA_ID = "DEFAULT_DATA_ID";
    /**
     * app group
     */
    private static final String APP_GROUP = "APP_GROUP";
    /**
     * app dataId
     */
    private static final String APP_DATA_ID = "APP_DATA_ID";
    /**
     * format - yaml
     */
    private static final String FILE_EXTENSION_YAML = "yaml";
    /**
     * format - properties
     */
    private static final String FILE_EXTENSION_PROPERTIES = "properties";
    /**
     * read configuration timeout
     */
    private static final long TIMEOUT = 5000L;

    private static final String USERNAME = "nacos";

    private static final String PASSWORD = "nacos";
    /**
     * business configuration example - yaml
     */
    private static final String YAML_EXAMPLE =
            "example:\n"
                    + "  config:\n"
                    + "    name: example\n"
                    + "    list:\n"
                    + "      - key1: value1\n"
                    + "      - key2: value2";
    /**
     * business configuration example - properties
     */
    private static final String PROPERTIES_EXAMPLE =
            "example.config.name=example\n"
                    + "example.config.list[0].key1=value1\n"
                    + "example.config.list[1].key2=value2\n";
    /**
     * Nacos SPI, for yaml
     */
    private NacosConfigurationLoader configYaml;
    /**
     * Nacos SPI, for properties
     */
    private NacosConfigurationLoader configProperties;
    /**
     * Nacos api
     */
    private ConfigService configService;

    /**
     * nacosConfig
     */
    private NacosConfig nacosConfig;

    /**
     * setUp
     */
    @BeforeEach
    public void setUp() {
        ConfigManager.stopTest();
        ConfigManager.startTest();

        Map<String, Object> extMap = Maps.newHashMap();
        extMap.put(NacosConfig.NACOS_NAMESPACE_KEY, NAMESPACE);
        extMap.put(NacosConfig.NACOS_SERVER_ADDR_KEY, SERVER_ADDR);
        extMap.put(NacosConfig.NACOS_TIMEOUT_KEY, TIMEOUT);
        extMap.put(NacosConfig.NACOS_USERNAME_KEY, USERNAME);
        extMap.put(NacosConfig.NACOS_PASSWORD_KEY, PASSWORD);

        List<Map<String, String>> configs = Arrays.asList(
                new HashMap<String, String>() {{
                    put(NacosConfig.NACOS_GROUP_KEY, DEFAULT_GROUP);
                    put(NacosConfig.NACOS_DATA_ID_KEY, DEFAULT_DATA_ID);
                }},
                new HashMap<String, String>() {{
                    put(NacosConfig.NACOS_GROUP_KEY, APP_GROUP);
                    put(NacosConfig.NACOS_DATA_ID_KEY, APP_DATA_ID);
                }}
        );
        extMap.put(NacosConfig.NACOS_CONFIGS_KEY, configs);
        extMap.put(NacosConfig.NACOS_FILE_EXTENSION_KEY, FILE_EXTENSION_YAML);
        PluginConfig pluginConfig = new PluginConfig(NacosConfigurationLoader.NAME, NacosConfigurationLoader.class,
                extMap);

        nacosConfig = new NacosConfig(pluginConfig);

        configYaml = new NacosConfigurationLoader();
        configYaml.setPluginConfig(pluginConfig);
        try {
            configYaml.init();
        } finally {
            configService = Mockito.mock(ConfigService.class);
            configYaml.setConfigService(configService);
        }

        extMap.put(NacosConfig.NACOS_FILE_EXTENSION_KEY, FILE_EXTENSION_PROPERTIES);
        PluginConfig pluginConfig1 = new PluginConfig(NacosConfigurationLoader.NAME, NacosConfigurationLoader.class,
                extMap);
        configProperties = new NacosConfigurationLoader();
        configProperties.setPluginConfig(pluginConfig1);
        try {
            configProperties.init();
        } finally {
            configProperties.setConfigService(configService);
        }
    }

    @AfterEach
    public void setDown() {
        ConfigManager.stopTest();
        ExtensionLoader.destroyAllPlugin();
    }

    @Test
    public void testNacosConfigSetMethod() {
        nacosConfig.setNamespace(NAMESPACE);
        nacosConfig.setPassword(PASSWORD);
        nacosConfig.setFileExtension(FILE_EXTENSION_YAML);
        nacosConfig.setUsername(USERNAME);
        nacosConfig.setServerAddr(SERVER_ADDR);
        nacosConfig.setTimeout(TIMEOUT);

        NacosConfig.Config config = new NacosConfig.Config(DEFAULT_GROUP, DEFAULT_DATA_ID);
        config.setGroup(DEFAULT_GROUP);
        config.setDataId(DEFAULT_DATA_ID);
        NacosConfig.Config configOther = new NacosConfig.Config(DEFAULT_GROUP, DEFAULT_DATA_ID);
        boolean equals = config.equals(configOther);
        Assertions.assertTrue(equals);
    }

    @Test
    public void testGetValue() throws NacosException {
        String key = "example.config.name";
        String key1 = "example.config.list[0].key1";

        when(configService.getConfig(DEFAULT_DATA_ID, DEFAULT_GROUP, TIMEOUT)).thenReturn(YAML_EXAMPLE);
        Assertions.assertEquals(configYaml.getValue(key, DEFAULT_GROUP), "example");
        Assertions.assertEquals(configYaml.getValue(key1, DEFAULT_GROUP), "value1");

        String key2 = "example.config.list[1].key2";
        when(configService.getConfig(APP_DATA_ID, APP_GROUP, TIMEOUT)).thenReturn(PROPERTIES_EXAMPLE);
        Assertions.assertEquals(configProperties.getValue(key, APP_GROUP), "example");
        Assertions.assertEquals(configProperties.getValue(key2, APP_GROUP), "value2");
    }

    @Test
    public void testGetValueForException() throws NacosException {
        String key = "example.config.name";
        when(configService.getConfig(DEFAULT_DATA_ID, DEFAULT_GROUP, TIMEOUT)).thenThrow(new NacosException());
        ConfigCenterException exception = Assertions.assertThrows(ConfigCenterException.class, () -> {
            configYaml.getValue(key, DEFAULT_GROUP);
        });
        Assertions.assertTrue(exception.getMessage().contains(
                String.format("Fetch config failed from Nacos, group: %s, dataId: %s", DEFAULT_GROUP, DEFAULT_DATA_ID)));
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void testGroupDataIdCaches() throws Exception {
        String key = "example.config.name";
        when(configService.getConfig(DEFAULT_DATA_ID, DEFAULT_GROUP, TIMEOUT)).thenReturn(YAML_EXAMPLE);
        Assertions.assertEquals(configYaml.getValue(key, DEFAULT_GROUP), "example");

        Field groupDataIdToKeysField = NacosConfigurationLoader.class.getDeclaredField("groupDataIdToKeys");
        boolean groupDataIdToKeysFieldAccessible = groupDataIdToKeysField.isAccessible();
        groupDataIdToKeysField.setAccessible(true);
        Multimap groupDataIdToKeys = (Multimap) groupDataIdToKeysField.get(configYaml);
        groupDataIdToKeysField.setAccessible(groupDataIdToKeysFieldAccessible);

        Collection<String> keys = groupDataIdToKeys.get(String.format("%s_%s", DEFAULT_GROUP, DEFAULT_DATA_ID));
        Assertions.assertEquals(keys.size(), 1);
        Assertions.assertTrue(keys.contains(key));

        Field groupDataIdToNacosListenerField = NacosConfigurationLoader.class
                .getDeclaredField("groupDataIdToNacosListener");
        boolean groupDataIdToNacosListenerFieldAccessible = groupDataIdToNacosListenerField.isAccessible();
        groupDataIdToNacosListenerField.setAccessible(true);
        Map groupDataIdToNacosListener = (Map) groupDataIdToNacosListenerField.get(configYaml);
        groupDataIdToNacosListenerField.setAccessible(groupDataIdToNacosListenerFieldAccessible);

        Object listener = groupDataIdToNacosListener.get(String.format("%s_%s", DEFAULT_GROUP, DEFAULT_DATA_ID));
        Assertions.assertTrue(listener instanceof Listener);
        Assertions.assertTrue(listener instanceof AbstractConfigChangeListener);

        // Test configuration DELETED changes
        ConfigChangeItem item = new ConfigChangeItem(key, "", "");
        item.setType(PropertyChangeType.DELETED);
        ConfigChangeEvent event = new ConfigChangeEvent(ImmutableMap.of(key, item));
        ((AbstractConfigChangeListener) listener).receiveConfigChange(event);
        keys = groupDataIdToKeys.get(String.format("%s_%s", DEFAULT_GROUP, DEFAULT_DATA_ID));
        Assertions.assertEquals(keys.size(), 0);

        Executor executor = ((AbstractConfigChangeListener) listener).getExecutor();
        Assertions.assertNotNull(executor);
    }

    @Test
    public void testGetAllValue() throws NacosException {
        String key = "example.config.name";
        String key1 = "example.config.list[0].key1";

        when(configService.getConfig(DEFAULT_DATA_ID, DEFAULT_GROUP, TIMEOUT)).thenReturn(YAML_EXAMPLE);
        Map<String, String> allYamlValue = configYaml.getAllValue(DEFAULT_GROUP);
        Assertions.assertEquals(allYamlValue.get(key), "example");
        Assertions.assertEquals(allYamlValue.get(key1), "value1");

        String key2 = "example.config.list[1].key2";
        when(configService.getConfig(APP_DATA_ID, APP_GROUP, TIMEOUT)).thenReturn(PROPERTIES_EXAMPLE);
        Map<String, String> allPropsValue1 = configProperties.getAllValue(APP_GROUP);
        Assertions.assertEquals(allPropsValue1.get(key), "example");
        Assertions.assertEquals(allPropsValue1.get(key2), "value2");
    }

    @Test
    void testGetAllValueXml() {
        Assertions.assertThrows(ConfigCenterException.class, () -> {
            String xmlValue = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>";
            when(configService.getConfig(DEFAULT_DATA_ID, DEFAULT_GROUP, TIMEOUT)).thenReturn(xmlValue);
            configYaml.getAllValue(DEFAULT_GROUP);
        });
    }

    @Test
    void testGetAllValueHtml() {
        Assertions.assertThrows(ConfigCenterException.class, () -> {
            String htmlValue = "<html lang=\"en\">";
            when(configService.getConfig(DEFAULT_DATA_ID, DEFAULT_GROUP, TIMEOUT)).thenReturn(htmlValue);
            configYaml.getAllValue(DEFAULT_GROUP);
        });
    }

    @Test
    void testGetAllValueText() {
        Assertions.assertThrows(ConfigCenterException.class, () -> {
            String textValue = "Test text.";
            when(configService.getConfig(DEFAULT_DATA_ID, DEFAULT_GROUP, TIMEOUT)).thenReturn(textValue);
            configYaml.getAllValue(DEFAULT_GROUP);
        });
    }

    @Test
    void testLoadConfig() {
        Assertions.assertThrows(ConfigCenterException.class, () -> {
            configYaml.loadConfig("fileName");
        });
    }

    @Test
    public void testListener() {
        ConfigurationListener listener = Mockito.mock(ConfigurationListener.class);
        configYaml.addListener(listener);
        configYaml.removeListener(listener);
    }

    @Test
    public void testAddListenerError() {
        configService = Mockito.mock(ConfigService.class);
        try {
            doAnswer(it -> {
                throw new NacosException(1, "add listener error");
            }).when(configService).addListener(anyString(), anyString(), any());
        } catch (NacosException e) {
            throw new RuntimeException(e);
        }
        configYaml.setConfigService(configService);

        ConfigurationListener listener = Mockito.mock(ConfigurationListener.class);
        Assertions.assertThrows(ConfigCenterException.class, () -> {
            configYaml.addListener(listener);
        });
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Test
    public void testListenerCallback() throws Exception {
        String key = "example.config.name";

        ConfigurationListener listener = event -> Assertions.assertEquals(event.getKey(), key);
        configYaml.addListener(listener);

        Field configurationListenerToNacosListener = NacosConfigurationLoader.class
                .getDeclaredField("configurationListenerToNacosListener");
        boolean groupDataIdToKeysFieldAccessible = configurationListenerToNacosListener.isAccessible();
        configurationListenerToNacosListener.setAccessible(true);
        Map listenerMap = (Map) configurationListenerToNacosListener.get(configYaml);
        configurationListenerToNacosListener.setAccessible(groupDataIdToKeysFieldAccessible);
        Map<NacosConfig.Config, Listener> nacosConfigToListener =
                (Map<NacosConfig.Config, Listener>) listenerMap.get(listener);
        Listener changeListener = nacosConfigToListener.get(new NacosConfig.Config(DEFAULT_GROUP, DEFAULT_DATA_ID));
        Assertions.assertNotNull(changeListener);
        Assertions.assertTrue(changeListener instanceof AbstractConfigChangeListener);

        ConfigChangeItem item = new ConfigChangeItem(key, "", "");
        item.setType(PropertyChangeType.DELETED);
        ConfigChangeEvent event = new ConfigChangeEvent(ImmutableMap.of(key, item));
        ((AbstractConfigChangeListener) changeListener).receiveConfigChange(event);

        Executor executor = changeListener.getExecutor();
        Assertions.assertNotNull(executor);
    }

    @Test
    public void testConvertConfigurationEvent() throws Exception {
        ConfigChangeItem changeItem = new ConfigChangeItem("test", "123", "1234");
        changeItem.setType(PropertyChangeType.MODIFIED);
        java.lang.reflect.Method method = NacosConfigurationLoader.class
                .getDeclaredMethod("convertConfigurationEvent", String.class, ConfigChangeItem.class);
        method.setAccessible(true);
        method.invoke(configYaml, DEFAULT_GROUP, changeItem);
    }

    @Test
    public void testDestroy() {
        configYaml.destroy();
    }

    @Test
    public void testDestroyError() {
        try {
            doAnswer(it -> {
                throw new NacosException();
            }).when(configService).shutDown();
        } catch (NacosException e) {
            throw new RuntimeException(e);
        }
        Assertions.assertThrows(TRpcExtensionException.class, () -> {
            configYaml.destroy();
        });
    }

    @Test
    public void testInitError() {
        try (MockedStatic<NacosFactory> mockedNacosFactory = Mockito.mockStatic(NacosFactory.class)) {
            mockedNacosFactory.when(() -> NacosFactory.createConfigService(any(Properties.class)))
                    .thenThrow(new NacosException());
            Assertions.assertThrows(TRpcExtensionException.class, () -> {
                configYaml.init();
            });
        }
    }
}
