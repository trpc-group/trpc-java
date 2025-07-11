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

package com.tencent.trpc.admin.service;

import com.tencent.trpc.core.admin.spi.Admin;
import com.tencent.trpc.core.admin.spi.AdminService;
import com.tencent.trpc.core.common.LifecycleBase;
import com.tencent.trpc.core.common.config.AdminConfig;
import com.tencent.trpc.core.common.config.ServerConfig;
import com.tencent.trpc.core.exception.LifecycleException;
import com.tencent.trpc.core.extension.ExtensionClass;
import com.tencent.trpc.core.extension.ExtensionLoader;
import com.tencent.trpc.core.logger.Logger;
import com.tencent.trpc.core.logger.LoggerFactory;
import com.tencent.trpc.core.stat.MetricStatFactory;
import java.util.Collection;
import java.util.List;
import javax.ws.rs.Path;
import org.jboss.resteasy.core.ResteasyDeploymentImpl;
import org.jboss.resteasy.plugins.server.netty.NettyJaxrsServer;
import org.jboss.resteasy.spi.Registry;

/**
 * Default management implementation
 */
public class DefaultAdminServiceImpl implements AdminService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultAdminServiceImpl.class);
    /**
     * netty jar server
     */
    private NettyJaxrsServer adminRestServer;
    private ServerConfig serverConfig;
    /**
     * Lifecycle class
     */
    private LifecycleObj lifecycleObj = new LifecycleObj();

    @Override
    public void init() {
        lifecycleObj.init();
    }

    @Override
    public void start() {
        lifecycleObj.start();
    }

    @Override
    public void stop() {
        lifecycleObj.stop();
    }

    @Override
    public void setServerConfig(ServerConfig serverConfig) {
        this.serverConfig = serverConfig;
    }

    protected class LifecycleObj extends LifecycleBase {

        @Override
        protected void initInternal() throws Exception {
            super.initInternal();
            adminRestServer = new NettyJaxrsServer();
            AdminConfig adminConfig = serverConfig.getAdminConfig();
            adminRestServer.setHostname(adminConfig.getAdminIp());
            adminRestServer.setPort(adminConfig.getAdminPort());
            adminRestServer.setDeployment(new ResteasyDeploymentImpl());
        }

        @Override
        protected void startInternal() {
            try {
                super.startInternal();
                adminRestServer.start();
                Registry registry = adminRestServer.getDeployment().getRegistry();
                ExtensionLoader<Admin> extensionLoader = ExtensionLoader.getExtensionLoader(Admin.class);
                Collection<ExtensionClass<Admin>> extensionClasses = extensionLoader.getAllExtensionClass();
                for (ExtensionClass<Admin> extensionClass : extensionClasses) {
                    Class<?> adminExtensionClass = extensionClass.getClazz();
                    if (adminExtensionClass.getAnnotation(Path.class) != null) {
                        registry.addResourceFactory(new TRpcResourceFactory(adminExtensionClass,
                                extensionClass.getExtInstance(), "/"));
                    }
                }

                List<String> metricStats = serverConfig.getAdminConfig().getMetricStats();
                MetricStatFactory.startStat(metricStats);
                LOGGER.info("AdminService started at {} port.",
                        serverConfig.getAdminConfig().getAdminPort());
            } catch (Exception e) {
                throw new LifecycleException("AdminService failed to start!", e);
            }
        }

        @Override
        protected void stopInternal() {
            try {
                super.stopInternal();
                if (adminRestServer != null) {
                    adminRestServer.stop();
                }
                MetricStatFactory.closeStat();
            } catch (Exception e) {
                throw new LifecycleException("AdminService failed to stop!");
            }
        }
    }
}
