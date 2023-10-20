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

package com.tencent.trpc.support.util;

import cn.hutool.core.convert.Convert;
import com.tencent.trpc.core.logger.Logger;
import com.tencent.trpc.core.logger.LoggerFactory;
import com.tencent.trpc.core.registry.RegisterInfo;
import com.tencent.trpc.registry.scheduler.TtlScheduler;
import com.tencent.trpc.support.ConsulInstanceManager;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.tencent.trpc.support.constant.ConsulConstant.TTL_ENABLED;
import static com.tencent.trpc.support.util.ConsulServiceUtils.getInstanceId;
import static com.tencent.trpc.support.util.ConsulServiceUtils.getKeyForPluginsInServiceConfig;

/**
 * TTL scheduler instance management utility class
 * Mainly abstracts the functions of adding and canceling TTL schedulers.
 * Add TTL scheduler during registration and remove TTL scheduler during deregistration.
 */
public class TtlSchedulerInstanceUtils {

    private static final Logger logger = LoggerFactory.getLogger(TtlSchedulerInstanceUtils.class);

    /**
     * TTL reporting cache, key: serviceName, value: TtlScheduler
     */
    private static final Map<String, TtlScheduler> TTL_SCHEDULERS = new ConcurrentHashMap<>();

    /**
     * Determine whether to enable TTL health check and add it to the scheduler for scheduling
     *
     * @param registerInfo Registration information
     */
    public static void addTtlScheduler(RegisterInfo registerInfo, ConsulInstanceManager consulInstanceManager) {
        // Configurations under the plugin
        Map<String, Object> pluginConfigMap = consulInstanceManager.getProtocolConfig().getExtMap();
        // Configurations for services under the plugin
        Map<String, Object> pluginInServiceConfigMap = registerInfo.getParameters();
        // Get service configuration based on priority

        Object ttlEnableObj = getKeyForPluginsInServiceConfig(pluginConfigMap,
                pluginInServiceConfigMap, TTL_ENABLED);
        if (Convert.toBool(ttlEnableObj, Boolean.TRUE)) {
            // Start a TTL scheduler for each service name
            TtlScheduler ttlScheduler = TTL_SCHEDULERS.computeIfAbsent(
                    registerInfo.getServiceName(), name -> new TtlScheduler(consulInstanceManager));
            ttlScheduler.add(getInstanceId(registerInfo));
            logger.debug("add service {} to scheduler", getInstanceId(registerInfo));
        }
    }


    /**
     * Remove TTL scheduling task
     *
     * @param registerInfo Registration information
     */
    public static void removeTtlScheduler(RegisterInfo registerInfo) {
        TtlScheduler ttlScheduler = TTL_SCHEDULERS.get(registerInfo.getServiceName());
        String instanceId = getInstanceId(registerInfo);
        if (null != ttlScheduler) {
            ttlScheduler.remove(instanceId);
        }
    }

    /**
     * Stop and clean up all TTL scheduling tasks
     */
    public static void stopAndClearAllTtlTask() {
        if (!TTL_SCHEDULERS.isEmpty()) {
            TTL_SCHEDULERS.forEach((key, ttlScheduler) -> ttlScheduler.stop());
            TTL_SCHEDULERS.clear();
        }
    }
}
