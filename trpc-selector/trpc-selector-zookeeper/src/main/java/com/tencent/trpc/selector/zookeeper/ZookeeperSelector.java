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

package com.tencent.trpc.selector.zookeeper;

import com.tencent.trpc.core.exception.TRpcExtensionException;
import com.tencent.trpc.core.extension.Extension;
import com.tencent.trpc.core.extension.ExtensionLoader;
import com.tencent.trpc.core.registry.spi.Registry;
import com.tencent.trpc.core.selector.AbstractSelector;
import com.tencent.trpc.core.selector.ServiceId;
import com.tencent.trpc.core.selector.spi.Discovery;
import com.tencent.trpc.registry.center.RegistryCenter;
import com.tencent.trpc.registry.discovery.RegistryDiscovery;

/**
 * ZookeeperSelector Based on the zookeeper registry. subscribed services have been cached locally, here only a simple
 * package to get the cached data.
 * The report interface can be implemented later to report the health level of the service provider and do the meltdown.
 */
@Extension(ZookeeperSelector.NAME)
public class ZookeeperSelector extends AbstractSelector {

    public static final String NAME = "zookeeper";

    @Override
    public void init() throws TRpcExtensionException {
        super.init();
        registryCenter = (RegistryCenter) ExtensionLoader.getExtensionLoader(Registry.class).getExtension(NAME);
    }

    /**
     * Subscription service. Because only serviceName is used for service discovery, the protocol, host and port of
     * RegisterInfo are not useful for the time being.
     *
     * @param serviceId The service to subscribe to.
     * @return Service Discovery bound by the subscribed service
     */
    @Override
    protected Discovery subscribe(ServiceId serviceId) {
        return new RegistryDiscovery(serviceId, (RegistryCenter) registryCenter);
    }
}
