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

package com.tencent.trpc.core.rpc;

import com.google.common.collect.Maps;
import com.tencent.trpc.core.common.config.ProtocolConfig;
import com.tencent.trpc.core.extension.ExtensionLoader;
import com.tencent.trpc.core.logger.Logger;
import com.tencent.trpc.core.logger.LoggerFactory;
import com.tencent.trpc.core.rpc.spi.RpcServerFactory;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Cache servers of the same type.
 */
public class RpcServerManager {

    private static final Logger logger = LoggerFactory.getLogger(RpcServerManager.class);
    private static Map<String, RpcServer> serverMap = Maps.newConcurrentMap();
    private static AtomicBoolean closedFlag = new AtomicBoolean(Boolean.FALSE);

    /**
     * Get or create RpcServer.
     *
     * @param protocolConfig the protocol configuration
     * @return the RpcServer instance
     */
    public static RpcServer getOrCreateRpcServer(ProtocolConfig protocolConfig) {
        return serverMap.computeIfAbsent(protocolConfig.toUniqId(), uniqId -> {
            ExtensionLoader<RpcServerFactory> extensionLoader =
                    ExtensionLoader.getExtensionLoader(RpcServerFactory.class);
            return extensionLoader.getExtension(protocolConfig.getProtocol())
                    .createRpcServer(protocolConfig);
        });
    }

    public static void remove(ProtocolConfig config) {
        serverMap.remove(config.toUniqId());
    }

    /**
     * Called when the service ends.
     */
    public static void shutdown() {
        if (closedFlag.compareAndSet(Boolean.FALSE, Boolean.TRUE)) {
            for (RpcServer each : serverMap.values()) {
                logger.info("Stopping server {}", each.getProtocolConfig().toUniqId());
                each.close();
            }
            serverMap.clear();
        }
    }

    /**
     * For test purpose.
     */
    public static synchronized void reset() {
        closedFlag.set(false);
    }

}
