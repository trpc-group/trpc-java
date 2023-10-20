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

import com.tencent.trpc.container.config.ApplicationConfigParser;
import com.tencent.trpc.core.common.TRpcSystemProperties;
import com.tencent.trpc.core.extension.Extension;
import com.tencent.trpc.core.logger.Logger;
import com.tencent.trpc.core.logger.LoggerFactory;
import com.tencent.trpc.core.utils.StringUtils;
import com.tencent.trpc.core.utils.YamlParser;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Map;

/**
 * Application configuration parser.
 */
@Extension("yaml")
@SuppressWarnings("unchecked")
public class YamlApplicationConfigParser implements ApplicationConfigParser {

    private static final Logger LOG = LoggerFactory.getLogger(YamlApplicationConfigParser.class);

    private static final String DEFAULT_YAML_CONFIG_FILE_NAME = "trpc_java.yaml";

    @Override
    public Map<String, Object> parseMap(String configPath) {
        // Parse from configPath
        if (!StringUtils.isEmpty(configPath)) {
            try {
                return YamlParser.parseAs(new FileInputStream(configPath), Map.class);
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
        }

        // Parse from default configPath
        String confPath = TRpcSystemProperties.getProperties(TRpcSystemProperties.CONFIG_PATH);
        try {
            if (StringUtils.isEmpty(confPath)) {
                LOG.warn("warning!!, not set properties [" + TRpcSystemProperties.CONFIG_PATH
                        + "], we will use classpath:" + DEFAULT_YAML_CONFIG_FILE_NAME + "");
                return YamlParser.parseAsFromClassPath(DEFAULT_YAML_CONFIG_FILE_NAME, Map.class);
            } else {
                return YamlParser.parseAs(new FileInputStream(confPath), Map.class);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Map<String, Object> parseMapFromClassPath(String configPath) {
        return YamlParser.parseAsFromClassPath(configPath, Map.class);
    }

}
