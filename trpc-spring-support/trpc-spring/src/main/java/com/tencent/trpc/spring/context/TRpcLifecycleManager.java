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

import com.tencent.trpc.core.common.ConfigManager;
import com.tencent.trpc.core.common.config.ProviderConfig;
import com.tencent.trpc.spring.util.TRpcSpringUtils;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.SmartApplicationListener;
import org.springframework.core.Ordered;
import org.springframework.lang.NonNull;

/**
 * Response on {@link ContextRefreshedEvent} and {@link ContextClosedEvent}
 * to perform necessary tRPC operations.
 */
public class TRpcLifecycleManager implements SmartApplicationListener {

    /**
     * tRPC 容器启动标记
     */
    private final AtomicBoolean started = new AtomicBoolean(Boolean.FALSE);

    @Override
    public boolean supportsEventType(@NonNull Class<? extends ApplicationEvent> eventType) {
        return ContextRefreshedEvent.class == eventType
                || ContextClosedEvent.class == eventType;
    }

    @Override
    public void onApplicationEvent(@NonNull ApplicationEvent event) {
        if (event instanceof ContextRefreshedEvent) {
            onContextRefreshedEvent((ContextRefreshedEvent) event);
        } else {
            onContextClosedEvent((ContextClosedEvent) event);
        }
    }

    /**
     * Start tRPC services on {@link ContextRefreshedEvent}
     */
    private void onContextRefreshedEvent(ContextRefreshedEvent event) {
        if (!TRpcSpringUtils.isAwareContext(event.getApplicationContext())) {
            return;
        }
        if (started.compareAndSet(Boolean.FALSE, Boolean.TRUE)) {
            replaceProvidersRef(event.getApplicationContext());
            startTRPC();
        }
    }

    /**
     * Shutdown tRPC services on {@link ContextClosedEvent}
     */
    private void onContextClosedEvent(ContextClosedEvent event) {
        if (!TRpcSpringUtils.isAwareContext(event.getApplicationContext())) {
            return;
        }
        if (started.compareAndSet(Boolean.TRUE, Boolean.FALSE)) {
            stopTRPC();
        }
    }

    /**
     * Replace tRPC provider ref to related spring beans.
     */
    private void replaceProvidersRef(ApplicationContext applicationContext) {
        ConfigManager.getInstance().getServerConfig().getServiceMap().values().stream()
                .flatMap(config -> config.getProviderConfigs().stream())
                .forEach(pc -> doReplaceProviderRef(applicationContext, pc));
    }

    @SuppressWarnings({"rawtypes"})
    private void doReplaceProviderRef(ApplicationContext applicationContext, ProviderConfig providerConfig) {
        try {
            TRpcSpringUtils.setRef(applicationContext, providerConfig);
        } catch (Exception e) {
            throw new RuntimeException("Spring get bean(class=" + providerConfig.getRefClazz() + ") exception", e);
        }
    }

    private void startTRPC() {
        ConfigManager.getInstance().start(false);
    }

    private void stopTRPC() {
        ConfigManager.getInstance().stop();
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 20000;
    }
}
