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
import com.tencent.trpc.core.registry.RegisterInfo;
import com.tencent.trpc.registry.center.AbstractFailedRetryRegistryCenter;
import com.tencent.trpc.registry.center.NotifyListener;
import java.util.Objects;

/**
 * Subscribe to the retry task for the service.
 */
public class RetrySubscribeTask extends AbstractRetryTask {

    private final NotifyListener notifyListener;

    public RetrySubscribeTask(AbstractFailedRetryRegistryCenter registryCenter, RegisterInfo registerInfo,
            NotifyListener notifyListener) {
        super(registryCenter, registerInfo);
        Objects.requireNonNull(notifyListener, "notifyListener can not be null");
        this.notifyListener = notifyListener;
    }

    @Override
    protected void doRetry(Timeout timeout) {
        registryCenter.doSubscribe(registerInfo, notifyListener);
        registryCenter.removeFailedSubscribedTask(registerInfo, notifyListener);
    }
}
