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
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.Ordered;
import org.springframework.lang.NonNull;

/**
 * Start tRPC on {@link ContextRefreshedEvent}.
 * <p>Runs late so that other {@link ContextRefreshedEvent} listeners (e.g. Spring Cloud RefreshScope eager
 * initialization) have finished before tRPC begins to accept traffic. Shutdown is handled by
 * {@link TRpcShutdownListener}, which runs first on {@link org.springframework.context.event.ContextClosedEvent}.</p>
 * <p>Only events of the application context which this manager belongs to are handled, events propagated from
 * child contexts (e.g. actuator management context, whose id is {@code {parentId}:management}) are ignored.</p>
 */
public class TRpcLifecycleManager implements ApplicationListener<ContextRefreshedEvent>, ApplicationContextAware,
        DisposableBean, Ordered {

    /**
     * Order of starting tRPC on {@link ContextRefreshedEvent}.
     */
    public static final int START_ORDER = Ordered.LOWEST_PRECEDENCE - 20;

    /**
     * Whether tRPC has been started by this manager.
     */
    private final AtomicBoolean started = new AtomicBoolean(Boolean.FALSE);

    /**
     * The application context which this manager belongs to.
     */
    private ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(@NonNull ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    /**
     * Whether tRPC has been started by this manager.
     *
     * @return true if started
     */
    public boolean isStarted() {
        return started.get();
    }

    /**
     * Whether the context is managed by this manager, i.e. it is the context this manager belongs to
     * and requires aware of tRPC.
     *
     * @param context the context of the event
     * @return true if managed
     */
    public boolean isManagedContext(ApplicationContext context) {
        if (context == null) {
            return false;
        }
        if (applicationContext != null && applicationContext != context) {
            return false;
        }
        return TRpcSpringUtils.isAwareContext(context);
    }

    /**
     * Stop tRPC only if it has been started by this manager, e.g. on context close or application startup failure.
     */
    public void stopIfStarted() {
        if (started.compareAndSet(Boolean.TRUE, Boolean.FALSE)) {
            ConfigManager.getInstance().stop();
        }
    }

    @Override
    public void onApplicationEvent(@NonNull ContextRefreshedEvent event) {
        if (!isManagedContext(event.getApplicationContext())) {
            return;
        }
        if (started.compareAndSet(Boolean.FALSE, Boolean.TRUE)) {
            try {
                replaceProvidersRef(event.getApplicationContext());
                ConfigManager.getInstance().start(false);
            } catch (Throwable t) {
                // tRPC lifecycle has already stopped itself on start failure, avoid stopping it again
                started.set(Boolean.FALSE);
                throw t;
            }
        }
    }

    /**
     * Fallback of stopping tRPC when the context refresh fails after tRPC has started, in which case
     * beans are destroyed without publishing {@link org.springframework.context.event.ContextClosedEvent}.
     */
    @Override
    public void destroy() {
        stopIfStarted();
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

    @Override
    public int getOrder() {
        return START_ORDER;
    }
}
