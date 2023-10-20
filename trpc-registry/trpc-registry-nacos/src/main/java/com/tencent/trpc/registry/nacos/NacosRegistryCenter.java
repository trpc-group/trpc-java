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

package com.tencent.trpc.registry.nacos;

import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.listener.EventListener;
import com.alibaba.nacos.api.naming.listener.NamingEvent;
import com.alibaba.nacos.api.naming.pojo.Instance;
import com.tencent.trpc.core.common.config.PluginConfig;
import com.tencent.trpc.core.exception.TRpcExtensionException;
import com.tencent.trpc.core.extension.Extension;
import com.tencent.trpc.core.logger.Logger;
import com.tencent.trpc.core.logger.LoggerFactory;
import com.tencent.trpc.core.registry.RegisterInfo;
import com.tencent.trpc.registry.center.AbstractFailedRetryRegistryCenter;
import com.tencent.trpc.registry.center.NotifyListener;
import com.tencent.trpc.registry.nacos.config.NacosRegistryCenterConfig;
import com.tencent.trpc.registry.nacos.util.NacosNamingServiceUtils;
import org.apache.commons.collections4.MapUtils;

import java.util.Collections;
import java.util.List;

import static com.tencent.trpc.registry.nacos.constant.NacosConstant.IS_ENABLE;
import static com.tencent.trpc.registry.nacos.constant.NacosConstant.IS_HEALTH;
import static com.tencent.trpc.registry.nacos.constant.NacosConstant.URL_META_KEY;
import static com.tencent.trpc.registry.nacos.constant.NacosConstant.IS_AVAILABLE;
import static com.tencent.trpc.registry.nacos.constant.NacosConstant.INSTANCE_ID_SEPARATOR;
import static com.tencent.trpc.registry.nacos.util.NacosNamingServiceUtils.createNamingService;


/**
 * Nacos registration center
 * Supports registration and deregistration
 */
@Extension("nacos")
public class NacosRegistryCenter extends AbstractFailedRetryRegistryCenter {

    private static final Logger logger = LoggerFactory.getLogger(NacosRegistryCenter.class);

    /**
     * nacos NamingService class
     */
    private NamingService namingService;

    /**
     * nacos Registry Configuration
     */
    private NacosRegistryCenterConfig config;

    @Override
    public void setPluginConfig(PluginConfig pluginConfig) throws TRpcExtensionException {
        super.setPluginConfig(pluginConfig);
        this.config = new NacosRegistryCenterConfig(pluginConfig);
    }

    @Override
    public void init() throws TRpcExtensionException {
        this.namingService = createNamingService(this.config);
    }

    @Override
    public void doRegister(RegisterInfo registerInfo) {
        execute(namingService -> {
            Instance instance = createInstance(registerInfo);
            namingService.registerInstance(registerInfo.getServiceName(),
                    registerInfo.getGroup(), instance);
        });
    }

    @Override
    public void doUnregister(RegisterInfo registerInfo) {
        execute(namingService -> {
            Instance instance = createInstance(registerInfo);
            namingService.deregisterInstance(registerInfo.getServiceName(),
                    registerInfo.getGroup(),
                    instance.getIp(),
                    instance.getPort());
        });
    }

    @Override
    public void doSubscribe(RegisterInfo registerInfo, NotifyListener notifyListener) {
        execute(namingService -> {
            List<Instance> instances = namingService.getAllInstances(registerInfo.getServiceName(),
                    registerInfo.getGroup());
            List<RegisterInfo> registerInfos = NacosNamingServiceUtils.convert(instances, registerInfo);
            notify(registerInfo, notifyListener, registerInfos);
            subscribeEventListener(registerInfo, notifyListener);
        });
    }

    @Override
    public void doUnsubscribe(RegisterInfo registerInfo, NotifyListener notifyListener) {
    }


    @Override
    public boolean isAvailable() {
        return IS_AVAILABLE.equals(namingService.getServerStatus());
    }

    /**
     * Creating an Instance based on RegisterInfo
     * 
     * @param registerInfo Registration Information
     * @return Nacos Instance Object
     */
    private Instance createInstance(RegisterInfo registerInfo) {
        Instance instance = new Instance();
        instance.setInstanceId(getInstanceId(registerInfo));
        instance.setServiceName(registerInfo.getServiceName());
        instance.setIp(registerInfo.getHost());
        instance.setPort(registerInfo.getPort());
        instance.setMetadata(Collections.singletonMap(URL_META_KEY, RegisterInfo.encode(registerInfo)));
        instance.setEnabled(MapUtils.getBooleanValue(registerInfo.getParameters(), IS_ENABLE, Boolean.TRUE));
        instance.setHealthy(MapUtils.getBooleanValue(registerInfo.getParameters(), IS_HEALTH, Boolean.TRUE));
        return instance;
    }

    /**
     * Get instanceId
     *
     * @param Registration Information 
     * @return instanceId
     */
    private static String getInstanceId(RegisterInfo registerInfo) {
        return String.join(INSTANCE_ID_SEPARATOR, registerInfo.getServiceName(), registerInfo.getHost(),
                String.valueOf(registerInfo.getPort()));
    }

    /**
     * Subscribing to Server Data Changes
     *
     * @param Registration Information
     * @param Listener
     * @throws NacosException NacosException
     */
    private void subscribeEventListener(RegisterInfo registerInfo, NotifyListener listener)
            throws NacosException {
        EventListener eventListener = event -> {
            if (event instanceof NamingEvent) {
                NamingEvent e = (NamingEvent) event;
                List<Instance> instances = e.getInstances();
                List<RegisterInfo> registerInfos = NacosNamingServiceUtils.convert(instances, registerInfo);
            }
        };
        namingService.subscribe(registerInfo.getServiceName(), registerInfo.getGroup(),
                eventListener);
    }

    /**
     * Executing Callback
     * 
     * @param callback Specific callback
     */
    private void execute(NamingServiceCallback callback) {
        try {
            callback.callback(namingService);
        } catch (NacosException e) {
            logger.error("callback execute error," + e.getErrMsg(), e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Set namingService, currently used for testing
     *
     * @param namingService namingService
     */
    public void setNamingService(NamingService namingService) {
        this.namingService = namingService;
    }

    /**
     * NamingService callback interface
     */
    interface NamingServiceCallback {

        /**
         * Callback method
         *
         * @param namingService {@link NamingService}
         * @throws NacosException
         */
        void callback(NamingService namingService) throws NacosException;

    }
}
