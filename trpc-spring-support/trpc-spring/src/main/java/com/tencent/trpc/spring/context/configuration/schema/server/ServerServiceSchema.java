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

package com.tencent.trpc.spring.context.configuration.schema.server;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.tencent.trpc.spring.context.configuration.schema.AbstractProtocolSchema;
import java.util.List;
import java.util.Map;

/**
 * Configurations for tRPC server service
 *
 * @see com.tencent.trpc.core.common.config.ServiceConfig
 */
public class ServerServiceSchema extends AbstractProtocolSchema {

    /**
     * Service name
     */
    private String name;

    /**
     * Service version
     */
    private String version;

    /**
     * Service group
     */
    private String group;

    /**
     * Service implementation class names
     *
     * @see ServiceProviderSchema
     */
    private List<Object> impls = Lists.newArrayList();

    /**
     * IP
     */
    private String ip;

    /**
     * Port
     */
    private Integer port;

    /**
     * Binding NIC
     */
    private String nic;

    /**
     * Worker pool name
     */
    private String workerPool;

    /**
     * Service request timeout
     */
    private Integer requestTimeout;

    /**
     * Disable default filters:
     * <p>{@link com.tencent.trpc.core.filter.ProviderInvokerHeadFilter}
     * <p>{@link com.tencent.trpc.core.filter.ProviderInvokerTailFilter}
     */
    private Boolean disableDefaultFilter;

    /**
     * Filters
     */
    private List<String> filters;

    /**
     * Registries configuration
     */
    private Map<String, Map<String, Object>> registrys = Maps.newHashMap();

    /**
     * Extension configs
     */
    private Map<String, Object> extMap = Maps.newHashMap();

    /**
     * Api endpoint base path, only effective on rest protocol
     */
    private String basePath;

    /**
     * Link timeout switch
     */
    private Boolean enableLinkTimeout;

    /**
     * @see IoMode
     */
    private IoMode ioMode;

    /**
     * Reuse port switch
     */
    private Boolean reusePort;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public List<Object> getImpls() {
        return impls;
    }

    public void setImpls(List<Object> impls) {
        this.impls = impls;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public String getNic() {
        return nic;
    }

    public void setNic(String nic) {
        this.nic = nic;
    }

    public String getWorkerPool() {
        return workerPool;
    }

    public void setWorkerPool(String workerPool) {
        this.workerPool = workerPool;
    }

    public Integer getRequestTimeout() {
        return requestTimeout;
    }

    public void setRequestTimeout(Integer requestTimeout) {
        this.requestTimeout = requestTimeout;
    }

    public Boolean getDisableDefaultFilter() {
        return disableDefaultFilter;
    }

    public void setDisableDefaultFilter(Boolean disableDefaultFilter) {
        this.disableDefaultFilter = disableDefaultFilter;
    }

    public List<String> getFilters() {
        return filters;
    }

    public void setFilters(List<String> filters) {
        this.filters = filters;
    }

    public Map<String, Map<String, Object>> getRegistrys() {
        return registrys;
    }

    public void setRegistrys(Map<String, Map<String, Object>> registrys) {
        this.registrys = registrys;
    }

    @Override
    public Map<String, Object> getExtMap() {
        return extMap;
    }

    @Override
    public void setExtMap(Map<String, Object> extMap) {
        this.extMap = extMap;
    }

    public String getBasePath() {
        return basePath;
    }

    public void setBasePath(String basePath) {
        this.basePath = basePath;
    }

    public Boolean getEnableLinkTimeout() {
        return enableLinkTimeout;
    }

    public void setEnableLinkTimeout(Boolean enableLinkTimeout) {
        this.enableLinkTimeout = enableLinkTimeout;
    }

    public IoMode getIoMode() {
        return ioMode;
    }

    public void setIoMode(IoMode ioMode) {
        this.ioMode = ioMode;
    }

    public Boolean getReusePort() {
        return reusePort;
    }

    public void setReusePort(Boolean reusePort) {
        this.reusePort = reusePort;
    }

    @Override
    public String toString() {
        return "ServerServiceSchema{" +
                "name='" + name + '\'' +
                ", version='" + version + '\'' +
                ", group='" + group + '\'' +
                ", impls=" + impls +
                ", ip='" + ip + '\'' +
                ", port=" + port +
                ", nic='" + nic + '\'' +
                ", workerPool='" + workerPool + '\'' +
                ", requestTimeout=" + requestTimeout +
                ", disableDefaultFilter=" + disableDefaultFilter +
                ", filters=" + filters +
                ", registrys=" + registrys +
                ", extMap=" + extMap +
                ", basePath='" + basePath + '\'' +
                ", enableLinkTimeout=" + enableLinkTimeout +
                ", ioMode=" + ioMode +
                ", reusePort=" + reusePort +
                '}';
    }
}
