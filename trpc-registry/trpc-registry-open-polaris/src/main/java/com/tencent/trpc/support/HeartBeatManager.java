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

package com.tencent.trpc.support;

import static com.tencent.trpc.polaris.common.PolarisRegistryConstant.HEARTBEAT_INTERVAL_DEFAULT;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.tencent.trpc.core.logger.Logger;
import com.tencent.trpc.core.logger.LoggerFactory;
import com.tencent.trpc.core.registry.RegisterInfo;
import com.tencent.trpc.registry.polaris.PolarisRegistry;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The Polaris heartbeat reporting timer.
 */
public class HeartBeatManager {

    private static final Logger logger = LoggerFactory.getLogger(HeartBeatManager.class);

    private static ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor(
            new ThreadFactory() {
                final AtomicInteger threadCount = new AtomicInteger(0);

                @Override
                public Thread newThread(Runnable r) {
                    Thread thread = new Thread(r, "heartbeat-trpc-polaris" + threadCount.getAndIncrement());
                    thread.setDaemon(true);
                    return thread;
                }
            });

    private static int heartBeatInterval = HEARTBEAT_INTERVAL_DEFAULT;

    /**
     * A service may register to multiple registry centers.
     */
    private static ConcurrentMap<RegisterInfo, Set<PolarisRegistry>> serviceMap = Maps.newConcurrentMap();

    public static void init(int heatBeatInterval) {
        heartBeatInterval = heatBeatInterval;
        scheduledExecutorService.scheduleWithFixedDelay(() -> {
            try {
                serviceMap.forEach((registerInfo, polarisRegistries) -> polarisRegistries.forEach(
                        polarisRegistry -> polarisRegistry.heartbeat(registerInfo)));
            } catch (Throwable e) {
                logger.error("heartbeat failed. and interval:{}", heartBeatInterval, e);
            }
        }, heartBeatInterval, heartBeatInterval, TimeUnit.MILLISECONDS);
    }

    /**
     * Add to the pool of instances waiting for heartbeat.
     *
     * @param registerInfo The service information.
     * @param polarisRegistry The corresponding registry center.
     */
    public static void startHeartBeat(RegisterInfo registerInfo, PolarisRegistry polarisRegistry) {
        serviceMap.computeIfAbsent(registerInfo, key -> Sets.newHashSet()).add(polarisRegistry);
    }

    /**
     * Destroy the heartbeat pool.
     */
    public static void destroy() {
        if (!scheduledExecutorService.isShutdown()) {
            scheduledExecutorService.shutdown();
        }
    }
}
