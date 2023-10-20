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

package com.tencent.trpc.spring.context;

import com.google.common.collect.Maps;
import com.tencent.trpc.core.common.ConfigManager;
import com.tencent.trpc.core.common.config.BackendConfig;
import com.tencent.trpc.spring.annotation.TRpcClient;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.InjectionMetadata;
import org.springframework.core.annotation.AnnotationAttributes;

/**
 * Extends {@link AnnotationInjectedBeanPostProcessor} to accomplish injecting
 * tRPC client proxies to fields annotated with @{@link TRpcClient}.
 *
 * @see TRpcClient
 */
public class TRpcClientAnnotationBeanPostProcessor extends AnnotationInjectedBeanPostProcessor {

    /**
     * field of {@link TRpcClient} used to identify client proxy
     */
    private static final String TRPC_CLIENT_ATTRIBUTE_ID_KEY = "id";
    /**
     * backend proxy cache
     */
    private final Map<String, Object> backendProxies = Maps.newConcurrentMap();

    public TRpcClientAnnotationBeanPostProcessor() {
        super(TRpcClient.class);
    }

    /**
     * Get the tRPC backend proxy instance to inject with.
     *
     * @param attributes {@link AnnotationAttributes} of the annotation
     * @param injectedType the type of the injected bean
     * @return the tRPC backend proxy instance to inject with
     */
    @Override
    protected Object doGetInjectedBean(AnnotationAttributes attributes, Object bean, String beanName,
            Class<?> injectedType, InjectionMetadata.InjectedElement injectedElement) {
        String backendName = getBackendName(attributes, injectedType);
        return backendProxies.computeIfAbsent(backendName, this::createBackendProxy);
    }

    @Override
    protected String buildInjectedObjectCacheKey(AnnotationAttributes attributes, Object bean,
            String beanName, Class<?> injectedType, InjectionMetadata.InjectedElement injectedElement) {
        String backendName = getBackendName(attributes, injectedType);
        return "@TRpcClient(" + backendName + ")";
    }

    /**
     * Get the tRPC client name from {@link TRpcClient} annotation
     */
    private String getBackendName(AnnotationAttributes attributes, Class<?> injectedType) {
        String backendName = attributes.getString(TRPC_CLIENT_ATTRIBUTE_ID_KEY);
        if (StringUtils.isNotBlank(backendName)) {
            return backendName;
        }
        throw new IllegalArgumentException("@TRpcClient id must have value " + injectedType.getName());
    }

    /**
     * Get the tRPC client proxy from {@link ConfigManager}
     */
    private Object createBackendProxy(String name) {
        BackendConfig backendConfig = ConfigManager.getInstance().getClientConfig().getBackendConfigMap().get(name);
        if (backendConfig == null) {
            throw new IllegalStateException("Cloud not found any backend of " + name + " from TRpc ConfigManager");
        }
        return backendConfig.getDefaultProxy();
    }

    @Override
    public void destroy() throws Exception {
        super.destroy();
        backendProxies.clear();
    }
}