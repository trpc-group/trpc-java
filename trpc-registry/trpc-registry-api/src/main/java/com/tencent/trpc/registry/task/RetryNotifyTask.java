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
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * The callback notification of the subscription service to the retry task when data changes.
 */
public class RetryNotifyTask extends AbstractRetryTask {

    private final NotifyListener notifyListener;

    private final List<RegisterInfo> registerInfos = new CopyOnWriteArrayList<>();

    public RetryNotifyTask(AbstractFailedRetryRegistryCenter registryCenter, RegisterInfo registerInfo,
            NotifyListener notifyListener) {
        super(registryCenter, registerInfo);
        Objects.requireNonNull(notifyListener, "notifyListener can not be null");
        this.notifyListener = notifyListener;
    }

    public void addRegisterInfoToRetry(List<RegisterInfo> registerInfos) {
        if (registerInfos.isEmpty()) {
            return;
        }
        this.registerInfos.addAll(registerInfos);
    }

    @Override
    protected void doRetry(Timeout timeout) {
        registryCenter.doNotify(registerInfo, notifyListener, registerInfos);
        registryCenter.removeFailedNotifyTask(registerInfo, notifyListener);
        this.registerInfos.clear();
    }
}
