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

import com.tencent.trpc.core.logger.Logger;
import com.tencent.trpc.core.logger.LoggerFactory;
import com.tencent.trpc.spring.context.TRpcLifecycleManager;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.context.event.ApplicationFailedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.annotation.Order;

/**
 * Handle the application failed event, the reason was TRpcLifecycleManager called before the application failed.
 * <p>Only stop tRPC through the existing {@link TRpcLifecycleManager} singletons of the failed context,
 * never create new beans, since the context may have been partly destroyed.</p>
 */
@Order
public class TRpcApplicationFailedListener implements ApplicationListener<ApplicationFailedEvent> {

    private static final Logger logger = LoggerFactory.getLogger(TRpcApplicationFailedListener.class);

    @Override
    public void onApplicationEvent(ApplicationFailedEvent event) {
        ConfigurableApplicationContext context = event.getApplicationContext();
        if (context == null) {
            return;
        }
        ConfigurableListableBeanFactory beanFactory;
        try {
            beanFactory = context.getBeanFactory();
        } catch (IllegalStateException e) {
            logger.debug("bean factory of the failed application context is not available", e);
            return;
        }
        for (String beanName : beanFactory.getBeanNamesForType(TRpcLifecycleManager.class, false, false)) {
            Object lifecycleManager = beanFactory.getSingleton(beanName);
            if (lifecycleManager instanceof TRpcLifecycleManager) {
                ((TRpcLifecycleManager) lifecycleManager).stopIfStarted();
            }
        }
    }
}
