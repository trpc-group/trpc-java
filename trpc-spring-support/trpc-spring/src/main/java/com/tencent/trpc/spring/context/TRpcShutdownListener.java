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

package com.tencent.trpc.spring.context;

import java.util.Objects;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.core.Ordered;
import org.springframework.lang.NonNull;

/**
 * Stop tRPC on {@link ContextClosedEvent}.
 * <p>Runs early so that tRPC unregisters and drains in-flight requests before other
 * {@link ContextClosedEvent} listeners release resources.</p>
 */
public class TRpcShutdownListener implements ApplicationListener<ContextClosedEvent>, Ordered {

    /**
     * Order of stopping tRPC on {@link ContextClosedEvent}.
     */
    public static final int STOP_ORDER = Ordered.HIGHEST_PRECEDENCE + 20000;

    private final TRpcLifecycleManager lifecycleManager;

    public TRpcShutdownListener(TRpcLifecycleManager lifecycleManager) {
        this.lifecycleManager = Objects.requireNonNull(lifecycleManager, "lifecycleManager");
    }

    @Override
    public void onApplicationEvent(@NonNull ContextClosedEvent event) {
        if (!lifecycleManager.isManagedContext(event.getApplicationContext())) {
            return;
        }
        lifecycleManager.stopIfStarted();
    }

    @Override
    public int getOrder() {
        return STOP_ORDER;
    }
}
