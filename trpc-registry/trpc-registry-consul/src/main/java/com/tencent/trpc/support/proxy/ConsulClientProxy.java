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

package com.tencent.trpc.support.proxy;

import cn.hutool.core.convert.Convert;
import com.ecwid.consul.v1.ConsulClient;
import com.tencent.trpc.core.common.config.ProtocolConfig;
import com.tencent.trpc.core.logger.Logger;
import com.tencent.trpc.core.logger.LoggerFactory;
import com.tencent.trpc.support.ConsulInstanceManager;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.matcher.ElementMatchers;
import org.apache.commons.collections4.MapUtils;

import java.lang.reflect.Constructor;
import java.util.Map;

import static com.tencent.trpc.support.constant.ConsulConstant.DEFAULT_PORT;
import static com.tencent.trpc.support.constant.ConsulConstant.DEFAULT_REGISTRY_ADDRESSED;
import static com.tencent.trpc.support.constant.ConsulConstant.REGISTRY_ADDRESSED;
import static com.tencent.trpc.support.constant.ConsulConstant.REGEX_REGISTRY_ADDRESSED;
import static com.tencent.trpc.support.constant.ConsulConstant.REGEX_REGISTRY_ADDRESSED_LIST;

/**
 * Consul client proxy class.
 * Used to handle client exceptions, automatically replace client references, and ensure high availability of Consul
 * registration and heartbeat.
 */
public class ConsulClientProxy {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConsulClientProxy.class);

    /**
     * Agent serial number.
     */
    private volatile int index = 0;

    /**
     * ByteBuddy enhanced class.
     */
    private final Class<? extends ConsulClient> cls;

    private final ConsulInstanceManager consulInstanceManager;

    public ConsulClientProxy(ConsulInstanceManager consulInstanceManager) {
        this.consulInstanceManager = consulInstanceManager;
        this.cls = new ByteBuddy().subclass(ConsulClient.class)
                .method(ElementMatchers.isDeclaredBy(ConsulClient.class))
                .intercept(MethodDelegation
                        .to(new ConsulExceptionProcessHandler(this)))
                .make()
                .load(ConsulClient.class.getClassLoader(), ClassLoadingStrategy.Default.INJECTION)
                .getLoaded();
    }


    /**
     * Get the proxy class.
     *
     * @return ConsulClient proxy
     */
    public ConsulClient getProxy() {
        ConsulClient consulClient;
        try {
            Class<?>[] parameterTypes = {String.class, int.class};
            Constructor<? extends ConsulClient> constructor = cls.getConstructor(parameterTypes);
            String[] addresses = getUsingAddresses(getIndex()).split(REGEX_REGISTRY_ADDRESSED);
            Object[] parameters = {addresses[0], Convert.toInt(addresses[1], DEFAULT_PORT)};
            consulClient = constructor.newInstance(parameters);
            index++;
        } catch (Exception e) {
            throw new RuntimeException("getProxy ConsulClient exception", e);
        }
        return consulClient;
    }


    /**
     * Get the address currently in use.
     *
     * @param indexTemp index
     * @return The address currently in use.
     */
    protected String getUsingAddresses(int indexTemp) {
        ProtocolConfig protocolConfig = consulInstanceManager.getProtocolConfig();
        if (null == protocolConfig || MapUtils.isEmpty(protocolConfig.getExtMap())) {
            LOGGER.debug("plugin config is empty, please check config");
            throw new IllegalArgumentException("plugin config is empty, please check config");
        }
        Map<String, Object> extMap = protocolConfig.getExtMap();
        String addressListStr = MapUtils.getString(extMap, REGISTRY_ADDRESSED, DEFAULT_REGISTRY_ADDRESSED);
        String[] addressesList = addressListStr.split(REGEX_REGISTRY_ADDRESSED_LIST);
        // Check if it is out of range and reset the index if necessary.
        return addressesList[indexTemp % addressesList.length];
    }


    /**
     * Get the current index of the agent.
     *
     * @return The index.
     */
    public int getIndex() {
        return index;
    }

    /**
     * Reset the Consul client.
     *
     * @param consulClient The Consul client.
     */
    public void resetConsulClient(ConsulClient consulClient) {
        consulInstanceManager.resetClient(consulClient);
    }
}
