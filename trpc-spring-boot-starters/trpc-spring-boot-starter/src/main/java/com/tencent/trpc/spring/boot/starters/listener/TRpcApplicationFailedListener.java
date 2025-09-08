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

package com.tencent.trpc.spring.boot.starters.listener;

import com.tencent.trpc.core.common.ConfigManager;
import org.springframework.boot.context.event.ApplicationFailedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.annotation.Order;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Handle the application failed event, the reason was TRpcLifecycleManager called before the application failed.
 */
@Order
public class TRpcApplicationFailedListener implements ApplicationListener<ApplicationFailedEvent> {
    private static final AtomicBoolean PROCESSED = new AtomicBoolean(false);

    @Override
    public void onApplicationEvent(ApplicationFailedEvent event) {
        if (PROCESSED.compareAndSet(false, true)) {
            stopTRPC();
        }
    }

    private void stopTRPC() {
        ConfigManager.getInstance().stop();
    }
}