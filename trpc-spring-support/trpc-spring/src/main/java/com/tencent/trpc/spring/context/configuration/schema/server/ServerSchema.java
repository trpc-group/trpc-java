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

package com.tencent.trpc.spring.context.configuration.schema.server;

import com.google.common.collect.Lists;
import java.util.List;

/**
 * Configuration for tRPC server
 *
 * @see com.tencent.trpc.core.common.config.ServerConfig
 */
public class ServerSchema {

    /**
     * Application name
     */
    private String app;

    /**
     * Server name
     */
    private String server;

    /**
     * Server admin configs
     */
    private ServerAdminSchema admin = new ServerAdminSchema();

    /**
     * Service configs
     */
    private List<ServerServiceSchema> service = Lists.newArrayList();

    /**
     * Global listeners
     */
    private List<ServerListenerSchema> serverListener = Lists.newArrayList();

    /**
     * Server container bootstrap listeners
     *
     * @see com.tencent.trpc.core.common.TRPCRunListener
     */
    private List<String> runListeners = Lists.newArrayList();

    /**
     * Binding NICs
     */
    private String nic;

    /**
     * Server ip
     */
    private String localIp;

    /**
     * Global request timeout
     */
    private Integer requestTimeout;

    /**
     * Link timeout switch
     */
    private Boolean enableLinkTimeout;

    /**
     * Disable default filters:
     * <p>{@link com.tencent.trpc.core.filter.ProviderInvokerHeadFilter}
     * <p>{@link com.tencent.trpc.core.filter.ProviderInvokerTailFilter}
     */
    private Boolean disableDefaultFilter;

    /**
     * Filters
     */
    private List<String> filters = Lists.newArrayList();

    /**
     * Server close timeout
     */
    private Long closeTimeout;

    /**
     * Server wait timeout
     */
    private Long waitTimeout;

    /**
     * Worker pool name
     */
    private String workerPool;

    /**
     * Config center plugin name
     */
    private String configCenter;

    public String getApp() {
        return app;
    }

    public void setApp(String app) {
        this.app = app;
    }

    public String getServer() {
        return server;
    }

    public void setServer(String server) {
        this.server = server;
    }

    public ServerAdminSchema getAdmin() {
        return admin;
    }

    public void setAdmin(ServerAdminSchema admin) {
        this.admin = admin;
    }

    public List<ServerServiceSchema> getService() {
        return service;
    }

    public void setService(List<ServerServiceSchema> service) {
        this.service = service;
    }

    public List<ServerListenerSchema> getServerListener() {
        return serverListener;
    }

    public void setServerListener(List<ServerListenerSchema> serverListener) {
        this.serverListener = serverListener;
    }

    public List<String> getRunListeners() {
        return runListeners;
    }

    public void setRunListeners(List<String> runListeners) {
        this.runListeners = runListeners;
    }

    public String getNic() {
        return nic;
    }

    public void setNic(String nic) {
        this.nic = nic;
    }

    public String getLocalIp() {
        return localIp;
    }

    public void setLocalIp(String localIp) {
        this.localIp = localIp;
    }

    public Integer getRequestTimeout() {
        return requestTimeout;
    }

    public void setRequestTimeout(Integer requestTimeout) {
        this.requestTimeout = requestTimeout;
    }

    public Boolean getEnableLinkTimeout() {
        return enableLinkTimeout;
    }

    public void setEnableLinkTimeout(Boolean enableLinkTimeout) {
        this.enableLinkTimeout = enableLinkTimeout;
    }

    public Boolean getDisableDefaultFilter() {
        return disableDefaultFilter;
    }

    public void setDisableDefaultFilter(Boolean disableDefaultFilter) {
        this.disableDefaultFilter = disableDefaultFilter;
    }

    public List<String> getFilters() {
        return filters;
    }

    public void setFilters(List<String> filters) {
        this.filters = filters;
    }

    public Long getCloseTimeout() {
        return closeTimeout;
    }

    public void setCloseTimeout(Long closeTimeout) {
        this.closeTimeout = closeTimeout;
    }

    public Long getWaitTimeout() {
        return waitTimeout;
    }

    public void setWaitTimeout(Long waitTimeout) {
        this.waitTimeout = waitTimeout;
    }

    public String getWorkerPool() {
        return workerPool;
    }

    public void setWorkerPool(String workerPool) {
        this.workerPool = workerPool;
    }

    public String getConfigCenter() {
        return configCenter;
    }

    public void setConfigCenter(String configCenter) {
        this.configCenter = configCenter;
    }

    @Override
    public String toString() {
        return "ServerSchema{" +
                "app='" + app + '\'' +
                ", server='" + server + '\'' +
                ", admin=" + admin +
                ", service=" + service +
                ", serverListener=" + serverListener +
                ", runListeners=" + runListeners +
                ", nic='" + nic + '\'' +
                ", localIp='" + localIp + '\'' +
                ", requestTimeout=" + requestTimeout +
                ", enableLinkTimeout=" + enableLinkTimeout +
                ", disableDefaultFilter=" + disableDefaultFilter +
                ", filters=" + filters +
                ", closeTimeout=" + closeTimeout +
                ", workerPool='" + workerPool + '\'' +
                ", configCenter='" + configCenter + '\'' +
                '}';
    }
}
