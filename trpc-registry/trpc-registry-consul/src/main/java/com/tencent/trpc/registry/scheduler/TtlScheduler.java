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

package com.tencent.trpc.registry.scheduler;

import com.tencent.trpc.core.common.NamedThreadFactory;
import com.tencent.trpc.core.logger.Logger;
import com.tencent.trpc.core.logger.LoggerFactory;
import com.tencent.trpc.support.ConsulInstanceManager;
import org.apache.commons.collections4.MapUtils;

import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static com.tencent.trpc.support.constant.ConsulConstant.SERVICE_PRE;
import static com.tencent.trpc.support.constant.ConsulConstant.TTL_SCHEDULE_TIME_INTERVAL;
import static com.tencent.trpc.support.constant.ConsulConstant.DEFAULT_TTL_SCHEDULE_TIME_INTERVAL;

/**
 * TTL scheduler
 */
public class TtlScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(TtlScheduler.class);

    private final Map<String, ScheduledFuture<?>> serviceHeartbeats = new ConcurrentHashMap<>();

    /**
     * Consul default TTL scheduling thread, creating a scheduling thread for each serviceName, with a core thread count
     * of 1.
     */
    private final ScheduledExecutorService scheduler =
            new ScheduledThreadPoolExecutor(1, new NamedThreadFactory("Ttl-Consul-Check-Executor"));

    /**
     * Registry center object.
     */
    private final ConsulInstanceManager consulInstanceManager;

    /**
     * Scheduling initialization delay and interval time (in milliseconds).
     */
    private final long scheduleInitialDelayAndPeriod;

    public TtlScheduler(ConsulInstanceManager consulInstanceManager) {
        this.consulInstanceManager = consulInstanceManager;
        scheduleInitialDelayAndPeriod = MapUtils.getLong(consulInstanceManager.getProtocolConfig().getExtMap()
                , TTL_SCHEDULE_TIME_INTERVAL, DEFAULT_TTL_SCHEDULE_TIME_INTERVAL);
    }

    /**
     * Add the TTL scheduling task corresponding to instanceId.
     * Keep the heartbeat detection between the client and the Consul server. If there is already a TTL task that is the
     * same or invalid, cancel the previous old task to prevent a large number of TTL scheduling tasks and ensure
     * program robustness.
     *
     * @param instanceId serviceName+ip+port
     */
    public void add(String instanceId) {
        ScheduledFuture<?> task = this.scheduler.scheduleAtFixedRate(
                new ConsulHeartbeatTask(instanceId), scheduleInitialDelayAndPeriod,
                scheduleInitialDelayAndPeriod, TimeUnit.MILLISECONDS);
        ScheduledFuture<?> previousTask = this.serviceHeartbeats.put(instanceId, task);
        // If instanceId is duplicated, cancel the previous TTL task.
        if (previousTask != null) {
            previousTask.cancel(true);
        }
    }

    /**
     * Remove the task corresponding to instanceId.
     *
     * @param instanceId instance id
     */
    public void remove(String instanceId) {
        ScheduledFuture<?> task = this.serviceHeartbeats.get(instanceId);
        if (task != null) {
            task.cancel(true);
        }
        this.serviceHeartbeats.remove(instanceId);
    }

    private class ConsulHeartbeatTask implements Runnable {

        private String checkId;

        ConsulHeartbeatTask(String serviceId) {
            this.checkId = serviceId;
            if (!this.checkId.startsWith(SERVICE_PRE)) {
                this.checkId = SERVICE_PRE + this.checkId;
            }
        }

        @Override
        public void run() {
            TtlScheduler.this.consulInstanceManager.agentCheckPass(this.checkId);
            LOGGER.debug("Sending consul heartbeat for: " + this.checkId);
        }

    }

    /**
     * Shut down the thread pool.
     */
    public void stop() {
        scheduler.shutdownNow();
    }

}