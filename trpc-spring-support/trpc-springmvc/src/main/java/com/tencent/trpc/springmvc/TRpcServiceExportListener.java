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

package com.tencent.trpc.springmvc;

import com.tencent.trpc.core.logger.Logger;
import com.tencent.trpc.core.logger.LoggerFactory;
import com.tencent.trpc.spring.context.TRpcLifecycleManager;
import java.util.Objects;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.Ordered;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

/**
 * Export tRPC rest services to {@link TRpcServiceHandlerMapping} on {@link ContextRefreshedEvent},
 * after tRPC has been started by {@link TRpcLifecycleManager}.
 */
public class TRpcServiceExportListener implements ApplicationListener<ContextRefreshedEvent>, Ordered {

    /**
     * Must be after {@link TRpcLifecycleManager#START_ORDER}.
     */
    public static final int EXPORT_ORDER = Ordered.LOWEST_PRECEDENCE - 10;

    private static final Logger logger = LoggerFactory.getLogger(TRpcServiceExportListener.class);

    private final TRpcServiceHandlerMapping handlerMapping;

    private final TRpcLifecycleManager lifecycleManager;

    /**
     * Construct {@link TRpcServiceExportListener}.
     *
     * @param handlerMapping the handler mapping to export tRPC services to
     * @param lifecycleManager the {@link TRpcLifecycleManager} which starts tRPC, null if absent
     */
    public TRpcServiceExportListener(TRpcServiceHandlerMapping handlerMapping,
            @Nullable TRpcLifecycleManager lifecycleManager) {
        this.handlerMapping = Objects.requireNonNull(handlerMapping, "handlerMapping");
        this.lifecycleManager = lifecycleManager;
    }

    @Override
    public void onApplicationEvent(@NonNull ContextRefreshedEvent event) {
        if (lifecycleManager == null) {
            logger.warn("TRpcLifecycleManager not found, skip exporting trpc backends to springmvc");
            return;
        }
        if (!lifecycleManager.isManagedContext(event.getApplicationContext())) {
            logger.debug("the event is not aware");
            return;
        }
        if (!lifecycleManager.isStarted()) {
            logger.warn("tRPC is not started, skip exporting trpc backends to springmvc");
            return;
        }
        handlerMapping.exportTRpcServices();
    }

    @Override
    public int getOrder() {
        return EXPORT_ORDER;
    }
}
