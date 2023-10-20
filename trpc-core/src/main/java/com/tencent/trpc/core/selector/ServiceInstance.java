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

package com.tencent.trpc.core.selector;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.apache.commons.lang3.ObjectUtils;

/**
 * Service instance define class,one serviceId contains multiple instance
 */
public class ServiceInstance implements Serializable {

    private static final long serialVersionUID = -5770826614635186498L;

    private final String host;
    private final int port;
    private final boolean isHealthy;
    private final Map<String, Object> parameters;

    public ServiceInstance(String host, int port) {
        this(host, port, new HashMap<>());
    }

    public ServiceInstance(String host, int port, Map<String, Object> parameters) {
        this.host = Objects.requireNonNull(host, "host");
        this.port = Objects.requireNonNull(port, "port");
        this.isHealthy = true;
        this.parameters = Objects.requireNonNull(parameters, "parameters");
    }

    public ServiceInstance(String host, int port, boolean isHealthy) {
        this(host, port, isHealthy, new HashMap<>());
    }

    public ServiceInstance(String host, int port, boolean isHealthy,
            Map<String, Object> parameters) {
        this.host = Objects.requireNonNull(host, "host");
        this.port = Objects.requireNonNull(port, "port");
        this.isHealthy = isHealthy;
        this.parameters = Objects.requireNonNull(parameters, "parameters");
    }

    public ServiceInstance() {
        this.host = null;
        this.port = 0;
        this.isHealthy = true;
        this.parameters = new HashMap<>();
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public boolean isHealthy() {
        return isHealthy;
    }

    public Map<String, Object> getParameters() {
        return parameters;
    }

    public String getParameter(String key) {
        if (parameters.get(key) != null) {
            return String.valueOf(parameters.get(key));
        }
        return null;
    }

    public Object getObject(String key) {
        return parameters.get(key);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((host == null) ? 0 : host.hashCode());
        result = prime * result + ((parameters == null) ? 0 : parameters.hashCode());
        result = prime * result + port;
        result = prime * result + (isHealthy ? 1 : 0);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        ServiceInstance other = (ServiceInstance) obj;
        // 两个对象的成员变量 值相等 就返回true， 否则返回 false
        return !ObjectUtils.notEqual(host, other.host) && port == other.port
                && !ObjectUtils.notEqual(parameters, other.parameters) && isHealthy == other.isHealthy;
    }

    @Override
    public String toString() {
        return "ServiceInstance{"
                + "host='" + host + '\''
                + ", port=" + port
                + '}';
    }

    public String toFullString() {
        return "ServiceInstance{"
                + "host='" + host + '\''
                + ", port=" + port
                + ", parameters=" + parameters
                + '}';
    }
}
