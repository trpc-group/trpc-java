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

import com.tencent.trpc.container.config.YamlUtils;
import com.tencent.trpc.core.common.config.ClientConfig;
import com.tencent.trpc.core.common.config.constant.ConfigConstants;
import com.tencent.trpc.core.utils.BinderUtils;
import java.util.Map;
import org.apache.commons.collections4.MapUtils;

/**
 * Client configuration parser.
 */
public class ClientConfigParser {

    /**
     * Parse client configuration.
     *
     * @param yamlMapConfig client yaml configuration
     * @return ClientConfig
     */
    @SuppressWarnings("unchecked")
    public static ClientConfig parseClientConfig(Map<String, Object> yamlMapConfig) {
        ClientConfig config = new ClientConfig();
        if (MapUtils.isEmpty(yamlMapConfig)) {
            return config;
        }
        YamlUtils yamlUtils = new YamlUtils("Label[client]");
        // Parse properties
        BinderUtils.bind(config, yamlMapConfig);
        // Parse backendConfig
        config.setBackendConfigMap(BackendConfigParser.parseConfigMap(yamlUtils.getList(yamlMapConfig,
                ConfigConstants.SERVICE)));
        return config;
    }

}
