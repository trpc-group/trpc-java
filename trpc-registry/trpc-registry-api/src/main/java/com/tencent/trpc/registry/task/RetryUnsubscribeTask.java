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

package com.tencent.trpc.registry.task;

import com.tencent.trpc.core.common.timer.Timeout;
import com.tencent.trpc.core.registry.RegisterInfo;
import com.tencent.trpc.registry.center.AbstractFailedRetryRegistryCenter;
import com.tencent.trpc.registry.center.NotifyListener;
import java.util.Objects;

/**
 * Unsubscribe from the retry task for the service.
 */
public class RetryUnsubscribeTask extends AbstractRetryTask {

    private final NotifyListener notifyListener;

    public RetryUnsubscribeTask(AbstractFailedRetryRegistryCenter registryCenter,
            RegisterInfo registerInfo, NotifyListener notifyListener) {
        super(registryCenter, registerInfo);
        Objects.requireNonNull(notifyListener, "notifyListener can not be null");
        this.notifyListener = notifyListener;
    }

    @Override
    protected void doRetry(Timeout timeout) {
        registryCenter.doUnsubscribe(registerInfo, notifyListener);
        registryCenter.removeFailedUnsubscribedTask(registerInfo, notifyListener);
    }
}
