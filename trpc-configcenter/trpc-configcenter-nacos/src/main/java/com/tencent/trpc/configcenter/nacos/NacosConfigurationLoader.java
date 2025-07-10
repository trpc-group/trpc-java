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

package com.tencent.trpc.configcenter.nacos;

import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.config.ConfigChangeEvent;
import com.alibaba.nacos.api.config.ConfigChangeItem;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.config.ConfigType;
import com.alibaba.nacos.api.config.PropertyChangeType;
import com.alibaba.nacos.api.config.listener.Listener;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.client.config.listener.impl.AbstractConfigChangeListener;
import com.fasterxml.jackson.dataformat.javaprop.JavaPropsMapper;
import com.fasterxml.jackson.dataformat.javaprop.JavaPropsSchema;
import com.google.common.base.Preconditions;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.tencent.trpc.core.common.config.PluginConfig;
import com.tencent.trpc.core.configcenter.ConfigurationEvent;
import com.tencent.trpc.core.configcenter.ConfigurationListener;
import com.tencent.trpc.core.configcenter.spi.ConfigurationLoader;
import com.tencent.trpc.core.exception.ConfigCenterException;
import com.tencent.trpc.core.exception.TRpcExtensionException;
import com.tencent.trpc.core.extension.DisposableExtension;
import com.tencent.trpc.core.extension.Extension;
import com.tencent.trpc.core.extension.InitializingExtension;
import com.tencent.trpc.core.extension.PluginConfigAware;
import com.tencent.trpc.core.logger.Logger;
import com.tencent.trpc.core.logger.LoggerFactory;
import com.tencent.trpc.core.utils.YamlParser;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.yaml.snakeyaml.constructor.ConstructorException;

/**
 * Config center - Nacos.
 * Nacos only supports configuration at the granularity of a single configuration file (that is, dataId),
 * (namespace + group + dataId) uniquely determine a configuration file.
 */
@Extension(value = NacosConfigurationLoader.NAME)
public class NacosConfigurationLoader implements ConfigurationLoader, PluginConfigAware, InitializingExtension,
        DisposableExtension {

    private static final Logger logger = LoggerFactory.getLogger(NacosConfigurationLoader.class);
    /**
     * plugin name
     */
    public static final String NAME = "nacos";
    /**
     * pre-allocated capacity
     */
    private static final int DEFAULT_SINGLE_CONFIG_SIZE = 16;
    /**
     * Cache of ((group + dataId) -> keys)
     */
    private final Multimap<String, String> groupDataIdToKeys = Multimaps
            .synchronizedSetMultimap(LinkedHashMultimap.create());
    /**
     * Cache of ((group + dataId) -> Nacos Listener)
     */
    private final Map<String, Listener> groupDataIdToNacosListener = new ConcurrentHashMap<>();
    /**
     * Map ConfigurationListener to Listener
     * Expect only one listener, cause currently can only listen to all group's configuration
     */
    private final Map<ConfigurationListener, Map<NacosConfig.Config, Listener>>
            configurationListenerToNacosListener = new ConcurrentHashMap<>(1);
    /**
     * yaml to properties mapper
     */
    private final JavaPropsMapper mapper = new JavaPropsMapper();
    /**
     * properties format: first array offset is 0, wrap index with []
     */
    private final JavaPropsSchema propsSchema = JavaPropsSchema.emptySchema()
            .withFirstArrayOffset(0)
            .withWriteIndexUsingMarkers(true);

    private NacosConfig nacosConfig;
    /**
     * Nacos configService
     */
    private ConfigService configService;

    private PluginConfig pluginConfig;

    /**
     * For Test.
     *
     * @param configService Nacos configService
     */
    public void setConfigService(ConfigService configService) {
        this.configService = configService;
    }

    /**
     * Get the value of the specified key corresponding to the KV configuration group.
     *
     * @param key configuration key
     * @param groupName configuration group name
     * @return the value of the key
     * @throws ConfigCenterException ConfigCenterException
     */
    @Override
    public String getValue(String key, String groupName) throws ConfigCenterException {
        List<NacosConfig.Config> matchedConfigs = checkAndMatchGroup(groupName);
        // get (group + dataId) from the cache first by key, if present
        Optional<NacosConfig.Config> cachedDataId = matchedConfigs.stream()
                .filter(config -> {
                    String groupDataId = String.format("%s_%s", config.getGroup(), config.getDataId());
                    Collection<String> keys = groupDataIdToKeys.get(groupDataId);
                    return keys.stream().anyMatch(k -> StringUtils.equals(k, key));
                }).findFirst();
        if (cachedDataId.isPresent()) {
            NacosConfig.Config config = cachedDataId.get();
            return fetchConfig(config.getDataId(), config.getGroup(), (props) -> (String) props.get(key));
        }
        String value = null;
        for (NacosConfig.Config config : matchedConfigs) {
            String group = config.getGroup();
            String dataId = config.getDataId();
            value = fetchConfig(dataId, group, (props) -> (String) props.get(key));
            if (Objects.nonNull(value)) {
                // cache the relationship between (group + dataId) and the key,
                // and register listener for current (group + dataId), used to maintain this cache.
                cacheKeyAndRegisterRelatedListener(group, dataId, key);
                break;
            }
        }
        return value;
    }

    private void cacheKeyAndRegisterRelatedListener(String group, String dataId, String key) {
        String groupDataId = String.format("%s_%s", group, dataId);
        if (!groupDataIdToNacosListener.containsKey(groupDataId)) {
            synchronized (groupDataIdToNacosListener) {
                if (!groupDataIdToNacosListener.containsKey(groupDataId)) {
                    groupDataIdToNacosListener.put(groupDataId, registerListenerToGroupDataId(group, dataId));
                }
            }
        }
        groupDataIdToKeys.put(groupDataId, key);
    }

    private Listener registerListenerToGroupDataId(String group, String dataId) {
        AbstractConfigChangeListener changeListener = new AbstractConfigChangeListener() {
            @Override
            public Executor getExecutor() {
                return Executors.newSingleThreadExecutor();
            }

            @Override
            public void receiveConfigChange(ConfigChangeEvent event) {
                Collection<ConfigChangeItem> changeItems = event.getChangeItems();
                changeItems.stream()
                        .filter(item -> PropertyChangeType.DELETED.equals(item.getType()))
                        .forEach(item -> {
                            logger.info("[Nacos] The property [key = {}] is deleted. group: {}, dataId: {}",
                                    item.getKey(), group, dataId);
                            Collection<String> keys = groupDataIdToKeys.get(String.format("%s_%s", group, dataId));
                            Iterator<String> keyIterator = keys.iterator();
                            while (keyIterator.hasNext()) {
                                if (item.getKey().equals(keyIterator.next())) {
                                    keyIterator.remove();
                                    break;
                                }
                            }
                        });
            }
        };
        try {
            configService.addListener(dataId, group, changeListener);
        } catch (NacosException e) {
            throw new ConfigCenterException(e);
        }
        return changeListener;
    }

    /**
     * Get all KV configurations of a group.
     *
     * @param groupName configuration group name
     * @return all configurations of a group
     * @throws ConfigCenterException ConfigCenterException
     */
    @Override
    public Map<String, String> getAllValue(String groupName) throws ConfigCenterException {
        List<NacosConfig.Config> matchedConfigs = checkAndMatchGroup(groupName);
        Map<String, String> allKeyToValue = new HashMap<>(DEFAULT_SINGLE_CONFIG_SIZE * matchedConfigs.size());
        for (NacosConfig.Config config : matchedConfigs) {
            Map<String, String> singleKeyToValue = fetchConfig(config.getDataId(), config.getGroup(),
                    (props) -> props.entrySet().stream().collect(Collectors
                            .toMap(entry -> (String) entry.getKey(), entry -> (String) entry.getValue()))
            );
            if (Objects.isNull(singleKeyToValue)) {
                continue;
            }
            allKeyToValue.putAll(singleKeyToValue);
        }
        return allKeyToValue;
    }

    private List<NacosConfig.Config> checkAndMatchGroup(String groupName) {
        List<NacosConfig.Config> matchedConfigs = nacosConfig.getConfigs().stream()
                .filter(config -> Objects.equals(config.getGroup(), groupName))
                .collect(Collectors.toList());
        Preconditions.checkArgument(!matchedConfigs.isEmpty(), "The group [{}] is not configured", groupName);
        return matchedConfigs;
    }

    @SuppressWarnings("rawtypes")
    private <R> R fetchConfig(String dataId, String group, Function<Properties, R> converter) {
        String config;
        try {
            config = configService.getConfig(dataId, group, nacosConfig.getTimeout());
        } catch (NacosException e) {
            throw new ConfigCenterException(String.format(
                    "Fetch config failed from Nacos, group: %s, dataId: %s", group, dataId), e);
        }
        if (StringUtils.isBlank(config)) {
            return null;
        }

        ByteArrayInputStream inputStream = new ByteArrayInputStream(config.getBytes(StandardCharsets.UTF_8));

        if (isYamlExtension()) {
            Properties props;
            try {
                HashMap map = YamlParser.parseAs(inputStream, HashMap.class);
                props = mapper.writeValueAsProperties(map, propsSchema);
            } catch (IOException | ConstructorException e) {
                throw new ConfigCenterException(String.format(
                        "Parse [yaml] config content failed, group: %s, dataId: %s", group, dataId), e);
            }
            return converter.apply(props);
        }

        if (isPropertiesExtension()) {
            Properties props = new Properties();
            try {
                props.load(inputStream);
            } catch (IOException e) {
                throw new ConfigCenterException(String.format(
                        "Parse [properties] config content failed, group: %s, dataId: %s", group, dataId), e);
            }
            return converter.apply(props);
        }
        throw new ConfigCenterException("Unsupported file-extension, only support yaml or properties");
    }

    private boolean isYamlExtension() {
        return ConfigType.YAML.getType().equals(nacosConfig.getFileExtension());
    }

    private boolean isPropertiesExtension() {
        return ConfigType.PROPERTIES.getType().equals(nacosConfig.getFileExtension());
    }

    /**
     * Load config content of [file] type, Nacos does not support.
     *
     * @param fileName file name
     * @return file content
     * @throws ConfigCenterException ConfigCenterException
     */
    @Override
    public <T> T loadConfig(String fileName) throws ConfigCenterException {
        throw new ConfigCenterException("Nacos does not support the configuration of [file] type");
    }

    /**
     * Add a listener to the configuration.
     *
     * @param listener ConfigurationListener of plugin
     */
    @Override
    public void addListener(ConfigurationListener listener) {
        Preconditions.checkArgument(Objects.nonNull(listener), "Listener can't be null");
        Map<NacosConfig.Config, Listener> nacosConfigToListener = new HashMap<>(nacosConfig.getConfigs().size());
        for (NacosConfig.Config config : nacosConfig.getConfigs()) {
            String group = config.getGroup();
            String dataId = config.getDataId();
            Listener changeListener = getNacosConfigChangeListener(listener, group, dataId);
            try {
                configService.addListener(dataId, group, changeListener);
            } catch (NacosException e) {
                // remove registered listeners from map
                if (!nacosConfigToListener.isEmpty()) {
                    nacosConfigToListener.forEach((conf, registeredListener) ->
                            configService.removeListener(conf.getDataId(), conf.getGroup(), registeredListener));
                }
                throw new ConfigCenterException(e);
            }
            nacosConfigToListener.put(config, changeListener);
        }
        if (!nacosConfigToListener.isEmpty()) {
            configurationListenerToNacosListener.put(listener, nacosConfigToListener);
        }
    }

    private Listener getNacosConfigChangeListener(ConfigurationListener listener, String group, String dataId) {
        return new AbstractConfigChangeListener() {
            @Override
            public Executor getExecutor() {
                return Executors.newSingleThreadExecutor();
            }

            @Override
            public void receiveConfigChange(ConfigChangeEvent event) {
                Collection<ConfigChangeItem> changeItems = event.getChangeItems();
                logger.info("[Nacos] New property value received for (namespace: {}) (group: {}, dataId: {}) : {}",
                        nacosConfig.getNamespace(), group, dataId, changeItems);
                changeItems.stream()
                        .map(item -> convertConfigurationEvent(group, item))
                        .filter(Objects::nonNull)
                        .forEach(listener::onReload);
            }
        };
    }

    /**
     * Convert to configuration event.
     *
     * @param groupName configuration group name
     * @param item single config change item
     * @return ConfigurationEvent
     */
    private ConfigurationEvent<String, String> convertConfigurationEvent(String groupName, ConfigChangeItem item) {
        return Optional.ofNullable(item)
                .map(v -> {
                    String type = Objects.isNull(v.getType()) ? null : v.getType().name();
                    return new ConfigurationEvent<>(groupName, v.getKey(), v.getNewValue(), type);
                }).orElseGet(() -> null);
    }

    /**
     * Remove listener, the listener must be added before remove.
     *
     * @param listener the ConfigurationListener added before
     */
    @Override
    public void removeListener(ConfigurationListener listener) {
        Map<NacosConfig.Config, Listener> nacosConfigToListener = configurationListenerToNacosListener.get(listener);
        if (Objects.nonNull(nacosConfigToListener)) {
            nacosConfigToListener.forEach((config, changeListener) ->
                    configService.removeListener(config.getDataId(), config.getGroup(), changeListener));
            configurationListenerToNacosListener.remove(listener);
        }
    }

    /**
     * Called when plugin exits.
     *
     * @throws TRpcExtensionException TRpcExtensionException
     */
    @Override
    public void destroy() throws TRpcExtensionException {
        try {
            configService.shutDown();
        } catch (NacosException e) {
            throw new TRpcExtensionException(e);
        }
    }

    /**
     * Called when plugin is initialized, initialize nacos configuration.
     * {@code setPluginConfig} should be called before this method.
     *
     * @throws TRpcExtensionException TRpcExtensionException
     */
    @Override
    public void init() throws TRpcExtensionException {
        nacosConfig = new NacosConfig(pluginConfig);
        logger.debug("init nacos config {}", nacosConfig.toString());
        String namespace = nacosConfig.getNamespace();
        String serverAddr = nacosConfig.getServerAddr();
        Preconditions.checkArgument(StringUtils.isNotBlank(namespace),
                "Nacos plugin config key [namespace] can't be null");
        Preconditions.checkArgument(StringUtils.isNotBlank(serverAddr),
                "Nacos plugin config key [serverAddr]  can't be null");
        Properties properties = new Properties();
        properties.put(NacosConfig.NACOS_NAMESPACE_KEY, namespace);
        properties.put(NacosConfig.NACOS_SERVER_ADDR_KEY, serverAddr);
        String username = nacosConfig.getUsername();
        String password = nacosConfig.getPassword();
        if (StringUtils.isNotBlank(username) && StringUtils.isNotBlank(password)) {
            properties.put(NacosConfig.NACOS_USERNAME_KEY, username);
            properties.put(NacosConfig.NACOS_PASSWORD_KEY, password);
        }
        try {
            configService = NacosFactory.createConfigService(properties);
        } catch (NacosException e) {
            throw new TRpcExtensionException(e);
        }
    }

    /**
     * Set the plugin config to current loader, used to init Nacos config.
     *
     * @param pluginConfig plugin config
     * @throws TRpcExtensionException TRpcExtensionException
     */
    @Override
    public void setPluginConfig(PluginConfig pluginConfig) throws TRpcExtensionException {
        this.pluginConfig = pluginConfig;
    }
}
