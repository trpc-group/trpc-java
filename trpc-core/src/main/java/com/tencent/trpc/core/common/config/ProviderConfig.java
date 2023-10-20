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

package com.tencent.trpc.core.common.config;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.tencent.trpc.core.common.Constants;
import com.tencent.trpc.core.common.config.annotation.ConfigProperty;
import com.tencent.trpc.core.common.config.constant.ConfigConstants;
import com.tencent.trpc.core.filter.FilterManager;
import com.tencent.trpc.core.logger.Logger;
import com.tencent.trpc.core.logger.LoggerFactory;
import com.tencent.trpc.core.rpc.anno.TRpcService;
import com.tencent.trpc.core.utils.BinderUtils;
import com.tencent.trpc.core.utils.ClassLoaderUtils;
import com.tencent.trpc.core.utils.PreconditionUtils;
import com.tencent.trpc.core.worker.WorkerPoolManager;
import com.tencent.trpc.core.worker.spi.WorkerPool;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * Configuration for the list of interfaces related to the service.
 */
public class ProviderConfig<T> implements Cloneable {

    protected static final Logger logger = LoggerFactory.getLogger(ProviderConfig.class);
    /**
     * Business service related configuration.
     */
    protected Class<T> serviceInterface;
    /**
     * Fully qualified class name of the provider implementation.
     */
    @ConfigProperty(name = "impl")
    protected String refClazz;
    protected T ref;
    /**
     * Service related configuration.
     */
    protected ServiceConfig serviceConfig;
    /**
     * Request timeout duration.
     */
    @ConfigProperty(value = Constants.DEFAULT_SERVER_TIMEOUT_MS, type = Integer.class, override = true)
    protected int requestTimeout;
    /**
     * Thread pool configuration.
     */
    @ConfigProperty(value = WorkerPoolManager.DEF_PROVIDER_WORKER_POOL_NAME, override = true)
    protected String workerPool;
    /**
     * Whether to disable default filters:
     * <p>{@link com.tencent.trpc.core.filter.ProviderInvokerHeadFilter}</p>
     * <p>{@link com.tencent.trpc.core.filter.ProviderInvokerTailFilter}</p>
     */
    @ConfigProperty(value = "false", type = Boolean.class)
    protected Boolean disableDefaultFilter;
    /**
     * Filter configuration.
     */
    @ConfigProperty
    protected List<String> filters;
    /**
     * Whether to enable full link timeout.
     */
    @ConfigProperty(value = "false", type = Boolean.class, override = true)
    protected Boolean enableLinkTimeout;
    /**
     * Worker pool.
     */
    protected WorkerPool workerPoolObj;
    protected volatile boolean setDefault = false;
    protected volatile boolean inited = false;

    public static <T> ProviderConfig<T> newInstance() {
        return new ProviderConfig<T>();
    }

    public synchronized void init() {
        if (!inited) {
            setDefault();
            initServiceInterfaceConfig();
            validateFilterConfig();
            initWorkerPool();
            inited = true;
            logger.info("Init ProviderConfig, inited info: " + toString());
        }
    }

    /**
     * Set default values.
     */
    public synchronized void setDefault() {
        if (!setDefault) {
            setFieldDefault();
            setDefault = true;
        }
    }

    /**
     * Service configuration prioritizes server:service node configurations. When there is no configuration and the
     * value is allowed to be overridden, we use the global configuration for overriding.
     * Finally, set the default value {@link ServiceConfig#setFiledDefault()}.
     *
     * @param serviceConfig service config
     */
    public void overrideConfigDefault(ServiceConfig serviceConfig) {
        Objects.requireNonNull(serviceConfig, "serviceConfig");
        BinderUtils.bind(this, serviceConfig, Boolean.TRUE);
        BinderUtils.bind(this, ConfigConstants.DISABLE_DEFAULT_FILTER, serviceConfig.getDisableDefaultFilter());
        BinderUtils.bind(this, ConfigConstants.FILTERS, CollectionUtils.isNotEmpty(serviceConfig.getFilters())
                ? serviceConfig.getFilters() : Lists.newArrayList());
    }

    /**
     * Set default values.
     */
    protected void setFieldDefault() {
        BinderUtils.bind(this);
    }

    /**
     * Validate filter.
     */
    protected void validateFilterConfig() {
        Optional.ofNullable(getFilters()).ifPresent(fs -> fs.forEach(FilterManager::validate));
    }

    /**
     * Initialize the thread pool.
     */
    protected void initWorkerPool() {
        String workerPool = getWorkerPool();
        Objects.requireNonNull(workerPool, "workerPoolName");
        WorkerPoolManager.validate(workerPool);
        workerPoolObj = WorkerPoolManager.get(workerPool);
        Objects.requireNonNull(workerPoolObj, "Not found worker pool with name <" + workerPool + ">");
    }


    @SuppressWarnings("unchecked")
    @Override
    public ProviderConfig<T> clone() {
        try {
            ProviderConfig<T> config = (ProviderConfig<T>) super.clone();
            config.resetFlag();
            return config;
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException("", e);
        }
    }

    protected void resetFlag() {
        this.setDefault = false;
        this.inited = false;
    }

    /**
     * Ensure ref, serviceInterface, and refClazz are all assigned.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    protected void initServiceInterfaceConfig() {
        // 1) Ensure ref
        if (ref == null) {
            if (StringUtils.isNotBlank(refClazz)) {
                try {
                    this.ref = (T) ClassLoaderUtils.getClassLoader(this.getClass()).loadClass(this.refClazz)
                            .newInstance();
                } catch (Exception e) {
                    throw new RuntimeException("refClazz(" + refClazz + ") new instance exception", e);
                }
            }
        }
        Preconditions.checkArgument(getRef() != null, "ServiceImpl is null");
        // 2) Ensure serviceInterface
        if (serviceInterface == null) {
            // First find the one with RpcService annotation, if empty, use the first one by default
            Class[] interfaces = ref.getClass().getInterfaces();
            PreconditionUtils.checkArgument(interfaces.length >= 1, "serviceImpl(%s) no interface",
                    ref.getClass());
            for (Class each : interfaces) {
                if (each.getAnnotation(TRpcService.class) != null) {
                    serviceInterface = each;
                }
            }
            if (serviceInterface != null) {
                serviceInterface = interfaces[0];
            }
        }
        Preconditions.checkArgument(getServiceInterface() != null, "ServiceInterface is null");
        // 3) For Spring rewriting, the clazz type will not be the real type. So only assign here if it's null
        if (this.refClazz == null) {
            this.refClazz = ref.getClass().getName();
        }
        if (!getServiceInterface().isAssignableFrom(ref.getClass())) {
            throw new IllegalArgumentException("ServiceImpl must be sub class of class:"
                    + getServiceInterface() + ", serviceImpl: " + getRefClassName());
        }
    }

    public String getRefClassName() {
        return ref == null ? "<NULL>" : ref.getClass().getName();
    }

    public WorkerPool getWorkerPoolObj() {
        return workerPoolObj;

    }

    @Override
    public String toString() {
        return "ProviderConfig {serviceInterface=" + serviceInterface + ", serviceImplClazz="
                + getRefClassName() + ", ref=" + ref + ", inited=" + inited + "}";
    }

    protected void checkFiledModifyPrivilege() {
        Preconditions.checkArgument(!isInited(), "Not allow to modify field,state is(init)");
    }

    public Class<T> getServiceInterface() {
        return serviceInterface;
    }

    public void setServiceInterface(Class<T> serviceInterface) {
        checkFiledModifyPrivilege();
        this.serviceInterface = serviceInterface;
    }

    public String getRefClazz() {
        return refClazz;
    }

    public void setRefClazz(String refClazz) {
        checkFiledModifyPrivilege();
        this.refClazz = refClazz;
    }

    public T getRef() {
        return ref;
    }

    public void setRef(T ref) {
        checkFiledModifyPrivilege();
        Objects.requireNonNull(ref, "ref");
        this.ref = ref;
    }

    public ServiceConfig getServiceConfig() {
        return serviceConfig;
    }

    public void setServiceConfig(ServiceConfig serviceConfig) {
        checkFiledModifyPrivilege();
        this.serviceConfig = serviceConfig;
    }

    public boolean isInited() {
        return inited;
    }

    public boolean isSetDefault() {
        return setDefault;
    }

    public int getRequestTimeout() {
        if (requestTimeout > 0) {
            return requestTimeout;
        }
        // 兼容通过配置类的方式
        return serviceConfig.getRequestTimeout();
    }

    public void setRequestTimeout(int requestTimeout) {
        checkFiledModifyPrivilege();
        this.requestTimeout = requestTimeout;
    }

    public String getWorkerPool() {
        if (null != workerPool) {
            return workerPool;
        }
        // compatible with configuration class method
        return serviceConfig.getWorkerPool();
    }

    public void setWorkerPool(String workerPool) {
        checkFiledModifyPrivilege();
        this.workerPool = workerPool;
    }

    public Boolean getDisableDefaultFilter() {
        return disableDefaultFilter;
    }

    public void setDisableDefaultFilter(Boolean disableDefaultFilter) {
        checkFiledModifyPrivilege();
        this.disableDefaultFilter = disableDefaultFilter;
    }

    public List<String> getFilters() {
        if (CollectionUtils.isNotEmpty(filters)) {
            return filters;
        }
        // compatible with configuration class method
        return serviceConfig.getFilters();
    }

    public void setFilters(List<String> filters) {
        checkFiledModifyPrivilege();
        this.filters = filters;
    }

    public Boolean getEnableLinkTimeout() {
        if (null != enableLinkTimeout) {
            return enableLinkTimeout;
        }
        // compatible with configuration class method
        return serviceConfig.getEnableLinkTimeout();
    }

    public void setEnableLinkTimeout(Boolean enableLinkTimeout) {
        checkFiledModifyPrivilege();
        this.enableLinkTimeout = enableLinkTimeout;
    }

}