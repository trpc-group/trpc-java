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

package com.tencent.trpc.registry.task;

import com.tencent.trpc.core.common.timer.Timeout;
import com.tencent.trpc.core.common.timer.Timer;
import com.tencent.trpc.core.common.timer.TimerTask;
import com.tencent.trpc.core.logger.Logger;
import com.tencent.trpc.core.logger.LoggerFactory;
import com.tencent.trpc.core.registry.RegisterInfo;
import com.tencent.trpc.registry.center.AbstractFailedRetryRegistryCenter;
import com.tencent.trpc.registry.common.RegistryCenterConfig;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * Abstract class for registry center retry operation tasks.
 */
public abstract class AbstractRetryTask implements TimerTask {

    private static final Logger logger = LoggerFactory.getLogger(AbstractRetryTask.class);

    /**
     * The service for the operation.
     */
    protected final RegisterInfo registerInfo;

    /**
     * Retry the registry center after a failure.
     */
    protected final AbstractFailedRetryRegistryCenter registryCenter;

    /**
     * The name of the task.
     */
    private final String taskName;

    /**
     * The actual retry times of the task.
     */
    private int times = 0;

    /**
     * The cancellation flag of the task.
     */
    private volatile boolean cancelled;

    private RegistryCenterConfig config;

    public AbstractRetryTask(AbstractFailedRetryRegistryCenter registryCenter, RegisterInfo registerInfo) {
        Objects.requireNonNull(registerInfo, "registerInfo can not be null");
        Objects.requireNonNull(registryCenter, "failedRetryRegistryCenter can not be null");
        this.registerInfo = registerInfo;
        this.registryCenter = registryCenter;
        this.taskName = this.getClass().getSimpleName();
        this.config = registryCenter.getRegistryCenterConfig();
        this.cancelled = false;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public void cancel() {
        this.cancelled = true;
    }

    /**
     * Add the retry task again after a failure.
     *
     * @param timeout The retry task.
     * @param tick The time interval for retrying.
     */
    protected void retryAgain(Timeout timeout, long tick) {
        Objects.requireNonNull(timeout, "timeout can not be null");

        Timer timer = timeout.timer();
        if (timeout.isCancelled() || isCancelled()) {
            return;
        }
        times++;
        timer.newTimeout(timeout.task(), tick, TimeUnit.MILLISECONDS);
    }

    /**
     * Add the retry task again after a failure.
     *
     * @param timeout The retry task.
     */
    protected void retryAgain(Timeout timeout) {
        retryAgain(timeout, registryCenter.getRealRetryPeriod());
    }

    /**
     * The actual running interface of the task.
     *
     * @param timeout a handle which is associated with this task
     */
    @Override
    public void run(Timeout timeout) throws Exception {
        if (logger.isDebugEnabled()) {
            logger.debug("taskName: {}, registerInfo: {}", taskName, registerInfo);
        }
        if (timeout.isCancelled() || isCancelled()) {
            return;
        }
        if (times >= this.config.getRetryTimes()) {
            logger.warn("Failed to execute task: {} with registerInfo: {} over max retryTimes: {}",
                    taskName, registerInfo, this.config.getRetryTimes());
            return;
        }
        try {
            doRetry(timeout);
        } catch (Throwable t) {
            logger.warn(
                    "Failed to execute task: {} with registerInfo: {}. Retry again soon. Cause: {}",
                    taskName, registerInfo, t.getMessage(), t);
            retryAgain(timeout, registryCenter.getRealRetryPeriod());
        }
    }

    /**
     * The implementation interface of the retry task subclass.
     *
     * @param timeout The retry task.
     */
    protected abstract void doRetry(Timeout timeout);

}
