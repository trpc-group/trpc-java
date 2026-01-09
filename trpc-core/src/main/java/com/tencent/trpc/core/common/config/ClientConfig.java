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

package com.tencent.trpc.core.common.config;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.tencent.trpc.core.common.Constants;
import com.tencent.trpc.core.common.config.annotation.ConfigProperty;
import com.tencent.trpc.core.common.config.constant.ConfigConstants;
import com.tencent.trpc.core.logger.Logger;
import com.tencent.trpc.core.logger.LoggerFactory;
import com.tencent.trpc.core.utils.BinderUtils;
import com.tencent.trpc.core.worker.WorkerPoolManager;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;

/**
 * Client configuration.
 */
public class ClientConfig extends BaseProtocolConfig {

    private static final Logger logger = LoggerFactory.getLogger(ClientConfig.class);
    
    @ConfigProperty
    protected String namespace;

    /**
     * Caller service name.
     */
    @ConfigProperty
    protected String callerServiceName;

    /**
     * Global default request timeout in milliseconds.
     */
    @ConfigProperty(value = Constants.DEFAULT_CLIENT_REQUEST_TIMEOUT_MS, type = Integer.class)
    protected int requestTimeout;

    /**
     * Global default proxy type.
     */
    @ConfigProperty(value = Constants.DEFAULT_PROXY)
    protected String proxyType;

    /**
     * Global default filters.
     */
    @ConfigProperty
    protected List<String> filters;

    /**
     * ClusterInvoker interceptor configuration.
     */
    @ConfigProperty
    protected List<String> interceptors;

    /**
     * Global default thread pool id.
     */
    @ConfigProperty(value = WorkerPoolManager.DEF_CONSUMER_WORKER_POOL_NAME)
    protected String workerPool;

    /**
     * BackendConfig mapping.
     */
    protected Map<String, BackendConfig> backendConfigMap = Maps.newConcurrentMap();

    /**
     * Whether the service is registered.
     */
    protected volatile boolean setDefault = false;

    /**
     * Whether it has been initialized.
     */
    protected volatile boolean initialized = false;

    /**
     * Whether it has been stopped.
     */
    protected volatile boolean stopped = false;

    /**
     * Set default values.
     */
    public synchronized void setDefault() {
        if (!setDefault) {
            setFiledDefault();
            backendConfigMap.forEach((name, item) -> {
                Preconditions.checkArgument(StringUtils.equals(name, item.getName()),
                        "Found invalid entry(name=" + name + ")");
                Preconditions.checkArgument(StringUtils.isNotBlank(name), "backendConfig's name is blank");
                item.overrideConfigDefault(this);
                item.mergeConfig(this);
                item.setDefault();
            });
            setDefault = true;
        }
    }

    /**
     * Initialize client.
     */
    public synchronized void init() {
        if (!initialized) {
            logger.info(">>>Starting init clientConfig");
            setDefault();
            backendConfigMap.values().forEach(BackendConfig::init);
            logger.info(">>>Started init clientConfig");
            initialized = true;
        }
    }

    /**
     * Stop client.
     */
    public synchronized void stop() {
        if (!stopped) {
            getBackendConfigMap().values().forEach(BackendConfig::stop);
            stopped = true;
        }
    }

    /**
     * Set client property default values.
     */
    protected void setFiledDefault() {
        BinderUtils.bind(this);
        BinderUtils.bind(this, ConfigConstants.FILTERS, Lists.newArrayList());
        BinderUtils.bind(this, ConfigConstants.INTERCEPTORS, Lists.newArrayList());
        BinderUtils.bind(this, ConfigConstants.IO_THREADS, Constants.DEFAULT_IO_THREADS);
        BinderUtils.bind(this, "backendConfigMap", Maps.newHashMap());
    }


    public void addBackendConfig(BackendConfig config) {
        checkFiledModifyPrivilege();
        getBackendConfigMap().put(config.getName(), config);
    }

    @Override
    protected void checkFiledModifyPrivilege() {
        super.checkFiledModifyPrivilege();
        Preconditions.checkArgument(!isInitialized(), "Not allow to modify field,state is(init)");
    }

    public BackendConfig getBackendConfig(String name) {
        return backendConfigMap.get(name);
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        checkFiledModifyPrivilege();
        this.namespace = namespace;
    }

    public String getCallerServiceName() {
        return callerServiceName;
    }

    public void setCallerServiceName(String callerServiceName) {
        checkFiledModifyPrivilege();
        this.callerServiceName = callerServiceName;
    }

    public int getRequestTimeout() {
        return requestTimeout;
    }

    public void setRequestTimeout(int requestTimeout) {
        checkFiledModifyPrivilege();
        this.requestTimeout = requestTimeout;
    }

    public Boolean isKeepAlive() {
        return keepAlive;
    }

    public Boolean isLazyinit() {
        return lazyinit;
    }

    public List<String> getFilters() {
        return filters;
    }

    public void setFilters(List<String> filters) {
        checkFiledModifyPrivilege();
        this.filters = filters;
    }

    public List<String> getInterceptors() {
        return interceptors;
    }

    public void setInterceptors(List<String> interceptors) {
        checkFiledModifyPrivilege();
        this.interceptors = interceptors;
    }

    public String getWorkerPool() {
        return workerPool;
    }

    public void setWorkerPool(String workerPool) {
        checkFiledModifyPrivilege();
        this.workerPool = workerPool;
    }

    public Map<String, BackendConfig> getBackendConfigMap() {
        return backendConfigMap != null ? backendConfigMap : Collections.emptyMap();
    }

    public void setBackendConfigMap(Map<String, BackendConfig> backendConfigMap) {
        checkFiledModifyPrivilege();
        this.backendConfigMap = backendConfigMap;
    }

    public String getProxyType() {
        return proxyType;
    }

    public void setProxyType(String proxyType) {
        checkFiledModifyPrivilege();
        this.proxyType = proxyType;
    }

    public boolean isSetDefault() {
        return setDefault;
    }

    public boolean isInitialized() {
        return initialized;
    }

    public Boolean isIoThreadGroupShare() {
        return ioThreadGroupShare;
    }

}
