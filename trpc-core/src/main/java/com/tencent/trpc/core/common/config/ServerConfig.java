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
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.tencent.trpc.core.admin.AdminServiceManager;
import com.tencent.trpc.core.admin.spi.AdminService;
import com.tencent.trpc.core.common.Constants;
import com.tencent.trpc.core.common.config.annotation.ConfigProperty;
import com.tencent.trpc.core.common.config.constant.ConfigConstants;
import com.tencent.trpc.core.container.spi.ServerListener;
import com.tencent.trpc.core.logger.Logger;
import com.tencent.trpc.core.logger.LoggerFactory;
import com.tencent.trpc.core.rpc.RpcServerManager;
import com.tencent.trpc.core.utils.BinderUtils;
import com.tencent.trpc.core.utils.NetUtils;
import com.tencent.trpc.core.worker.WorkerPoolManager;
import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;

public class ServerConfig {

    protected static final Logger logger = LoggerFactory.getLogger(ServerConfig.class);
    /**
     * Application name.
     */
    @ConfigProperty
    protected String app;
    /**
     * Application service name.
     */
    @ConfigProperty
    protected String server;
    /**
     * Administration configuration.
     */
    protected AdminConfig adminConfig;
    /**
     * Local network card.
     */
    @ConfigProperty
    protected String nic;
    /**
     * Local IP.
     */
    @ConfigProperty
    protected String localIp;
    /**
     * Local address.
     */
    protected InetSocketAddress localAddress;
    /**
     * Request timeout duration.
     */
    @ConfigProperty(value = Constants.DEFAULT_SERVER_TIMEOUT_MS, type = Integer.class)
    protected int requestTimeout;
    /**
     * Whether to enable full link timeout.
     */
    @ConfigProperty(value = "false", type = Boolean.class)
    protected Boolean enableLinkTimeout;
    /**
     * Whether to disable default filters:
     * <p>{@link com.tencent.trpc.core.filter.ProviderInvokerHeadFilter}</p>
     * <p>{@link com.tencent.trpc.core.filter.ProviderInvokerTailFilter}</p>
     */
    @ConfigProperty(value = "false", type = Boolean.class)
    protected Boolean disableDefaultFilter;
    @ConfigProperty
    protected List<String> filters = Lists.newArrayList();
    /**
     * Timeout duration to close the service.
     */
    @ConfigProperty(value = Constants.DEFAULT_SERVER_CLOSE_TIMEOUT, type = Long.class)
    protected long closeTimeout;
    /**
     * Default service registry (currently only supports one).
     */
    protected List<String> registryIds;
    /**
     * Default bound protocol (currently only supports one).
     */
    protected List<ServerListener> serverListeners = Collections.emptyList();
    /**
     * Administration service.
     */
    protected AdminService adminService;
    /**
     * Thread pool.
     */
    @ConfigProperty(value = WorkerPoolManager.DEF_PROVIDER_WORKER_POOL_NAME)
    protected String workerPool;
    /**
     * Configuration center.
     */
    @ConfigProperty(value = Constants.DEFAULT_CONFIGCENTER)
    protected String configCenter;
    @ConfigProperty
    protected List<String> runListeners = Lists.newArrayList();
    /**
     * Service mapping.
     */
    protected Map<String, ServiceConfig> serviceMap = Maps.newHashMap();
    protected volatile boolean setDefault = false;
    protected volatile boolean inited = false;
    protected volatile boolean stoped = false;
    protected volatile boolean registed = false;

    public void setDefault() {
        if (!setDefault) {
            setFieldDefault();
            serviceMap.values().forEach(item -> {
                item.overrideConfigDefault(this);
                item.mergeConfig(this);
                item.setDefault();
            });
            setDefault = true;
        }
    }

    public synchronized void init() {
        try {
            if (!inited) {
                setDefault();
                Preconditions.checkArgument(StringUtils.isNotBlank(localIp),
                        "serverConfig,localIp is null");
                // Initialize
                serviceMap.values().forEach(ServiceConfig::init);
                // Start business listeners
                serverListeners.forEach(v -> {
                    v.onServerStarted();
                    logger.info(">>>tRPC Server listener {} start", v.getClass());
                });
                // Start services
                serviceMap.values().forEach(ServiceConfig::export);
                // Start administration port
                initAdmin();
                inited = true;
            }
        } catch (Exception exception) {
            logger.error("Server init exception, will stop", exception);
            stop();
            throw new RuntimeException("Server init exception", exception);
        }
    }

    public synchronized void stop() {
        if (!stoped) {
            Optional.ofNullable(adminService).ifPresent(AdminService::stop);
            serviceMap.values().forEach(v -> {
                try {
                    v.unExport();
                } catch (Exception exception) {
                    logger.error("Provider unExport failed, service:{}, exception:", v, exception);
                }
            });
            serverListeners.forEach(v -> {
                v.onServerStopped();
                logger.info("tRPC Server listener {} stop", v.getClass());
            });
            RpcServerManager.shutdown();
            stoped = true;
        }
    }

    /**
     * Set server default values.
     */
    protected void setFieldDefault() {
        BinderUtils.bind(this);
        if (nic != null) {
            BinderUtils.lazyBind(this, ConfigConstants.LOCAL_IP, nic,
                    obj -> NetUtils.resolveMultiNicAddr((String) obj));
        } else {
            BinderUtils.bind(this, ConfigConstants.LOCAL_IP, NetUtils.getHostIp());
        }
        BinderUtils.bind(this, ConfigConstants.LOCAL_IP, NetUtils.LOCAL_HOST);
        BinderUtils.bind(this, ConfigConstants.LOCAL_ADDRESS, new InetSocketAddress(localIp, 0));
        BinderUtils.bind(this, ConfigConstants.FILTERS, Lists.newArrayList());
    }

    public Boolean getEnableLinkTimeout() {
        return enableLinkTimeout;
    }

    public void setEnableLinkTimeout(Boolean enableLinkTimeout) {
        checkFiledModifyPrivilege();
        this.enableLinkTimeout = enableLinkTimeout;
    }

    protected void initAdmin() {
        if (adminConfig != null && adminConfig.getAdminPort() != 0) {
            this.adminService = AdminServiceManager.getManager().getDefaultExtension();
            if (this.adminService != null) {
                adminService.setServerConfig(this);
                adminService.init();
                adminService.start();
            }
        }
    }

    public synchronized void register() {
        if (!registed) {
            init();
            serviceMap.values().forEach(ServiceConfig::register);
            registed = true;
        }
    }

    public synchronized void unregister() {
        // to prevent partial registration failure, causing registed to remain false, do not check registed here
        serviceMap.values().forEach(ServiceConfig::unRegister);
        registed = false;
    }

    public Map<String, ServiceConfig> getServiceMap() {
        return serviceMap != null ? serviceMap : Collections.emptyMap();
    }

    public void setServiceMap(Map<String, ServiceConfig> serviceMap) {
        checkFiledModifyPrivilege();
        this.serviceMap = serviceMap;
    }

    public InetSocketAddress getLocalAddress() {
        if (localAddress == null) {
            if (StringUtils.isNotBlank(localIp)) {
                return new InetSocketAddress(localIp, 0);
            }
        }
        return localAddress;
    }

    public void setLocalAddress(InetSocketAddress localAddress) {
        checkFiledModifyPrivilege();
        this.localAddress = localAddress;
    }

    protected void checkFiledModifyPrivilege() {
        Preconditions.checkArgument(!isInited(), "Not allow to modify field,state is(init)");
    }

    public String getApp() {
        return app;
    }

    public void setApp(String app) {
        checkFiledModifyPrivilege();
        this.app = app;
    }

    public boolean isSetDefault() {
        return setDefault;
    }

    public boolean isInited() {
        return inited;
    }

    public String getLocalIp() {
        return localIp;
    }

    public void setLocalIp(String localIp) {
        checkFiledModifyPrivilege();
        this.localIp = localIp;
    }

    public String getServer() {
        return server;
    }

    public void setServer(String server) {
        checkFiledModifyPrivilege();
        this.server = server;
    }

    public AdminConfig getAdminConfig() {
        return adminConfig;
    }

    public void setAdminConfig(AdminConfig adminConfig) {
        checkFiledModifyPrivilege();
        this.adminConfig = adminConfig;
    }

    public Boolean getDisableDefaultFilter() {
        return disableDefaultFilter;
    }

    public void setDisableDefaultFilter(Boolean disableDefaultFilter) {
        checkFiledModifyPrivilege();
        this.disableDefaultFilter = disableDefaultFilter;
    }

    public List<String> getFilters() {
        return filters;
    }

    public void setFilters(List<String> filters) {
        checkFiledModifyPrivilege();
        this.filters = filters;
    }

    public long getCloseTimeout() {
        return closeTimeout;
    }

    public void setCloseTimeout(long closeTimeout) {
        checkFiledModifyPrivilege();
        this.closeTimeout = closeTimeout;
    }

    public List<String> getRegistryIds() {
        return registryIds;
    }

    public void setRegistryIds(List<String> registryIds) {
        checkFiledModifyPrivilege();
        this.registryIds = registryIds;
    }

    public List<ServerListener> getServerListeners() {
        return serverListeners;
    }

    public void setServerListeners(List<ServerListener> serverListeners) {
        checkFiledModifyPrivilege();
        this.serverListeners = serverListeners;
    }

    public AdminService getAdminService() {
        return adminService;
    }

    public void setAdminService(AdminService adminService) {
        checkFiledModifyPrivilege();
        this.adminService = adminService;
    }

    public int getRequestTimeout() {
        return requestTimeout;
    }

    public void setRequestTimeout(int requestTimeout) {
        checkFiledModifyPrivilege();
        this.requestTimeout = requestTimeout;
    }

    public String getWorkerPool() {
        return workerPool;
    }

    public void setWorkerPool(String workerPool) {
        checkFiledModifyPrivilege();
        this.workerPool = workerPool;
    }

    public String getConfigCenter() {
        return configCenter;
    }

    public void setConfigCenter(String configCenter) {
        checkFiledModifyPrivilege();
        this.configCenter = configCenter;
    }

    public String getNic() {
        return nic;
    }

    public void setNic(String nic) {
        checkFiledModifyPrivilege();
        this.nic = nic;
    }

    public List<String> getRunListeners() {
        return runListeners;
    }

    public void setRunListeners(List<String> runListeners) {
        checkFiledModifyPrivilege();
        this.runListeners = runListeners;
    }

}
