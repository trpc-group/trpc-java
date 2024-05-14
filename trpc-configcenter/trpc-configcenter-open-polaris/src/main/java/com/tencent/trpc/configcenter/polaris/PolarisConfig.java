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

import com.google.common.base.Preconditions;
import com.tencent.trpc.core.common.config.PluginConfig;
import org.apache.commons.collections4.MapUtils;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class PolarisConfig {

    public static final String POLARIS_NAMESPACE_KEY = "namespace";

    public static final String POLARIS_SERVER_ADDR_KEY = "serverAddrs";

    public static final String POLARIS_CONFIGS_KEY = "configs";

    public static final String POLARIS_TIMEOUT_KEY = "timeout";

    public static final String POLARIS_TOKEN_KEY = "token";

    public static final String POLARIS_GROUP_KEY = "group";

    public static final String POLARIS_FILENAMES_KEY = "files";

    /**
     * default read configuration timeout
     */
    private static final Long DEFAULT_READ_TIMEOUT = 3000L;

    private String namespace;

    private List<String> serverAddrs;

    private Long timeout;

    private String token;

    private List<Config> configs;

    @SuppressWarnings("unchecked")
    PolarisConfig(PluginConfig pluginConfig) {
        Preconditions.checkArgument(Objects.nonNull(pluginConfig), "Polaris plugin config can't be null");
        Map<String, Object> polarisProperties = pluginConfig.getProperties();

        namespace = MapUtils.getString(polarisProperties, POLARIS_NAMESPACE_KEY);
        serverAddrs = (List<String>) MapUtils.getObject(polarisProperties, POLARIS_SERVER_ADDR_KEY);
        Object configList = MapUtils.getObject(polarisProperties, POLARIS_CONFIGS_KEY);
        Preconditions.checkArgument(configList instanceof List,
                "Polaris plugin config, wrong value type for key [polaris.configs], expected: List<Map>");
        configs = ((List<?>) configList).stream().map(this::buildConfig).collect(Collectors.toList());
        timeout = MapUtils.getLong(polarisProperties, POLARIS_TIMEOUT_KEY, DEFAULT_READ_TIMEOUT);
        token = MapUtils.getString(polarisProperties, POLARIS_TOKEN_KEY);
    }

    @SuppressWarnings("unchecked")
    private Config buildConfig(Object config) {
        Preconditions.checkArgument(config instanceof Map,
                "Polaris plugin config, wrong value type of element in [polaris.configs], expected: Map");
        Map<String, Object> configMap = (Map<String, Object>) config;
        String group = (String) configMap.get(POLARIS_GROUP_KEY);
        List<String> names = (List<String>) configMap.get(POLARIS_FILENAMES_KEY);
        return new Config(group, names);
    }

    public String getNamespace() {
        return namespace;
    }

    public List<String> getServerAddrs() {
        return serverAddrs;
    }

    public Long getTimeout() {
        return timeout;
    }

    public String getToken() {
        return token;
    }

    public List<Config> getConfigs() {
        return configs;
    }

    public static class Config {

        /**
         * configuration group
         */
        private String group;
        /**
         * configuration dataId
         */
        private List<String> filenames;

        public Config(String group, List<String> filenames) {
            this.group = group;
            this.filenames = filenames;
        }

        public String getGroup() {
            return group;
        }

        public void setGroup(String group) {
            this.group = group;
        }

        public List<String> getFilenames() {
            return filenames;
        }

        public void setFilename(List<String> filenames) {
            this.filenames = filenames;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Config config = (Config) o;
            return Objects.equals(group, config.group) && Objects.equals(filenames, config.filenames);
        }

        @Override
        public int hashCode() {
            return Objects.hash(group, filenames);
        }
    }
}
