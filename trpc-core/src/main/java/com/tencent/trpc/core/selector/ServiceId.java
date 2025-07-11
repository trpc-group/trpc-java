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

package com.tencent.trpc.core.selector;

import com.google.common.collect.Maps;
import java.io.Serializable;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;

public class ServiceId implements Serializable {

    private static final long serialVersionUID = -5247984211120142301L;

    /**
     * ServiceName
     **/
    private String serviceName;

    /**
     * Service version
     **/
    private String version;

    /**
     * Service group
     **/
    private String group;

    /**
     * Other config
     **/
    private Map<String, Object> parameters = Maps.newHashMap();

    /**
     * Caller info
     */
    private String callerServiceName = "";
    private String callerNamespace = "";
    private String callerEnvName = "";

    @Override
    public String toString() {
        return "ServiceId [serviceName=" + serviceName + ", version=" + version + ", group=" + group
                + ", parameters=" + parameters + ", callerServiceName=" + callerServiceName
                + ", callerNamespace=" + callerNamespace + ", callerEnvName=" + callerEnvName + "]";
    }

    public String toSimpleString() {
        return "ServiceId {serviceName=" + serviceName + ", version=" + version + ", group=" + group
                + ", callerServiceName=" + callerServiceName + ", callerNamespace=" + callerNamespace
                + ", callerEnvName=" + callerEnvName + "}";
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
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

    public Map<String, Object> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, Object> parameters) {
        this.parameters = parameters;
    }

    public Object getObject(String key, Object defaultValue) {
        Object value = parameters.get(key);
        if (value != null) {
            return value;
        }
        return defaultValue;
    }

    public String getParameter(String key) {
        Object value = parameters.get(key);
        if (value != null) {
            return String.valueOf(value).trim();
        }
        return null;
    }

    public String getParameter(String key, String defaultValue) {
        String value = getParameter(key);
        if (StringUtils.isEmpty(value)) {
            return defaultValue;
        }
        return value;
    }

    public int getParameter(String key, int defaultValue) {
        String value = getParameter(key);
        if (StringUtils.isBlank(value)) {
            return defaultValue;
        }
        return Integer.parseInt(value);
    }

    public boolean getParameter(String key, boolean defaultValue) {
        String value = getParameter(key);
        if (StringUtils.isBlank(value)) {
            return defaultValue;
        }
        return Boolean.parseBoolean(value);
    }

    public long getParameter(String key, long defaultValue) {
        String value = getParameter(key);
        if (StringUtils.isBlank(value)) {
            return defaultValue;
        }
        return Long.parseLong(value);
    }

    public String getCallerServiceName() {
        return callerServiceName;
    }

    public void setCallerServiceName(String callerServiceName) {
        this.callerServiceName = callerServiceName;
    }

    public String getCallerNamespace() {
        return callerNamespace;
    }

    public void setCallerNamespace(String callerNamespace) {
        this.callerNamespace = callerNamespace;
    }

    public String getCallerEnvName() {
        return callerEnvName;
    }

    public void setCallerEnvName(String callerEnvName) {
        this.callerEnvName = callerEnvName;
    }
}
