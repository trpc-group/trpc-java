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

package com.tencent.trpc.container.config.yaml;

import com.google.common.collect.Lists;
import com.tencent.trpc.container.config.YamlUtils;
import com.tencent.trpc.core.common.config.AdminConfig;
import com.tencent.trpc.core.common.config.PluginConfig;
import com.tencent.trpc.core.common.config.ServerConfig;
import com.tencent.trpc.core.common.config.constant.ConfigConstants;
import com.tencent.trpc.core.container.spi.ServerListener;
import com.tencent.trpc.core.utils.BinderUtils;
import com.tencent.trpc.core.utils.ClassLoaderUtils;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;

/**
 * Server configuration parser.
 */
public class ServerConfigParser {

    /**
     * Parse server configuration.
     *
     * @param yamlMapConfig yaml configuration
     * @param pluginConfigMap plugin configuration
     * @return parsed ServerConfig
     */
    public static ServerConfig parseServerConfig(Map<String, Object> yamlMapConfig,
            Map<Class<?>, Map<String, PluginConfig>> pluginConfigMap) {
        ServerConfig serverConfig = new ServerConfig();
        if (MapUtils.isEmpty(yamlMapConfig)) {
            return serverConfig;
        }
        BinderUtils.bind(serverConfig, yamlMapConfig);
        BinderUtils.bind(ConfigConstants.SERVER_LISTENERS, serverConfig, yamlMapConfig, ConfigConstants.SERVER_LISTENER,
                listeners -> parseStartedListeners(new YamlUtils("Label[server_listener]"), listeners));
        setDefaultServerListener(serverConfig);
        YamlUtils yamlUtils = new YamlUtils("Label[server]");
        serverConfig.setAdminConfig(parseAdminConfig(yamlUtils, yamlMapConfig));
        serverConfig.setServiceMap(ServiceConfigParser.parseServiceMapConfig(yamlUtils.getList(yamlMapConfig,
                ConfigConstants.SERVICE), pluginConfigMap));
        return serverConfig;
    }

    /**
     * When ServerListeners is empty, set the default ServerListener.
     *
     * @param serverConfig server configuration
     */
    private static void setDefaultServerListener(ServerConfig serverConfig) {
        if (CollectionUtils.isEmpty(serverConfig.getServerListeners())) {
            serverConfig.setServerListeners(Lists.newArrayList());
        }
    }

    /**
     * Parse admin configuration.
     *
     * @param yamlUtils yaml utility class
     * @param yamlMapConfig yaml configuration
     * @return parsed AdminConfig
     */
    protected static AdminConfig parseAdminConfig(YamlUtils yamlUtils, Map<String, Object> yamlMapConfig) {
        Map<String, Object> map = yamlUtils.getMap(yamlMapConfig, ConfigConstants.ADMIN);
        if (map.isEmpty()) {
            return null;
        }
        AdminConfig adminConfig = new AdminConfig();
        BinderUtils.bind(adminConfig, map);
        return adminConfig;
    }

    /**
     * Parse listener classes.
     * If empty, add DefaultServerListener listener by default.
     *
     * @param yamlUtils yaml utility class
     * @param yamlConfig listener node yaml configuration
     * @return list of ServerListener objects
     */
    @SuppressWarnings({"unchecked"})
    protected static List<ServerListener> parseStartedListeners(YamlUtils yamlUtils, Object yamlConfig) {
        return Optional.ofNullable(yamlConfig)
                .map(listenerConfigs -> ((Collection<Map<String, Object>>) listenerConfigs).stream()
                        .map(listenerConfig -> yamlUtils.getString(listenerConfig, ConfigConstants.LISTENER_CLASS))
                        .map(ServerConfigParser::loadServerListener).collect(Collectors.toList()))
                .orElseGet(Lists::newArrayList);
    }

    /**
     * Load the specified ServerListener and create an instance through reflection. When the specified name does not
     * exist, an exception is thrown.
     *
     * @param listenerName fully qualified name of the ServerListener
     * @return an instance of the specified listenerName's ServerListener
     */
    private static ServerListener loadServerListener(String listenerName) {
        try {
            return (ServerListener) ClassLoaderUtils.getClassLoader(ServerConfigParser.class)
                    .loadClass(listenerName).newInstance();
        } catch (Exception e) {
            throw new IllegalArgumentException("server config application listener class invalid, class name is "
                    + listenerName);
        }
    }

}
