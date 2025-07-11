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

package com.tencent.trpc.registry.consul;

import com.ecwid.consul.v1.Response;
import com.ecwid.consul.v1.health.model.HealthService;
import com.tencent.trpc.core.common.config.PluginConfig;
import com.tencent.trpc.core.common.config.ProtocolConfig;
import com.tencent.trpc.core.extension.Extension;
import com.tencent.trpc.core.logger.Logger;
import com.tencent.trpc.core.logger.LoggerFactory;
import com.tencent.trpc.registry.center.AbstractFailedRetryRegistryCenter;
import com.tencent.trpc.registry.center.NotifyListener;
import com.tencent.trpc.core.exception.TRpcExtensionException;
import com.tencent.trpc.core.registry.RegisterInfo;
import com.tencent.trpc.support.ConsulInstanceManager;

import static com.tencent.trpc.support.util.NotifiersServiceUtils.stopAndClearAllNotifiersTask;
import static com.tencent.trpc.support.util.NotifiersServiceUtils.unWatchServiceRegisterInfoUpdate;
import static com.tencent.trpc.support.util.NotifiersServiceUtils.watchServiceRegisterInfoUpdate;

import static com.tencent.trpc.support.util.ConsulServiceUtils.getWatchTimeout;
import static com.tencent.trpc.support.util.ConsulServiceUtils.convert;

import static com.tencent.trpc.support.util.TtlSchedulerInstanceUtils.stopAndClearAllTtlTask;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.apache.commons.collections4.MapUtils;

import static com.tencent.trpc.support.constant.ConsulConstant.CONSUL_SERVICE_INDEX;
import static com.tencent.trpc.support.constant.ConsulConstant.ANY_VALUE;

/**
 * Consul registry.
 */
@Extension("consul")
public class ConsulRegistryCenter extends AbstractFailedRetryRegistryCenter {

    private static final Logger logger = LoggerFactory.getLogger(ConsulRegistryCenter.class);

    /**
     * Consul instance management class.
     */
    private ConsulInstanceManager consulInstanceManager;

    private ProtocolConfig protocolConfig;

    @Override
    public void setPluginConfig(PluginConfig pluginConfig) throws TRpcExtensionException {
        super.setPluginConfig(pluginConfig);
        initProtocolConfig(pluginConfig);
    }

    /**
     * Initialize connection configuration for registry.
     */
    private void initProtocolConfig(PluginConfig pluginConfig) {
        ProtocolConfig protocolConfig = new ProtocolConfig();
        Map<String, Object> pluginParams = pluginConfig.getProperties();
        if (MapUtils.isEmpty(pluginParams)) {
            logger.debug("plugin config is empty, please check config");
            throw new IllegalStateException("plugin config is empty, please check config");
        }
        protocolConfig.setExtMap(pluginParams);
        this.protocolConfig = protocolConfig;
    }

    @Override
    public void init() throws TRpcExtensionException {
        // Init parameters
        initConsulRegistry(this.getProtocolConfig());
    }

    @Override
    public void doRegister(RegisterInfo registerInfo) {
        consulInstanceManager.register(registerInfo);
    }

    @Override
    public void doUnregister(RegisterInfo registerInfo) {
        consulInstanceManager.unregister(registerInfo);
    }

    @Override
    public void doSubscribe(RegisterInfo registerInfo, NotifyListener notifyListener) {
        // Get list of healthy registered information.
        HealthServiceRegisterInfoList healthServiceRegisterInfoList = new HealthServiceRegisterInfoList(registerInfo)
                .getHealthRegisterInfos();
        notify(registerInfo, notifyListener, healthServiceRegisterInfoList.getRegisterInfos());
        // Watch for changes in data on Consul server and receive notifications.
        watchServiceRegisterInfoUpdate(registerInfo,
                new ConsulNotifier(registerInfo, healthServiceRegisterInfoList.getIndex()));
    }

    @Override
    public void doUnsubscribe(RegisterInfo registerInfo, NotifyListener notifyListener) {
        Objects.requireNonNull(notifyListener, "unsubscribe notifyListener can not null");
        // Cancel watching for changes in data on Consul server and stop receiving notifications.
        unWatchServiceRegisterInfoUpdate(registerInfo);
    }


    @Override
    public void destroy() {
        super.destroy();
        stopAndClearAllTtlTask();
        stopAndClearAllNotifiersTask();
    }

    @Override
    public boolean isAvailable() {
        return consulInstanceManager.isAvailable();
    }

    /**
     * Initialize Consul registry class.
     */
    private void initConsulRegistry(ProtocolConfig protocolConfig) {
        consulInstanceManager = new ConsulInstanceManager(protocolConfig);
    }

    /**
     * Set the Consul instance management class.
     */
    public void setConsulInstanceManager(ConsulInstanceManager consulInstanceManager) {
        this.consulInstanceManager = consulInstanceManager;
    }

    public ProtocolConfig getProtocolConfig() {
        return protocolConfig;
    }

    public void setProtocolConfig(ProtocolConfig protocolConfig) {
        this.protocolConfig = protocolConfig;
    }

    public class ConsulNotifier implements Runnable {

        private final RegisterInfo registerInfo;
        private long consulIndex;
        private boolean running;

        ConsulNotifier(RegisterInfo registerInfo, long consulIndex) {
            this.registerInfo = registerInfo;
            this.consulIndex = consulIndex;
            this.running = true;
        }

        @Override
        public void run() {
            while (this.running) {
                if (ANY_VALUE.equals(registerInfo.getServiceName())) {
                    processServices();
                } else {
                    processService();
                }
            }
        }

        private void processService() {
            String service = registerInfo.getServiceName();
            Response<List<HealthService>> response = consulInstanceManager.getHealthServices(service,
                    consulIndex, getWatchTimeout(registerInfo));
            Long currentIndex = response.getConsulIndex();
            if (currentIndex != null && currentIndex > consulIndex) {
                consulIndex = currentIndex;
                List<HealthService> services = response.getValue();
                List<RegisterInfo> registerInfos = convert(services, registerInfo);
                Optional.ofNullable(getSubscribedRegisterInfos()).map(s -> s.get(registerInfo))
                        .ifPresent(s -> s.getNotifyListeners()
                                .forEach(listener -> doNotify(registerInfo, listener, registerInfos)));
            }
        }

        private void processServices() {
            Response<Map<String, List<String>>> response = consulInstanceManager
                    .getAllServices(consulIndex, getWatchTimeout(registerInfo));
            Long currentIndex = response.getConsulIndex();
            if (currentIndex != null && currentIndex > consulIndex) {
                consulIndex = currentIndex;
                List<HealthService> services = consulInstanceManager.getHealthServices(response.getValue());
                List<RegisterInfo> registerInfos = convert(services, registerInfo);
                Optional.of(getSubscribedRegisterInfos()).map(s -> s.get(registerInfo))
                        .ifPresent(s -> s.getNotifyListeners()
                                .forEach(listener -> doNotify(registerInfo, listener, registerInfos)));
            }
        }

        public void stop() {
            this.running = false;
        }
    }

    /**
     * Consul health registration information list.
     */
    public class HealthServiceRegisterInfoList {

        private final RegisterInfo registerInfo;
        private Long index;
        private List<RegisterInfo> registerInfos;

        public HealthServiceRegisterInfoList(RegisterInfo registerInfo) {
            this.registerInfo = registerInfo;
        }

        public Long getIndex() {
            return index;
        }

        public List<RegisterInfo> getRegisterInfos() {
            return registerInfos;
        }

        public HealthServiceRegisterInfoList getHealthRegisterInfos() {
            // For future compatibility, it means that serviceName can be configured as "*", which can obtain
            // a list of registered services in all Consul.
            if (ANY_VALUE.equals(registerInfo.getServiceName())) {
                Response<Map<String, List<String>>> response = consulInstanceManager.getAllServices(CONSUL_SERVICE_INDEX
                        , getWatchTimeout(registerInfo));
                index = response.getConsulIndex();
                List<HealthService> services = consulInstanceManager.getHealthServices(response.getValue());
                registerInfos = convert(services, registerInfo);
            } else {
                String service = registerInfo.getServiceName();
                Response<List<HealthService>> response = consulInstanceManager.getHealthServices(service,
                        CONSUL_SERVICE_INDEX, getWatchTimeout(registerInfo));
                index = response.getConsulIndex();
                registerInfos = convert(response.getValue(), registerInfo);
            }
            return this;
        }
    }
}
