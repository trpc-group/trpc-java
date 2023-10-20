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

import java.util.List;

/**
 * Configurations for service implementations
 *
 * @see com.tencent.trpc.core.common.config.ProviderConfig
 */
public class ServiceProviderSchema {

    /**
     * Implementation class name
     */
    private String impl;

    /**
     * Request timeout in millis
     */
    private Integer requestTimeout;

    /**
     * Worker pool name
     */
    private String workerPool;

    /**
     * Disable default filters：
     * <p>{@link com.tencent.trpc.core.filter.ProviderInvokerHeadFilter}
     * <p>{@link com.tencent.trpc.core.filter.ProviderInvokerTailFilter}
     */
    private Boolean disableDefaultFilter;

    /**
     * Filters
     */
    private List<String> filters;

    /**
     * Link timeout switch
     */
    private Boolean enableLinkTimeout;

    public String getImpl() {
        return impl;
    }

    public void setImpl(String impl) {
        this.impl = impl;
    }

    public Integer getRequestTimeout() {
        return requestTimeout;
    }

    public void setRequestTimeout(Integer requestTimeout) {
        this.requestTimeout = requestTimeout;
    }

    public String getWorkerPool() {
        return workerPool;
    }

    public void setWorkerPool(String workerPool) {
        this.workerPool = workerPool;
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

    public Boolean getEnableLinkTimeout() {
        return enableLinkTimeout;
    }

    public void setEnableLinkTimeout(Boolean enableLinkTimeout) {
        this.enableLinkTimeout = enableLinkTimeout;
    }

    @Override
    public String toString() {
        return "ServiceProviderSchema{" +
                "impl='" + impl + '\'' +
                ", requestTimeout=" + requestTimeout +
                ", workerPool='" + workerPool + '\'' +
                ", disableDefaultFilter=" + disableDefaultFilter +
                ", filters=" + filters +
                ", enableLinkTimeout=" + enableLinkTimeout +
                '}';
    }
}
