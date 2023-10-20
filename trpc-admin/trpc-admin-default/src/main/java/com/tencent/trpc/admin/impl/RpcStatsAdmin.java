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

package com.tencent.trpc.admin.impl;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.tencent.trpc.admin.dto.rpc.RpcStatsClientDto;
import com.tencent.trpc.admin.dto.rpc.RpcStatsDto;
import com.tencent.trpc.admin.dto.rpc.RpcStatsServiceDto;
import com.tencent.trpc.core.admin.spi.Admin;
import com.tencent.trpc.core.common.ConfigManager;
import com.tencent.trpc.core.common.Version;
import com.tencent.trpc.core.common.config.BackendConfig;
import com.tencent.trpc.core.common.config.PluginConfig;
import com.tencent.trpc.core.common.config.ServiceConfig;
import com.tencent.trpc.core.logger.Logger;
import com.tencent.trpc.core.logger.LoggerFactory;
import com.tencent.trpc.core.management.ForkJoinPoolMXBean;
import com.tencent.trpc.core.management.ThreadPoolMXBean;
import com.tencent.trpc.core.worker.WorkerPoolManager;
import com.tencent.trpc.core.worker.spi.WorkerPool;
import com.tencent.trpc.core.worker.support.thread.ForkJoinWorkerPool;
import com.tencent.trpc.core.worker.support.thread.ThreadWorkerPool;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import org.apache.commons.collections4.CollectionUtils;

/**
 * RpcStatsAdmin
 *
 * Proposal can be found at trpc/trpc-proposal/blob/master/A6-tvar.md
 *
 * RPC-related data, including statistics on interface response time, server connection count, and other information.
 */
@Path("/cmds/stats")
public class RpcStatsAdmin implements Admin {

    private static final Logger logger = LoggerFactory.getLogger(RpcStatsAdmin.class);


    /**
     * Get RPC-related data
     *
     * @return the rpc stats dto
     */
    public static RpcStatsDto getRpcStatsDto() {
        RpcStatsDto rpcStatsDto = new RpcStatsDto();
        rpcStatsDto.setRpcVersion(Version.version());
        rpcStatsDto.setRpcFrameThreadCount(getRpcFrameThreadCount());
        rpcStatsDto.setRpcServiceCount(ConfigManager.getInstance().getServerConfig().getServiceMap().size());

        Map<String, Object> rpcServiceMap = new HashMap<>();
        ConfigManager.getInstance().getServerConfig().getServiceMap().forEach((key, service) -> {
            RpcStatsServiceDto rpcStatsServiceDto = new RpcStatsServiceDto(service.getWorkerPoolObj());
            tvarServiceFieldSet(rpcServiceMap, service.getName(), "rpc_service_", rpcStatsServiceDto);
        });

        Map<String, Object> rpcClientMap = new HashMap<>();
        ConfigManager.getInstance().getClientConfig().getBackendConfigMap().forEach((key, client) -> {
            RpcStatsClientDto rpcStatsClientDto = new RpcStatsClientDto(client.getWorkerPoolObj());
            tvarServiceFieldSet(rpcClientMap, client.getName(), "rpc_client_", rpcStatsClientDto);
        });
        rpcStatsDto.setRpcServiceMap(rpcServiceMap);
        rpcStatsDto.setRpcClientMap(rpcClientMap);
        return rpcStatsDto;
    }


    /**
     * Get the number of threads created by the RPC framework
     *
     * @return the rpc frame thread count
     */
    public static long getRpcFrameThreadCount() {
        long rpcFrameThreadCount;
        // IO threads for all services
        long serviceIoThreadCount = ConfigManager.getInstance().getServerConfig().getServiceMap().values().stream()
                .mapToLong(ServiceConfig::getIoThreads).sum();

        // Boss threads for all services
        long serviceBossThreadCount = ConfigManager.getInstance().getServerConfig().getServiceMap().values().stream()
                .mapToLong(ServiceConfig::getBossThreads).sum();

        // IO threads for all clients
        long clientIoThreadCount = ConfigManager.getInstance().getClientConfig().getBackendConfigMap().values().stream()
                .mapToLong(BackendConfig::getIoThreads).sum();

        // Boss threads for all clients
        long clientBossThreadCount = ConfigManager.getInstance().getClientConfig().getBackendConfigMap().values()
                .stream()
                .mapToLong(BackendConfig::getBossThreads).sum();

        List<WorkerPool> workerPoolList = WorkerPoolManager.getAllInitializedExtension();
        long totalWorkerPool = 0;
        totalWorkerPool = getTotalWorkerPool(workerPoolList, totalWorkerPool);
        rpcFrameThreadCount = totalWorkerPool + serviceIoThreadCount + serviceBossThreadCount + clientBossThreadCount
                + clientIoThreadCount;
        return rpcFrameThreadCount;
    }

    private static long getTotalWorkerPool(List<WorkerPool> workerPoolList, long totalWorkerPool) {
        // If the user has configured threads, use the configured threads. Otherwise, get the default threads.
        if (CollectionUtils.isNotEmpty(workerPoolList)) {
            for (WorkerPool workerPool : workerPoolList) {
                if (workerPool instanceof ThreadWorkerPool) {
                    totalWorkerPool += ((ThreadPoolMXBean) workerPool.report()).getMaximumPoolSize();
                }
                if (workerPool instanceof ForkJoinWorkerPool) {
                    totalWorkerPool += ((ForkJoinPoolMXBean) workerPool.report()).getParallelism();
                }
            }
        } else {
            for (int i = 0; i < ConfigManager.getInstance().getServerConfig().getServiceMap().size(); i++) {
                PluginConfig pluginConfigProvider = WorkerPoolManager.DEF_PROVIDER_WORKER_POOL_CONFIG;
                totalWorkerPool += (Integer) pluginConfigProvider.getProperties().get("core_pool_size");
            }
            PluginConfig pluginConfigConsumer = WorkerPoolManager.DEF_CONSUMER_WORKER_POOL_CONFIG;
            totalWorkerPool += (Integer) pluginConfigConsumer.getProperties().get("core_pool_size");

            PluginConfig pluginConfigNaming = WorkerPoolManager.DEF_NAMING_WORKER_POOL_CONFIG;
            totalWorkerPool += (Integer) pluginConfigNaming.getProperties().get("core_pool_size");
        }
        return totalWorkerPool;
    }

    private static void tvarServiceFieldSet(Map<String, Object> rpcServiceMap, String name, String prefix,
            Object obj) {
        try {
            Field[] fields = obj.getClass().getDeclaredFields();
            for (Field field : fields) {
                field.setAccessible(true);
                JsonIgnore jsonIgnore = field.getAnnotation(JsonIgnore.class);
                if (jsonIgnore == null || !jsonIgnore.value()) {
                    JsonProperty jsonProperty = field.getAnnotation(JsonProperty.class);
                    String filedName;
                    if (jsonProperty != null) {
                        filedName = jsonProperty.value();
                    } else {
                        filedName = field.getName();
                    }
                    rpcServiceMap.put(prefix + name + "_" + filedName,
                            field.get(obj));
                }
            }
        } catch (Exception e) {
            logger.error("trpc stats tvar service field fail:{}", e.getMessage(), e);
        }
    }

    /**
     * Get all RPC TAVR data
     *
     * @return rpc stats dto
     */
    @Path("/rpc")
    @GET
    @Produces({MediaType.APPLICATION_JSON})
    public RpcStatsDto rpc() {
        return getRpcStatsDto();
    }

}
