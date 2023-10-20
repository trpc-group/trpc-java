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

import com.tencent.trpc.core.common.NamedThreadFactory;
import com.tencent.trpc.core.logger.Logger;
import com.tencent.trpc.core.logger.LoggerFactory;
import com.tencent.trpc.core.registry.RegisterInfo;
import com.tencent.trpc.registry.consul.ConsulRegistryCenter;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

import static java.util.concurrent.Executors.newCachedThreadPool;

/**
 * Notifier business processing utility class
 * Used to encapsulate consumer-side listening for changes in Consul server data
 */
public class NotifiersServiceUtils {

    public static final Logger LOGGER = LoggerFactory.getLogger(NotifiersServiceUtils.class);

    /**
     * Here we use newCachedThreadPool. For each RegisterInfo object, only one watch thread is needed.
     * When performing {@link #watchServiceRegisterInfoUpdate}, for the same RegisterInfo, the previous monitoring task
     * will be stopped.
     */
    private static final ExecutorService NOTIFIER_EXECUTOR = newCachedThreadPool(
            new NamedThreadFactory("trpc-consul-notifier", true));

    /**
     * Registration information and Consul notifier cache
     */
    private static final Map<RegisterInfo, ConsulRegistryCenter.ConsulNotifier> NOTIFIERS = new ConcurrentHashMap<>();


    /**
     * Listen for updates to the specified service registration information
     * For the same RegisterInfo, the previous monitoring task will be stopped.
     * Ensure that each RegisterInfo object only needs one watch thread.
     *
     * @param registerInfo Registration information to be monitored
     * @param consulNotifier Notifier
     */
    public static void watchServiceRegisterInfoUpdate(RegisterInfo registerInfo,
            ConsulRegistryCenter.ConsulNotifier consulNotifier) {
        Objects.requireNonNull(registerInfo, "watch service register info can not null");
        ConsulRegistryCenter.ConsulNotifier preConsulNotifierTask = NOTIFIERS.put(registerInfo, consulNotifier);
        if (null != preConsulNotifierTask) {
            preConsulNotifierTask.stop();
            LOGGER.warn("stop consul watch service register info previous task, register service name {}",
                    registerInfo.getServiceName());
        }
        NOTIFIER_EXECUTOR.submit(consulNotifier);
    }

    /**
     * Cancel listening for updates to the specified service registration information
     *
     * @param registerInfo Registration information to be canceled
     */
    public static void unWatchServiceRegisterInfoUpdate(RegisterInfo registerInfo) {
        Objects.requireNonNull(registerInfo, "unwatch service register info can not null");
        ConsulRegistryCenter.ConsulNotifier notifier = NOTIFIERS.remove(registerInfo);
        notifier.stop();
    }


    /**
     * Stop and clean up all monitoring tasks
     */
    public static void stopAndClearAllNotifiersTask() {
        if (!NOTIFIERS.isEmpty()) {
            NOTIFIERS.forEach((registerInfo, notifiers) -> notifiers.stop());
            NOTIFIERS.clear();
        }

        NOTIFIER_EXECUTOR.shutdown();
    }

}
