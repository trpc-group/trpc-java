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

import com.tencent.trpc.core.common.config.annotation.ConfigProperty;
import java.util.List;

/**
 * The ability of admin needs to pay special attention to the leakage of sensitive information.
 * It is recommended not to provide write operations.
 */
public class AdminConfig {

    @ConfigProperty
    protected String adminIp;

    @ConfigProperty
    protected int adminPort;

    @ConfigProperty
    protected List<String> metricStats;

    public List<String> getMetricStats() {
        return metricStats;
    }

    public void setMetricStats(List<String> metricStats) {
        this.metricStats = metricStats;
    }

    public String getAdminIp() {
        return adminIp;
    }

    public void setAdminIp(String adminIp) {
        this.adminIp = adminIp;
    }

    public int getAdminPort() {
        return adminPort;
    }

    public void setAdminPort(int adminPort) {
        this.adminPort = adminPort;
    }

    @Override
    public String toString() {
        return "AdminConfig [adminIp=" + adminIp + ", adminPort=" + adminPort + "]";
    }

}