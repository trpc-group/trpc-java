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

package com.tencent.trpc.spring.context.configuration.schema.client;

import com.google.common.collect.Maps;
import com.tencent.trpc.core.common.Constants;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Configurations for tRPC client service
 *
 * @see com.tencent.trpc.core.common.config.BackendConfig
 * @see AbstractClientProtocolSchema
 */
public class ClientServiceSchema extends AbstractClientProtocolSchema {

    /**
     * Client service name
     */
    private String name;

    /**
     * Request timeout in millis
     */
    private Integer requestTimeout;

    /**
     * Filters
     */
    private List<String> filters;

    /**
     * Worker pool name
     */
    private String workerPool;

    /**
     * Dynamic proxy type
     */
    private String proxyType;

    /**
     * Api endpoint base path, only effective on rest protocol
     */
    private String basePath;

    /**
     * Server address
     */
    private String namingUrl;

    /**
     * NamingUrl options
     */
    private Map<String, Object> namingMap = new HashMap<String, Object>() {
        {
            put(Constants.METADATA, Maps.newHashMap());
        }
    };

    /**
     * Service namespace
     */
    private String namespace;

    /**
     * Service version
     */
    private String version;

    /**
     * Service group
     */
    private String group;

    /**
     * Callee description
     */
    private String callee;

    /**
     * Api interface
     */
    private String interfaceName;

    /**
     * Caller service name
     */
    private String callerServiceName;

    /**
     * Mock switch
     */
    private Boolean mock;

    /**
     * MockClass name
     */
    private String mockClass;

    /**
     * Backup request time
     */
    private Integer backupRequestTimeMs;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getRequestTimeout() {
        return requestTimeout;
    }

    public void setRequestTimeout(Integer requestTimeout) {
        this.requestTimeout = requestTimeout;
    }

    public List<String> getFilters() {
        return filters;
    }

    public void setFilters(List<String> filters) {
        this.filters = filters;
    }

    public String getWorkerPool() {
        return workerPool;
    }

    public void setWorkerPool(String workerPool) {
        this.workerPool = workerPool;
    }

    public String getProxyType() {
        return proxyType;
    }

    public void setProxyType(String proxyType) {
        this.proxyType = proxyType;
    }

    public String getBasePath() {
        return basePath;
    }

    public void setBasePath(String basePath) {
        this.basePath = basePath;
    }

    public String getNamingUrl() {
        return namingUrl;
    }

    public void setNamingUrl(String namingUrl) {
        this.namingUrl = namingUrl;
    }

    public Map<String, Object> getNamingMap() {
        return namingMap;
    }

    public void setNamingMap(Map<String, Object> namingMap) {
        this.namingMap = namingMap;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
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

    public String getCallee() {
        return callee;
    }

    public void setCallee(String callee) {
        this.callee = callee;
    }

    public Boolean getMock() {
        return mock;
    }

    public void setMock(Boolean mock) {
        this.mock = mock;
    }

    public String getMockClass() {
        return mockClass;
    }

    public void setMockClass(String mockClass) {
        this.mockClass = mockClass;
    }

    public Integer getBackupRequestTimeMs() {
        return backupRequestTimeMs;
    }

    public void setBackupRequestTimeMs(Integer backupRequestTimeMs) {
        this.backupRequestTimeMs = backupRequestTimeMs;
    }

    public String getInterface() {
        return interfaceName;
    }

    public ClientServiceSchema setInterface(String interfaceName) {
        this.interfaceName = interfaceName;
        return this;
    }

    public String getCallerServiceName() {
        return callerServiceName;
    }

    public void setCallerServiceName(String callerServiceName) {
        this.callerServiceName = callerServiceName;
    }

    @Override
    public String toString() {
        return "ClientServiceSchema{"
                + "name='" + name + '\''
                + ", requestTimeout=" + requestTimeout
                + ", filters=" + filters
                + ", workerPool='" + workerPool + '\''
                + ", proxyType='" + proxyType + '\''
                + ", basePath='" + basePath + '\''
                + ", namingUrl='" + namingUrl + '\''
                + ", namingMap=" + namingMap
                + ", namespace='" + namespace + '\''
                + ", version='" + version + '\''
                + ", group='" + group + '\''
                + ", callee='" + callee + '\''
                + ", interfaceName='" + interfaceName + '\''
                + ", callerServiceName='" + callerServiceName + '\''
                + ", mock=" + mock
                + ", mockClass='" + mockClass + '\''
                + ", backupRequestTimeMs=" + backupRequestTimeMs
                + '}';
    }
}
