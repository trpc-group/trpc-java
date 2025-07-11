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

package com.tencent.trpc.admin;

/**
 * Server configuration view
 */
public class ServerConfigOverview {

    /**
     * Application name
     */
    private String app;
    /**
     * Server name
     */
    private String serverName;

    public ServerConfigOverview() {
    }

    public String getApp() {
        return app;
    }

    public void setApp(String app) {
        this.app = app;
    }

    public String getServerName() {
        return serverName;
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    @Override
    public String toString() {
        return "ServerConfigOverview{" + "app='" + app + '\'' + ", serverName='" + serverName + '\''
                + '}';
    }
}
