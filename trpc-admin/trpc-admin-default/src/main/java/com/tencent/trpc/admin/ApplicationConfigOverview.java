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

import com.tencent.trpc.core.common.ConfigManager;
import com.tencent.trpc.core.common.config.GlobalConfig;
import com.tencent.trpc.core.common.config.ServerConfig;
import java.util.List;

/**
 * Application configuration view
 */
public class ApplicationConfigOverview {

    /**
     * Application configuration view
     */
    private static ApplicationConfigOverview instance = new ApplicationConfigOverview();
    /**
     * Application configuration view object
     */
    private GlobalConfigOverview global;
    /**
     * Server configuration view object
     */
    private ServerConfigOverview server;
    /**
     * Protocol configuration view object
     */
    private List<ProtocolConfigOverview> protocols;

    private ApplicationConfigOverview() {
    }

    public static ApplicationConfigOverview getInstance() {
        return instance;
    }

    /**
     * Init config
     *
     * @param applicationConfig application configuration
     */
    public static void init(ConfigManager applicationConfig) {
        GlobalConfig globalConfig = applicationConfig.getGlobalConfig();
        GlobalConfigOverview globalConfigOverview = new GlobalConfigOverview();
        if (globalConfig != null) {
            globalConfigOverview.setNamespace(globalConfig.getNamespace());
            globalConfigOverview.setEnvName(globalConfig.getEnvName());
            globalConfigOverview.setContainerName(globalConfig.getContainerName());
        }
        instance.setGlobal(globalConfigOverview);

        ServerConfig serverConfig = applicationConfig.getServerConfig();
        ServerConfigOverview serverConfigOverview = new ServerConfigOverview();
        if (serverConfig != null) {
            serverConfigOverview.setApp(serverConfig.getApp());
            serverConfigOverview.setServerName(serverConfig.getServer());
        }
        instance.setServer(serverConfigOverview);
    }

    public GlobalConfigOverview getGlobal() {
        return global;
    }

    public void setGlobal(GlobalConfigOverview global) {
        this.global = global;
    }

    public ServerConfigOverview getServer() {
        return server;
    }

    public void setServer(ServerConfigOverview server) {
        this.server = server;
    }

    public List<ProtocolConfigOverview> getProtocols() {
        return protocols;
    }

    public void setProtocols(List<ProtocolConfigOverview> protocols) {
        this.protocols = protocols;
    }

    @Override
    public String toString() {
        return "ApplicationConfigOverview{" + "global=" + global + ", server=" + server
                + ", protocols="
                + protocols + '}';
    }
}
