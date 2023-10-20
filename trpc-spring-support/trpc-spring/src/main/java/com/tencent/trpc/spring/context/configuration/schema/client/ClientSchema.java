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

import com.google.common.collect.Lists;
import java.util.List;

/**
 * Configurations for tRPC client
 *
 * @see com.tencent.trpc.core.common.config.ClientConfig
 * @see AbstractClientProtocolSchema
 */
public class ClientSchema extends AbstractClientProtocolSchema {

    /**
     * Namespace, Production/Development
     */
    private String namespace;

    /**
     * Global request timeout in millis for clients
     */
    private Integer requestTimeout;

    /**
     * Global dynamic proxy type for clients
     */
    private String proxyType;

    /**
     * Global filters
     */
    private List<String> filters = Lists.newArrayList();

    /**
     * Global interceptors
     */
    private List<String> interceptors = Lists.newArrayList();

    /**
     * Global worker pool name
     */
    private String workerPool;

    /**
     * Caller name
     */
    private String callerServiceName;

    /**
     * Client services
     */
    private List<ClientServiceSchema> service = Lists.newArrayList();

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public Integer getRequestTimeout() {
        return requestTimeout;
    }

    public void setRequestTimeout(Integer requestTimeout) {
        this.requestTimeout = requestTimeout;
    }

    public String getProxyType() {
        return proxyType;
    }

    public void setProxyType(String proxyType) {
        this.proxyType = proxyType;
    }

    public List<String> getFilters() {
        return filters;
    }

    public void setFilters(List<String> filters) {
        this.filters = filters;
    }

    public List<String> getInterceptors() {
        return interceptors;
    }

    public void setInterceptors(List<String> interceptors) {
        this.interceptors = interceptors;
    }

    public String getWorkerPool() {
        return workerPool;
    }

    public void setWorkerPool(String workerPool) {
        this.workerPool = workerPool;
    }

    public List<ClientServiceSchema> getService() {
        return service;
    }

    public void setService(List<ClientServiceSchema> service) {
        this.service = service;
    }

    public String getCallerServiceName() {
        return callerServiceName;
    }

    public void setCallerServiceName(String callerServiceName) {
        this.callerServiceName = callerServiceName;
    }

    @Override
    public String toString() {
        return "ClientSchema(" + super.toString()
                + "namespace='" + namespace + '\''
                + ", requestTimeout=" + requestTimeout
                + ", proxyType='" + proxyType + '\''
                + ", filters=" + filters
                + ", interceptors=" + interceptors
                + ", workerPool='" + workerPool + '\''
                + ", service=" + service + '\''
                + ", callerServiceName=" + callerServiceName
                + ')';
    }
}
