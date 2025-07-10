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

/**
 * Metrics call information define
 */
public class MetricsCallInfo {

    private final String app;
    private final String server;
    private final String service;
    private final String methodName;
    private final String ip;
    private final String container;
    private final String containerSetId;

    public MetricsCallInfo(String app, String server, String service, String methodName, String ip,
            String container, String containerSetId) {
        this.app = app;
        this.server = server;
        this.service = service;
        this.methodName = methodName;
        this.ip = ip;
        this.container = container;
        this.containerSetId = containerSetId;
    }

    public String getApp() {
        return app;
    }

    public String getServer() {
        return server;
    }

    public String getService() {
        return service;
    }

    public String getMethodName() {
        return methodName;
    }

    public String getIp() {
        return ip;
    }

    public String getContainer() {
        return container;
    }

    public String getContainerSetId() {
        return containerSetId;
    }

    @Override
    public String toString() {
        return "MetricsCallInfo{"
                + "App='" + app + '\''
                + ", Server='" + server + '\''
                + ", Service='" + service + '\''
                + ", Interface='" + methodName + '\''
                + ", Ip='" + ip + '\''
                + ", Container='" + container + '\''
                + ", ContainerSetId='" + containerSetId + '\''
                + '}';
    }

}
