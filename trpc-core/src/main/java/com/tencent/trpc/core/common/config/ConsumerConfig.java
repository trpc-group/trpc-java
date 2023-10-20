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
import com.tencent.trpc.core.common.ConfigManager;
import com.tencent.trpc.core.logger.Logger;
import com.tencent.trpc.core.logger.LoggerFactory;
import com.tencent.trpc.core.utils.RpcUtils;
import java.net.InetSocketAddress;

/**
 * Client interface related configuration.
 */
public class ConsumerConfig<T> implements Cloneable {

    protected static final Logger logger = LoggerFactory.getLogger(ConsumerConfig.class);
    /**
     * Class name, in generic scenarios, use {@link com.tencent.trpc.core.rpc.GenericClient}.
     */
    protected Class<T> serviceInterface;
    /**
     * Mock configuration.
     */
    protected boolean mock;
    /**
     * MockClass configuration.
     */
    protected String mockClass;
    protected BackendConfig backendConfig;

    public ConsumerConfig() {
        // Add a default configuration
        backendConfig = new BackendConfig();
    }

    public T getProxy() {
        Preconditions.checkArgument(backendConfig != null, "backendConfig");
        return backendConfig.getProxy(this);
    }

    /**
     * Specify the client set call, do not use the set of this service itself.
     *
     * @param setName set name
     * @return the proxy
     */
    public T getProxyWithSourceSet(String setName) {
        Preconditions.checkArgument(backendConfig != null, "backendConfig");
        return backendConfig.getProxyWithSourceSet(this, setName);
    }

    /**
     * Specify the called set name. This called service must enable set and the set name must be exactly the same.
     *
     * @param setName set name
     * @return the proxy
     */
    public T getProxyWithDestinationSet(String setName) {
        Preconditions.checkArgument(backendConfig != null, "backendConfig");
        return backendConfig.getProxyWithDestinationSet(this, setName);
    }

    @Override
    public ConsumerConfig<T> clone() {
        try {
            ConsumerConfig<T> config = (ConsumerConfig<T>) super.clone();
            return config;
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException("", e);
        }
    }

    /**
     * Get service level timeout duration.
     *
     * @param rpcMethodName rpc method name
     * @return timeout duration
     */
    public int getMethodTimeout(String rpcMethodName) {
        return backendConfig == null ? -1 : backendConfig.getRequestTimeout();
    }

    /**
     * Get backup request time.
     *
     * @return backup request time
     */
    public int getBackupRequestTimeMs() {
        return backendConfig == null ? 0 : backendConfig.getBackupRequestTimeMs();
    }

    public boolean isGeneric() {
        return RpcUtils.isGenericClient(serviceInterface);
    }

    public InetSocketAddress getLocalAddress() {
        if (ConfigManager.getInstance().getServerConfig() != null) {
            return ConfigManager.getInstance().getServerConfig().getLocalAddress();
        }
        return null;
    }

    @Override
    public String toString() {
        return "ConsumerConfig [serviceInterface=" + serviceInterface + ", mock=" + mock
                + ", mockClass=" + mockClass + ", backendConfig=" + backendConfig + "]";
    }

    public Class<T> getServiceInterface() {
        return serviceInterface;
    }

    public void setServiceInterface(Class<T> serviceInterface) {
        this.serviceInterface = serviceInterface;
    }

    public boolean getMock() {
        return mock;
    }

    public void setMock(boolean mock) {
        this.mock = mock;
    }

    public String getMockClass() {
        return mockClass;
    }

    public void setMockClass(String mockClass) {
        this.mockClass = mockClass;
    }

    public BackendConfig getBackendConfig() {
        return backendConfig;
    }

    public void setBackendConfig(BackendConfig backendConfig) {
        this.backendConfig = backendConfig;
    }

}