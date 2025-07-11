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

import com.google.common.base.Preconditions;
import com.tencent.trpc.core.common.config.PluginConfig;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.apache.commons.collections4.MapUtils;

public class NacosConfig {

    public static final String NACOS_NAMESPACE_KEY = "namespace";

    public static final String NACOS_SERVER_ADDR_KEY = "serverAddr";

    public static final String NACOS_GROUP_KEY = "group";

    public static final String NACOS_DATA_ID_KEY = "dataId";

    public static final String NACOS_CONFIGS_KEY = "configs";
    /**
     * configuration file format, only support yaml or properties
     */
    public static final String NACOS_FILE_EXTENSION_KEY = "fileExtension";

    public static final String NACOS_TIMEOUT_KEY = "timeout";

    public static final String NACOS_USERNAME_KEY = "username";

    public static final String NACOS_PASSWORD_KEY = "password";
    /**
     * default read configuration timeout
     */
    private static final Long DEFAULT_READ_TIMEOUT = 3000L;

    private String namespace;

    private String serverAddr;
    /**
     * configuration file format, only support yaml or properties
     */
    private String fileExtension;

    private List<Config> configs;

    private Long timeout;

    private String username;

    private String password;

    public NacosConfig(PluginConfig pluginConfig) {
        Preconditions.checkArgument(Objects.nonNull(pluginConfig), "Nacos plugin config can't be null");
        Map<String, Object> nacosProperties = pluginConfig.getProperties();
        namespace = MapUtils.getString(nacosProperties, NACOS_NAMESPACE_KEY);
        serverAddr = MapUtils.getString(nacosProperties, NACOS_SERVER_ADDR_KEY);
        Object configList = MapUtils.getObject(nacosProperties, NACOS_CONFIGS_KEY);
        Preconditions.checkArgument(configList instanceof List,
                "Nacos plugin config, wrong value type for key [nacos.configs], expected: List<Map>");
        configs = ((List<?>) configList).stream().map(this::buildConfig).collect(Collectors.toList());
        fileExtension = MapUtils.getString(nacosProperties, NACOS_FILE_EXTENSION_KEY);
        timeout = MapUtils.getLong(nacosProperties, NACOS_TIMEOUT_KEY, DEFAULT_READ_TIMEOUT);
        username = MapUtils.getString(nacosProperties, NACOS_USERNAME_KEY);
        password = MapUtils.getString(nacosProperties, NACOS_PASSWORD_KEY);
    }

    @SuppressWarnings("unchecked")
    private Config buildConfig(Object config) {
        Preconditions.checkArgument(config instanceof Map,
                "Nacos plugin config, wrong value type of element in [nacos.configs], expected: Map");
        Map<String, String> configMap = (Map<String, String>) config;
        return new Config(configMap.get(NACOS_GROUP_KEY), configMap.get(NACOS_DATA_ID_KEY));
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getServerAddr() {
        return serverAddr;
    }

    public void setServerAddr(String serverAddr) {
        this.serverAddr = serverAddr;
    }

    public String getFileExtension() {
        return fileExtension;
    }

    public void setFileExtension(String fileExtension) {
        this.fileExtension = fileExtension;
    }

    public List<Config> getConfigs() {
        return configs;
    }

    public void setConfigs(List<Config> configs) {
        this.configs = configs;
    }

    public Long getTimeout() {
        return timeout;
    }

    public void setTimeout(Long timeout) {
        this.timeout = timeout;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public static class Config {

        /**
         * configuration group
         */
        private String group;
        /**
         * configuration dataId
         */
        private String dataId;

        public Config(String group, String dataId) {
            this.group = group;
            this.dataId = dataId;
        }

        public String getGroup() {
            return group;
        }

        public void setGroup(String group) {
            this.group = group;
        }

        public String getDataId() {
            return dataId;
        }

        public void setDataId(String dataId) {
            this.dataId = dataId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Config config = (Config) o;
            return group.equals(config.group) && dataId.equals(config.dataId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(group, dataId);
        }
    }
}
