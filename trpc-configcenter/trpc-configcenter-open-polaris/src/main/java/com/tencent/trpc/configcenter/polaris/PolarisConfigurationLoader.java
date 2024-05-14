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
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

public class PolarisConfigurationLoader implements ConfigurationLoader, PluginConfigAware, InitializingExtension,
        DisposableExtension {

    private static final String[] SUFFIX_YAML = new String[]{".yaml", ".yml"};

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
        AtomicReference<String> targetValue = new AtomicReference<>();
        iterateAllFiles(groupName, (saveKey, value) -> {
            if (!Objects.equals(saveKey, key)) {
                return true;
            }
            return targetValue.compareAndSet(null, value);
        });
        return null;
    }

    @Override
    public Map<String, String> getAllValue(String groupName) throws ConfigCenterException {
        Map<String, String> result = new HashMap<>();
        iterateAllFiles(groupName, (key, value) -> {
            result.put(key, value);
            return true;
        });
        return result;
    }

    private void iterateAllFiles(String group, BiFunction<String, String, Boolean> consumer) {
        List<String> filenames = new ArrayList<>();
        polarisConfig.getConfigs()
                .stream()
                .filter(config -> Objects.equals(group, config.getGroup()))
                .forEach(config -> config.getFilenames()
                        .stream()
                        .filter(s -> isProperties(s) || isYaml(s))
                        .forEach(filenames::add));

        if (CollectionUtils.isEmpty(filenames)) {
            return;
        }
        // according to the order of files set by the user, which file is above, the value of that file shall prevail
        Collections.reverse(filenames);
        for (String name : filenames) {
            ConfigKVFile kvFile = null;
            if (isYaml(name)) {
                kvFile = fetcher.getConfigYamlFile(polarisConfig.getNamespace(), group, name);
            }
            if (isProperties(name)) {
                kvFile = fetcher.getConfigPropertiesFile(polarisConfig.getNamespace(), group, name);
            }
            if (kvFile == null) {
                continue;
            }
            for (String k : kvFile.getPropertyNames()) {
                String v = kvFile.getProperty(k, null);
                if (Objects.nonNull(v)) {
                    boolean isContinue = consumer.apply(k, v);
                    if (!isContinue) {
                        return;
                    }
                }
            }
        }
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
                        if (kvFile == null) {
                            return;
                        }
                        kvFile.addChangeListener((ConfigKVFileChangeListener) event -> {
                            for (String changeKey : event.changedKeys()) {
                                String newVal = String.valueOf(event.getPropertyNewValue(changeKey));
                                ConfigurationEvent<String, String> kvEvent = new ConfigurationEvent<>(group, changeKey, newVal,
                                        event.getPropertiesChangeType(changeKey).name());
                                listeners.forEach(configurationListener -> configurationListener.onReload(kvEvent));
                            }
                        });
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
        for (String suffix : SUFFIX_YAML) {
            if (n.endsWith(suffix)) {
                return true;
            }
        }
        return false;
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
