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

import com.tencent.polaris.api.config.Configuration;
import com.tencent.polaris.client.api.SDKContext;
import com.tencent.polaris.configuration.api.core.ConfigFileService;
import com.tencent.polaris.configuration.api.core.ConfigKVFile;
import com.tencent.polaris.configuration.api.core.ConfigKVFileChangeListener;
import com.tencent.polaris.configuration.factory.ConfigFileServiceFactory;
import com.tencent.polaris.factory.ConfigAPIFactory;
import com.tencent.polaris.factory.api.APIFactory;
import com.tencent.trpc.core.common.config.PluginConfig;
import com.tencent.trpc.core.configcenter.ConfigurationEvent;
import com.tencent.trpc.core.configcenter.ConfigurationListener;
import com.tencent.trpc.core.configcenter.spi.ConfigurationLoader;
import com.tencent.trpc.core.exception.ConfigCenterException;
import com.tencent.trpc.core.exception.TRpcExtensionException;
import com.tencent.trpc.core.extension.DisposableExtension;
import com.tencent.trpc.core.extension.InitializingExtension;
import com.tencent.trpc.core.extension.PluginConfigAware;
import com.tencent.trpc.core.utils.CollectionUtils;
import com.tencent.trpc.core.utils.ConcurrentHashSet;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class PolarisConfigurationLoader implements ConfigurationLoader, PluginConfigAware, InitializingExtension,
        DisposableExtension {

    private PolarisConfig polarisConfig;

    private PluginConfig pluginConfig;

    private SDKContext sdkContext;

    private ConfigFileService fetcher;

    private final Set<ConfigurationListener> listeners = new ConcurrentHashSet<>();

    @Override
    public void init() throws TRpcExtensionException {
        this.polarisConfig = new PolarisConfig(pluginConfig);
        Configuration sdkConfig = ConfigAPIFactory.defaultConfig();
        sdkConfig.getConfigFile().getServerConnector().setAddresses(polarisConfig.getServerAddrs());
        sdkConfig.getConfigFile().getServerConnector().setToken(polarisConfig.getToken());
        sdkContext = APIFactory.initContextByConfig(sdkConfig);
        sdkContext.init();
        fetcher = ConfigFileServiceFactory.createConfigFileService(sdkContext);
    }

    @Override
    public String getValue(String key, String groupName) throws ConfigCenterException {
        List<String> filenames = new ArrayList<>();
        polarisConfig.getConfigs()
                .stream()
                .filter(config -> Objects.equals(groupName, config.getGroup()))
                .forEach(config -> config.getFilenames()
                        .stream()
                        .filter(s -> isProperties(s) || isYaml(s))
                        .forEach(filenames::add));

        if (CollectionUtils.isEmpty(filenames)) {
            return null;
        }
        for (String name : filenames) {
            ConfigKVFile kvFile = null;
            if (isYaml(name)) {
                kvFile = fetcher.getConfigYamlFile(polarisConfig.getNamespace(), groupName, name);
            }
            if (isProperties(name)) {
                kvFile = fetcher.getConfigPropertiesFile(polarisConfig.getNamespace(), groupName, name);
            }
            if (kvFile == null) {
                continue;
            }
            String val = kvFile.getProperty(key, null);
            if (Objects.nonNull(val)) {
                return val;
            }
        }
        return null;
    }

    @Override
    public Map<String, String> getAllValue(String groupName) throws ConfigCenterException {
        List<String> filenames = new ArrayList<>();
        polarisConfig.getConfigs()
                .stream()
                .filter(config -> Objects.equals(groupName, config.getGroup()))
                .forEach(config -> config.getFilenames()
                        .stream()
                        .filter(s -> isProperties(s) || isYaml(s))
                        .forEach(filenames::add));

        if (CollectionUtils.isEmpty(filenames)) {
            return null;
        }
        Map<String, String> result = new HashMap<>();
        // 按照用户设置的文件顺序，那个文件在上面，就以那个文件的值为准
        Collections.reverse(filenames);
        for (String name : filenames) {
            ConfigKVFile kvFile = null;
            if (isYaml(name)) {
                kvFile = fetcher.getConfigYamlFile(polarisConfig.getNamespace(), groupName, name);
            }
            if (isProperties(name)) {
                kvFile = fetcher.getConfigPropertiesFile(polarisConfig.getNamespace(), groupName, name);
            }
            if (kvFile == null) {
                continue;
            }
            for (String k : kvFile.getPropertyNames()) {
                String v = kvFile.getProperty(k, null);
                if (Objects.nonNull(v)) {
                    result.put(k, v);
                }
            }
        }
        return result;
    }

    @Override
    public <T> T loadConfig(String fileName) throws ConfigCenterException {
        return null;
    }

    @Override
    public void addListener(ConfigurationListener listener) {
        if (listeners.contains(listener)) {
            return;
        }
        listeners.add(listener);
        polarisConfig.getConfigs().forEach(config -> {
            String group = config.getGroup();
            config.getFilenames()
                    .stream()
                    .filter(s -> isProperties(s) || isYaml(s))
                    .forEach(name -> {
                        ConfigKVFile kvFile = null;
                        if (isYaml(name)) {
                            kvFile = fetcher.getConfigYamlFile(polarisConfig.getNamespace(), group, name);
                        }
                        if (isProperties(name)) {
                            kvFile = fetcher.getConfigPropertiesFile(polarisConfig.getNamespace(), group, name);
                        }
                        if (Objects.nonNull(kvFile)) {
                            kvFile.addChangeListener((ConfigKVFileChangeListener) event -> {
                                for (String changeKey : event.changedKeys()) {
                                    String newVal = String.valueOf(event.getPropertyNewValue(changeKey));
                                    ConfigurationEvent<String, String> kvEvent = new ConfigurationEvent<>(group, changeKey, newVal,
                                            event.getPropertiesChangeType(changeKey).name());
                                    listeners.forEach(configurationListener -> configurationListener.onReload(kvEvent));
                                }
                            });
                        }
                    });
        });
    }

    @Override
    public void removeListener(ConfigurationListener listener) {
        listeners.remove(listener);
    }

    @Override
    public void destroy() throws TRpcExtensionException {
        sdkContext.destroy();
    }

    private boolean isYaml(String n) {
        n = n.toLowerCase();
        return n.endsWith(".yaml") || n.endsWith(".yml");
    }

    private boolean isProperties(String n) {
        n = n.toLowerCase();
        return n.endsWith(".properties");
    }

    @Override
    public void setPluginConfig(PluginConfig pluginConfig) throws TRpcExtensionException {
        this.pluginConfig = pluginConfig;
    }

    /**
     * only for unit test
     *
     * @param polarisConfig {@link PolarisConfig}
     */
    void setPolarisConfig(PolarisConfig polarisConfig) {
        this.polarisConfig = polarisConfig;
    }

    /**
     * only for uint test
     *
     * @param fetcher {@link ConfigFileService}
     */
    void setFetcher(ConfigFileService fetcher) {
        this.fetcher = fetcher;
    }

    /**
     * only for unit test
     *
     * @return {@link ConfigFileService}
     */
    ConfigFileService getFetcher() {
        return fetcher;
    }
}
