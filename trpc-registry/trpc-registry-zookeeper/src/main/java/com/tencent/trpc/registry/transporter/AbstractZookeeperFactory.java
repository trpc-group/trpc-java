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

package com.tencent.trpc.registry.transporter;

import com.tencent.trpc.core.logger.Logger;
import com.tencent.trpc.core.logger.LoggerFactory;
import com.tencent.trpc.registry.common.RegistryCenterConfig;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.commons.lang3.StringUtils;

/**
 * Abstract creation factory for the zookeeper client. This designs a layer of cache for the zookeeper client that
 * can be reused when using through the client.
 */
public abstract class AbstractZookeeperFactory implements ZookeeperFactory {

    private static final Logger logger = LoggerFactory.getLogger(AbstractZookeeperFactory.class);

    /**
     * Zookeeper client cache
     */
    private final Map<String, ZookeeperClient> zookeeperClientCache = new ConcurrentHashMap<>();

    /**
     * Zookeeper client creation interface
     *
     * @param config Registration center configuration item
     * @return zookeeper client
     */
    protected abstract ZookeeperClient createClient(RegistryCenterConfig config);

    @Override
    public ZookeeperClient connect(RegistryCenterConfig config) {
        return zookeeperClientCache.compute(getCuratorConnectString(config), (connectConfig, client) -> {
            if (client != null && client.isConnected()) {
                return client;
            }
            return createClient(config);
        });
    }

    /**
     * Get the connection address of zookeeper client
     *
     * @param config zookeeper client connection configuration
     * @return zookeeper client connection address
     */
    private String getCuratorConnectString(RegistryCenterConfig config) {
        if (StringUtils.isNotEmpty(config.getAddresses())) {
            return config.getAddresses();
        }
        throw new IllegalStateException("curator can't get addresses");
    }
}
