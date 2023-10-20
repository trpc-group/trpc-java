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

package com.tencent.trpc.container.config.yaml;

import com.tencent.trpc.core.common.config.BackendConfig;
import com.tencent.trpc.core.common.config.constant.ConfigConstants;
import com.tencent.trpc.core.utils.BinderUtils;
import com.tencent.trpc.core.utils.ClassLoaderUtils;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * BackendConfig parser.
 */
public class BackendConfigParser {

    /**
     * Parse the client->service label configuration in the yaml file, example is as follows:
     * <pre>
     * client:
     *    protocol: trpc
     *    service:
     *      - name: trpc.TestApp.TestServer.Greeter
     *        interface: com.tencent.trpc.container.demo.GreeterClient
     *        naming_url: ip://127.0.0.1:12345
     *      - name: trpc.TestApp.TestServer.Notconnect
     *        interface: com.tencent.trpc.container.demo.GreeterClient
     *        naming_url: ip://127.0.0.1:9999
     *        request_timeout: 2000
     * </pre>
     *
     * @param yamlConfig yaml configuration
     * @return backendConfig
     */
    public static Map<String, BackendConfig> parseConfigMap(List<Map<String, Object>> yamlConfig) {
        return yamlConfig.stream().map(BackendConfigParser::parseConfig).collect(
                Collectors.toMap(BackendConfig::getName, Function.identity()));
    }

    /**
     * Parse a single client->service label configuration in the yaml file, example is as follows:
     * <pre>
     * client:
     *    protocol: trpc
     *    service:
     *      - name: trpc.TestApp.TestServer.Notconnect
     *        interface: com.tencent.trpc.container.demo.GreeterClient
     *        naming_url: ip://127.0.0.1:9999
     *        request_timeout: 2000
     * </pre>
     *
     * @param yamlMapConfig yaml configuration
     * @return backendConfig
     */
    public static BackendConfig parseConfig(Map<String, Object> yamlMapConfig) {
        BackendConfig config = new BackendConfig();
        BinderUtils.bind(config, yamlMapConfig);
        BinderUtils.bind(ConfigConstants.SERVICE_INTERFACE, config, yamlMapConfig, ConfigConstants.INTERFACE,
                o -> loadClass(yamlMapConfig, o.toString()));
        return config;
    }

    /**
     * Load the specified object.
     *
     * @param yamlMapConfig yaml configuration
     * @param className service interface
     * @return object
     */
    private static Class<?> loadClass(Map<String, Object> yamlMapConfig, String className) {
        try {
            return ClassLoaderUtils.getClassLoader(BackendConfigParser.class).loadClass(className);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Yaml config parse exception, position(client->service), interface name is "
                    + yamlMapConfig.get(ConfigConstants.INTERFACE));
        }
    }

}
