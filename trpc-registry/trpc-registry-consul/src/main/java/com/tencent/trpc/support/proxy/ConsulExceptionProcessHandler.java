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

package com.tencent.trpc.support.proxy;

import com.ecwid.consul.v1.ConsulClient;
import com.tencent.trpc.core.logger.Logger;
import com.tencent.trpc.core.logger.LoggerFactory;
import com.tencent.trpc.core.utils.StringUtils;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.SuperCall;

import java.util.concurrent.Callable;

import static com.tencent.trpc.support.constant.ConsulConstant.CONSUL_CLIENT_CONNECTED_EXCEPTION;

/**
 * Dynamic proxy processing class.
 */
public class ConsulExceptionProcessHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConsulExceptionProcessHandler.class);

    /**
     * Used in multi-threaded scenarios to prevent duplicate reconnection triggers.
     */
    private volatile long version = 0;

    /**
     * The proxy object.
     */
    private final ConsulClientProxy dynamicProxy;

    ConsulExceptionProcessHandler(ConsulClientProxy dynamicProxy) {
        this.dynamicProxy = dynamicProxy;
    }

    /**
     * Interceptor execution logic.
     */
    @RuntimeType
    public Object intercept(@SuperCall Callable<?> obj)
            throws Exception {
        Object result = null;
        try {
            result = obj.call();
        } catch (Exception e) {
            processConsulException(e);
        }
        return result;
    }


    /**
     * Handle Consul client exceptions.
     *
     * @param e The Consul exception.
     */
    private void processConsulException(Exception e) {
        // If there is a connection exception, simply recreate the connection.
        if (!StringUtils.isEmpty(e.getMessage()) && e.getMessage().contains(CONSUL_CLIENT_CONNECTED_EXCEPTION)) {
            buildConfigClient(version);
            LOGGER.debug("reconnect consul client config addresses: {}",
                    dynamicProxy.getUsingAddresses(dynamicProxy.getIndex()));
        }
        LOGGER.error("connect consul error {}", e);
        // Throw an exception for external capture and trigger retry logic.
        throw new RuntimeException(e);
    }

    /**
     * Rebuild the Consul client.
     */
    private synchronized void buildConfigClient(long currentVersion) {
        if (currentVersion != version) {
            return;
        }
        version++;
        // Get the new client object.
        ConsulClient consulClient = dynamicProxy.getProxy();
        // Reset the new client object.
        dynamicProxy.resetConsulClient(consulClient);
    }
}
