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

package com.tencent.trpc.core.metrics;

@Deprecated
public class ActiveInvocation {

    /**
     * Active call information
     */
    private final String activeService;
    private final String activeMethodName;
    /**
     * Extended Fields
     */
    private String activeExtension;

    /**
     * Passive call information
     */
    private final MetricsCallInfo passiveCallInfo;

    public ActiveInvocation(String activeService, String activeMethodName, MetricsCallInfo metricsCallInfo) {
        this.activeService = activeService;
        this.activeMethodName = activeMethodName;
        this.passiveCallInfo = metricsCallInfo;
    }

    public String getActiveService() {
        return activeService;
    }

    public String getActiveMethodName() {
        return activeMethodName;
    }

    public String getActiveExtension() {
        return activeExtension;
    }

    public void setActiveExtension(String activeExtension) {
        this.activeExtension = activeExtension;
    }

    public MetricsCallInfo getPassiveCallInfo() {
        return passiveCallInfo;
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("ActiveInvocation{");
        sb.append("activeService='").append(activeService).append('\'');
        sb.append(", activeInterface='").append(activeMethodName).append('\'');
        sb.append(", activeExtension='").append(activeExtension).append('\'');
        sb.append(", passiveCallInfo=").append(passiveCallInfo);
        sb.append('}');
        return sb.toString();
    }

}
