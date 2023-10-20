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

package com.tencent.trpc.selector.consul;

import com.tencent.trpc.core.exception.TRpcExtensionException;
import com.tencent.trpc.core.extension.DisposableExtension;
import com.tencent.trpc.core.extension.Extension;
import com.tencent.trpc.core.extension.ExtensionLoader;
import com.tencent.trpc.core.logger.Logger;
import com.tencent.trpc.core.logger.LoggerFactory;
import com.tencent.trpc.core.registry.spi.Registry;
import com.tencent.trpc.core.selector.AbstractSelector;
import com.tencent.trpc.core.selector.ServiceId;
import com.tencent.trpc.core.selector.loadbalance.support.RandomLoadBalance;
import com.tencent.trpc.core.selector.spi.Discovery;
import com.tencent.trpc.registry.center.RegistryCenter;
import com.tencent.trpc.registry.discovery.RegistryDiscovery;

/**
 * Consul service selector
 */
@Extension(ConsulSelector.NAME)
public class ConsulSelector extends AbstractSelector implements DisposableExtension {

    public static final String NAME = "consul";

    private static final Logger logger = LoggerFactory.getLogger(ConsulSelector.class);

    @Override
    public void init() throws TRpcExtensionException {
        super.init();
        registryCenter = ExtensionLoader.getExtensionLoader(Registry.class)
                .getExtension(NAME);
    }

    @Override
    protected Discovery subscribe(ServiceId serviceId) {
        return new RegistryDiscovery(serviceId, (RegistryCenter) registryCenter);
    }

    @Override
    public void destroy() throws TRpcExtensionException {
        serviceName2Discovery.clear();
    }

    public void setRegistryCenter(RegistryCenter registryCenter) {
        this.registryCenter = registryCenter;
    }

    public void setLoadBalance(RandomLoadBalance loadBalance) {
        this.loadBalance = loadBalance;
    }
}
